package zio3d.engine.shaders

import java.nio.file.Path

import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL12.{GL_CLAMP_TO_EDGE, GL_TEXTURE_WRAP_R}
import org.lwjgl.opengl.GL13.{GL_TEXTURE0, GL_TEXTURE_CUBE_MAP, GL_TEXTURE_CUBE_MAP_POSITIVE_X}
import org.lwjgl.opengl.GL15.{GL_ARRAY_BUFFER, GL_STATIC_DRAW}
import org.lwjgl.opengl.GL20
import zio._
import zio3d.core.buffers.Buffers
import zio3d.core.gl._
import zio3d.core.images.Images
import zio3d.engine._
import zio3d.engine.loaders.LoadingError
import zio3d.engine.loaders.LoadingError.{ProgramLinkError, ShaderCompileError}

package object skybox {
  type SkyboxShaderInterpreter = Has[SkyboxShaderInterpreter.Service]

  object SkyboxShaderInterpreter {

    trait Service extends ShaderInterpreter.Service[SkyboxDefinition, SkyboxShaderProgram]

    val live = ZLayer.fromEnvironment[PreShaderEnv, SkyboxShaderInterpreter] { env =>
      Has(new Service {

        private val gl = env.get[GL.Service]
        private val buffers = env.get[Buffers.Service]
        private val images = env.get[Images.Service]

        private val strVertexShader =
          """#version 330
            |
            |layout (location=0) in vec3 position;
            |
            |out vec3 texCoord;
            |
            |uniform mat4 modelViewMatrix;
            |uniform mat4 projectionMatrix;
            |
            |void main()
            |{
            |    gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
            |    texCoord = position;
            |}
        """.stripMargin

        private val strFragmentShader =
          s"""#version 330
             |out vec4 fragColor;
             |
             |in vec3 texCoord;
             |
             |uniform samplerCube skybox;
             |uniform vec3 ambientLight;
             |
             |void main()
             |{
             |    fragColor = vec4(ambientLight, 1) * texture(skybox, texCoord);
             |}
    """.stripMargin

        def loadShaderProgram =
          for {
            vs <- gl.compileShader(GL20.GL_VERTEX_SHADER, strVertexShader).mapError(ShaderCompileError)
            fs <- gl.compileShader(GL20.GL_FRAGMENT_SHADER, strFragmentShader).mapError(ShaderCompileError)
            p  <- gl.createProgram
            _  <- gl.attachShader(p, vs)
            _  <- gl.attachShader(p, fs)
            _  <- gl.linkProgram(p)
            _  <- gl.useProgram(p)
            ls <- gl.getProgrami(p, GL20.GL_LINK_STATUS)
            _  <- gl.getProgramInfoLog(p).flatMap(log => IO.fail(ProgramLinkError(log))).when(ls == 0)

            uniProjectionMatrix <- gl.getUniformLocation(p, "projectionMatrix")
            uniModelViewMatrix  <- gl.getUniformLocation(p, "modelViewMatrix")
            uniSkybox           <- gl.getUniformLocation(p, "skybox")
            uniAmbientLight     <- gl.getUniformLocation(p, "ambientLight")

            positionAttr <- gl.getAttribLocation(p, "position")
            texCoordAttr <- gl.getAttribLocation(p, "texCoord")

          } yield SkyboxShaderProgram(
            p,
            uniModelViewMatrix,
            uniProjectionMatrix,
            uniSkybox,
            uniAmbientLight,
            positionAttr,
            texCoordAttr
          )

        def loadMesh(program: SkyboxShaderProgram, input: SkyboxDefinition) =
          for {
            vao <- gl.genVertexArrays()
            _   <- gl.bindVertexArray(vao)

            vbo <- gl.genBuffers
            _   <- gl.bindBuffer(GL_ARRAY_BUFFER, vbo)
            buf <- buffers.floatBuffer(input.vertices)
            _   <- gl.bufferData(GL_ARRAY_BUFFER, buf, GL_STATIC_DRAW)
            _   <- gl.vertexAttribPointer(program.positionAttr, 3, GL_FLOAT, false, 0, 0)

            _ <- gl.bindBuffer(GL_ARRAY_BUFFER, VertexBufferObject.None)
            _ <- gl.bindVertexArray(VertexArrayObject.None)

            t <- loadTexture(input.textures)

          } yield Mesh(vao, List(vbo), 12, Material.textured(t))

        def loadTexture(faces: List[Path]) =
          for {
            t <- gl.genTextures
            _ <- gl.bindTexture(GL_TEXTURE_CUBE_MAP, t)
            _ <- IO.foreach(faces.zipWithIndex)(f => loadCubeFace(f._1, f._2))
            _ <- gl.texParameter(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
            _ <- gl.texParameter(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
            _ <- gl.texParameter(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
            _ <- gl.texParameter(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
            _ <- gl.texParameter(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE)
          } yield t

        private def loadCubeFace(source: Path, i: Int) =
          for {
            img <- images.loadImage(source, flipVertical = false, 3)
            _ <- gl.texImage2D(
                  GL_TEXTURE_CUBE_MAP_POSITIVE_X + i,
                  0,
                  img.format,
                  img.width,
                  img.height,
                  0,
                  img.format,
                  GL_UNSIGNED_BYTE,
                  img.image
                )
          } yield ()

        def render(
          program: SkyboxShaderProgram,
          item: GameItem,
          transformation: Transformation,
          fixtures: Fixtures
        ) =
          gl.useProgram(program.program) *>
            gl.uniform1i(program.uniSkybox, 0) *>
            gl.uniformMatrix4fv(program.uniProjectionMatrix, false, transformation.projectionMatrix) *>
            gl.uniform3f(program.uniAmbientLight, 1.0f, 1.0f, 1.0f) *>
            ZIO.foreach(item.model.meshes)(m => renderMesh(program, m, item.instances, transformation)) *>
            gl.bindTexture(GL_TEXTURE_CUBE_MAP, Texture.None) *>
            gl.bindVertexArray(VertexArrayObject.None) *>
            gl.useProgram(Program.None)

        private def renderMesh(
          program: SkyboxShaderProgram,
          mesh: Mesh,
          items: List[ItemInstance],
          trans: Transformation
        ): UIO[Unit] =
          gl.depthFunc(GL_LEQUAL) *>
            gl.bindVertexArray(mesh.vao) *>
            mesh.material.texture.fold(IO.unit)(t => bindTexture(t)) *>
            gl.enableVertexAttribArray(program.positionAttr) *>
            gl.enableVertexAttribArray(program.texCoordAttr) *>
            ZIO.foreach(items)(i => renderInstance(program, i, trans)) *>
            gl.disableVertexAttribArray(program.positionAttr) *>
            gl.disableVertexAttribArray(program.texCoordAttr) *>
            gl.bindVertexArray(VertexArrayObject.None) *>
            gl.depthFunc(GL_LESS)

        private def renderInstance(
          program: SkyboxShaderProgram,
          i: ItemInstance,
          transformation: Transformation
        ) =
          gl.uniformMatrix4fv(program.uniModelViewMatrix, false, transformation.noTranslation.getModelViewMatrix(i)) *>
            gl.drawArrays(GL_TRIANGLES, 0, 36)

        private def bindTexture(texture: Texture) =
          gl.activeTexture(GL_TEXTURE0) *>
            gl.bindTexture(GL_TEXTURE_CUBE_MAP, texture)

        override def cleanup(program: SkyboxShaderProgram) =
          gl.deleteProgram(program.program)
      })
    }
  }

  final def loadShaderProgram: ZIO[SkyboxShaderInterpreter, LoadingError, SkyboxShaderProgram] =
    ZIO.accessM(_.get.loadShaderProgram)

  final def loadMesh(
    program: SkyboxShaderProgram,
    input: SkyboxDefinition
  ): ZIO[SkyboxShaderInterpreter, LoadingError, Mesh] =
    ZIO.accessM(_.get.loadMesh(program, input))

  final def render(
    program: SkyboxShaderProgram,
    item: GameItem,
    transformation: Transformation,
    fixtures: Fixtures
  ): ZIO[SkyboxShaderInterpreter, Nothing, Unit] =
    ZIO.accessM(_.get.render(program, item, transformation, fixtures))

  final def cleanup(program: SkyboxShaderProgram): ZIO[SkyboxShaderInterpreter, Nothing, Unit] =
    ZIO.accessM(_.get.cleanup(program))
}
