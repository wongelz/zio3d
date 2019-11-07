package zio3d.engine.shaders.skybox

import java.nio.file.Path

import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL12._
import org.lwjgl.opengl.GL13._
import org.lwjgl.opengl.GL15._
import org.lwjgl.opengl.GL20
import zio.{IO, UIO}
import zio3d.core.buffers.Buffers
import zio3d.core.gl.GL
import zio3d.core.gl.GL._
import zio3d.engine.loaders.LoadingError.{ProgramLinkError, ShaderCompileError}
import zio3d.engine.loaders.texture._
import zio3d.engine.shaders.ShaderInterpreter
import zio3d.engine.shaders.ShaderInterpreter.Service
import zio3d.engine._
import zio3d.engine.shaders.skybox.SkyboxShaderInterpreter.SkyboxShaderProgram

trait SkyboxShaderInterpreter {
  def skyboxShaderInterpreter: Service[SkyboxDefinition, SkyboxShaderProgram]
}

object SkyboxShaderInterpreter {

  final case class SkyboxShaderProgram(
    program: Program,
    uniModelViewMatrix: UniformLocation,
    uniProjectionMatrix: UniformLocation,
    uniSkybox: UniformLocation,
    uniAmbientLight: UniformLocation,
    positionAttr: AttribLocation,
    texCoordAttr: AttribLocation
  )

  trait Live extends GL.Live with Buffers.Live with TextureLoader.Live with SkyboxShaderInterpreter {

    val skyboxShaderInterpreter = new ShaderInterpreter.Service[SkyboxDefinition, SkyboxShaderProgram] {

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
        items: Iterable[GameItem],
        transformation: Transformation,
        fixtures: Fixtures
      ): IO[Nothing, Unit] =
        gl.useProgram(program.program) *>
          gl.uniform1i(program.uniSkybox, 0) *>
          gl.uniformMatrix4fv(program.uniProjectionMatrix, false, transformation.projectionMatrix) *>
          gl.uniform3f(program.uniAmbientLight, 1.0f, 1.0f, 1.0f) *>
          IO.foreach(items)(i => renderItem(program, i, transformation)) *>
          gl.bindTexture(GL_TEXTURE_CUBE_MAP, Texture.None) *>
          gl.bindVertexArray(VertexArrayObject.None) *>
          gl.useProgram(Program.None)

      private def renderItem(program: SkyboxShaderProgram, item: GameItem, transformation: Transformation) =
        gl.uniformMatrix4fv(program.uniModelViewMatrix, false, transformation.noTranslation.getModelViewMatrix(item)) *>
          IO.foreach(item.meshes)(m => renderMesh(program, m))

      private def renderMesh(program: SkyboxShaderProgram, mesh: Mesh): UIO[Unit] =
        gl.depthFunc(GL_LEQUAL) *>
          gl.bindVertexArray(mesh.vao) *>
          mesh.material.texture.fold(IO.unit)(t => bindTexture(t)) *>
          gl.enableVertexAttribArray(program.positionAttr) *>
          gl.enableVertexAttribArray(program.texCoordAttr) *>
          gl.drawArrays(GL_TRIANGLES, 0, 36) *>
          gl.disableVertexAttribArray(program.positionAttr) *>
          gl.disableVertexAttribArray(program.texCoordAttr) *>
          gl.bindVertexArray(VertexArrayObject.None) *>
          gl.depthFunc(GL_LESS)

      private def bindTexture(texture: Texture) =
        gl.activeTexture(GL_TEXTURE0) *>
          gl.bindTexture(GL_TEXTURE_CUBE_MAP, texture)
    }
  }

  object Live extends Live

}
