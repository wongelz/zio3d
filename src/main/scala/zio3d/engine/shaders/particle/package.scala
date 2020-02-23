package zio3d.engine.shaders

import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL13.GL_TEXTURE0
import org.lwjgl.opengl.GL15.{GL_ARRAY_BUFFER, GL_ELEMENT_ARRAY_BUFFER, GL_STATIC_COPY, GL_STATIC_DRAW}
import org.lwjgl.opengl.GL20
import zio._
import zio3d.core.buffers.Buffers
import zio3d.core.gl._
import zio3d.engine._
import zio3d.engine.loaders.LoadingError
import zio3d.engine.loaders.LoadingError.{ProgramLinkError, ShaderCompileError}
import zio3d.engine.loaders.texture.TextureLoader

package object particle {

  type ParticleShaderInterpreter = Has[ParticleShaderInterpreter.Service]

  object ParticleShaderInterpreter {

    trait Service extends ShaderInterpreter.Service[SimpleMeshDefinition, ParticleShaderProgram]

    val live = ZLayer.fromEnvironment[PreShaderEnv, ParticleShaderInterpreter] { env =>
      Has(new Service {

        private val gl = env.get[GL.Service]
        private val buffers = env.get[Buffers.Service]
        private val textureLoader = env.get[TextureLoader.Service]

        private val strVertexShader =
          """#version 330
            |
            |layout (location=0) in vec3 position;
            |layout (location=1) in vec2 texCoord;
            |layout (location=2) in vec3 vertexNormal;
            |
            |out vec2 outTexCoord;
            |
            |uniform mat4 modelViewMatrix;
            |uniform mat4 projectionMatrix;
            |
            |uniform float texXOffset;
            |uniform float texYOffset;
            |uniform int numCols;
            |uniform int numRows;
            |
            |void main()
            |{
            |    gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
            |
            |    // Support for texture atlas, update texture coordinates
            |    float x = (texCoord.x / numCols + texXOffset);
            |    float y = (texCoord.y / numRows + texYOffset);
            |
            |    outTexCoord = vec2(x, y);
            |}
        """.stripMargin

        private val strFragmentShader =
          """#version 330
            |
            |in  vec2 outTexCoord;
            |out vec4 fragColor;
            |
            |uniform sampler2D textureSampler;
            |
            |void main()
            |{
            |    fragColor = texture(textureSampler, outTexCoord);
            |}
        """.stripMargin

        def loadShaderProgram: IO[LoadingError, ParticleShaderProgram] =
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
            uniTexSampler       <- gl.getUniformLocation(p, "textureSampler")
            uniTexXOffset       <- gl.getUniformLocation(p, "texXOffset")
            uniTexYOffset       <- gl.getUniformLocation(p, "texYOffset")
            uniNumCols          <- gl.getUniformLocation(p, "numCols")
            uniNumRows          <- gl.getUniformLocation(p, "numRows")

            positionAttr <- gl.getAttribLocation(p, "position")
            texCoordAttr <- gl.getAttribLocation(p, "texCoord")
            normalAttr   <- gl.getAttribLocation(p, "vertexNormal")
          } yield ParticleShaderProgram(
            p,
            uniModelViewMatrix,
            uniProjectionMatrix,
            uniTexSampler,
            uniTexXOffset,
            uniTexYOffset,
            uniNumCols,
            uniNumRows,
            positionAttr,
            texCoordAttr,
            normalAttr
          )

        override def loadMesh(program: ParticleShaderProgram, input: SimpleMeshDefinition) =
          for {
            vao <- gl.genVertexArrays()
            _   <- gl.bindVertexArray(vao)

            posVbo <- gl.genBuffers
            posBuf <- buffers.floatBuffer(input.positions)
            _      <- gl.bindBuffer(GL_ARRAY_BUFFER, posVbo)
            _      <- gl.bufferData(GL_ARRAY_BUFFER, posBuf, GL_STATIC_DRAW)
            _      <- gl.vertexAttribPointer(program.positionAttr, 3, GL_FLOAT, false, 0, 0)

            texVbo <- gl.genBuffers
            texBuf <- buffers.floatBuffer(input.texCoords)
            _      <- gl.bindBuffer(GL_ARRAY_BUFFER, texVbo)
            _      <- gl.bufferData(GL_ARRAY_BUFFER, texBuf, GL_STATIC_DRAW)
            _      <- gl.vertexAttribPointer(program.texCoordAttr, 2, GL_FLOAT, false, 0, 0)

            norVbo <- gl.genBuffers
            norBuf <- buffers.floatBuffer(input.normals)
            _      <- gl.bindBuffer(GL_ARRAY_BUFFER, norVbo)
            _      <- gl.bufferData(GL_ARRAY_BUFFER, norBuf, GL_STATIC_DRAW)
            _      <- gl.vertexAttribPointer(program.normalsAttr, 3, GL_FLOAT, false, 0, 0)

            indVbo <- gl.genBuffers
            indBuf <- buffers.intBuffer(input.indices)
            _      <- gl.bindBuffer(GL_ELEMENT_ARRAY_BUFFER, indVbo)
            _      <- gl.bufferData(GL_ELEMENT_ARRAY_BUFFER, indBuf, GL_STATIC_COPY)

            _ <- gl.bindBuffer(GL_ARRAY_BUFFER, VertexBufferObject.None)
            _ <- gl.bindVertexArray(VertexArrayObject.None)

            t <- textureLoader.loadMaterial(input.material)
          } yield Mesh(vao, List(posVbo, texVbo, indVbo), input.indices.length, t)

        override def render(
          program: ParticleShaderProgram,
          item: GameItem,
          transformation: Transformation,
          fixtures: Fixtures
        ) =
          gl.useProgram(program.program) *>
            gl.uniformMatrix4fv(program.uniProjectionMatrix, false, transformation.projectionMatrix) *>
            gl.uniform1i(program.uniTextureSampler, 0) *>
            gl.depthMask(false) *>
            gl.blendFunc(GL_SRC_ALPHA, GL_ONE) *>
            ZIO.foreach(item.model.meshes)(m => renderMesh(program, m, item.instances, transformation)) *>
            gl.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA) *>
            gl.depthMask(true) *>
            gl.useProgram(Program.None)

        private def renderMesh(
          program: ParticleShaderProgram,
          mesh: Mesh,
          items: List[ItemInstance],
          trans: Transformation
        ) =
          mesh.material.texture.fold(IO.unit)(bindTexture) *>
            gl.bindVertexArray(mesh.vao) *>
            gl.enableVertexAttribArray(program.positionAttr) *>
            gl.enableVertexAttribArray(program.texCoordAttr) *>
            gl.enableVertexAttribArray(program.normalsAttr) *>
            ZIO.foreach(items)(i => renderInstance(program, mesh, i, trans)) *>
            gl.disableVertexAttribArray(program.positionAttr) *>
            gl.disableVertexAttribArray(program.texCoordAttr) *>
            gl.disableVertexAttribArray(program.normalsAttr) *>
            gl.bindTexture(GL_TEXTURE_2D, Texture.None) *>
            gl.bindVertexArray(VertexArrayObject.None)

        private def renderInstance(
          program: ParticleShaderProgram,
          mesh: Mesh,
          item: ItemInstance,
          transformation: Transformation
        ) = {
          val textureAnim = item.textureAnimation
          val numCols     = textureAnim.fold(1)(_.cols)
          val numRows     = textureAnim.fold(1)(_.rows)
          val textPos     = textureAnim.fold(0)(_.currentFrame)
          val col         = textPos % numCols
          val row         = textPos / numCols
          val texXOffset  = col.toFloat / numCols
          val texYOffset  = row.toFloat / numRows

          gl.uniform1i(program.uniNumCols, numCols) *>
            gl.uniform1i(program.uniNumRows, numRows) *>
            gl.uniform1f(program.uniTexXOffset, texXOffset) *>
            gl.uniform1f(program.uniTexYOffset, texYOffset) *>
            gl.uniformMatrix4fv(program.uniModelViewMatrix, false, buildModelViewMatrix(transformation, item)) *>
            gl.drawElements(GL_TRIANGLES, mesh.vertexCount, GL_UNSIGNED_INT, 0)
        }

        private def buildModelViewMatrix(t: Transformation, i: ItemInstance) = {
          val modelMatrix = t.getModelMatrix(i)
          t.viewMatrix * t.viewMatrix.transpose3x3(modelMatrix)
        }

        private def bindTexture(texture: Texture) =
          gl.activeTexture(GL_TEXTURE0) *>
            gl.bindTexture(GL_TEXTURE_2D, texture)

        override def cleanup(program: ParticleShaderProgram): UIO[Unit] =
          gl.deleteProgram(program.program)
      })
    }
  }

  final def loadShaderProgram: ZIO[ParticleShaderInterpreter, LoadingError, ParticleShaderProgram] =
    ZIO.accessM(_.get.loadShaderProgram)

  final def render(
    program: ParticleShaderProgram,
    item: GameItem,
    transformation: Transformation,
    fixtures: Fixtures
  ): ZIO[ParticleShaderInterpreter, Nothing, Unit] =
    ZIO.accessM(_.get.render(program, item, transformation, fixtures))

  final def loadMesh(
    program: ParticleShaderProgram,
    input: SimpleMeshDefinition
  ): ZIO[ParticleShaderInterpreter, LoadingError, Mesh] =
    ZIO.accessM(_.get.loadMesh(program, input))

  final def cleanup(program: ParticleShaderProgram): ZIO[ParticleShaderInterpreter, Nothing, Unit] =
    ZIO.accessM(_.get.cleanup(program))
}
