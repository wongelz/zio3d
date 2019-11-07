package zio3d.engine.shaders.simple

import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL13._
import org.lwjgl.opengl.GL15._
import org.lwjgl.opengl.GL20
import zio.{IO, ZIO}
import zio3d.core.buffers.Buffers
import zio3d.core.gl.GL
import zio3d.core.gl.GL._
import zio3d.engine._
import zio3d.engine.loaders.LoadingError.{ProgramLinkError, ShaderCompileError}
import zio3d.engine.loaders.texture.TextureLoader
import zio3d.engine.shaders.ShaderInterpreter
import zio3d.engine.shaders.simple.SimpleShaderInterpreter.SimpleShaderProgram

/**
 * Shader pipeline for rendering simple, static meshes.
 */
trait SimpleShaderInterpreter {
  def simpleShaderInterpreter: ShaderInterpreter.Service[SimpleMeshDefinition, SimpleShaderProgram]
}

object SimpleShaderInterpreter {

  final case class SimpleShaderProgram(
    program: Program,
    uniModelViewMatrix: UniformLocation,
    uniProjectionMatrix: UniformLocation,
    uniTextureSampler: UniformLocation,
    positionAttr: AttribLocation,
    texCoordAttr: AttribLocation,
    normalsAttr: AttribLocation
  )

  trait Live extends GL.Live with Buffers.Live with TextureLoader.Live with SimpleShaderInterpreter {
    val simpleShaderInterpreter = new ShaderInterpreter.Service[SimpleMeshDefinition, SimpleShaderProgram] {
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
          |void main()
          |{
          |    gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
          |    outTexCoord = texCoord;
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
          uniTexSampler       <- gl.getUniformLocation(p, "textureSampler")

          positionAttr <- gl.getAttribLocation(p, "position")
          texCoordAttr <- gl.getAttribLocation(p, "texCoord")
          normalAttr   <- gl.getAttribLocation(p, "vertexNormal")

        } yield SimpleShaderProgram(
          p,
          uniModelViewMatrix,
          uniProjectionMatrix,
          uniTexSampler,
          positionAttr,
          texCoordAttr,
          normalAttr
        )

      def loadMesh(program: SimpleShaderProgram, input: SimpleMeshDefinition) =
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

      def render(
        program: SimpleShaderProgram,
        items: Iterable[GameItem],
        transformation: Transformation,
        fixtures: Fixtures
      ): IO[Nothing, Unit] =
        gl.useProgram(program.program) *>
          gl.uniformMatrix4fv(program.uniProjectionMatrix, false, transformation.projectionMatrix) *>
          gl.uniform1i(program.uniTextureSampler, 0) *>
          ZIO.foreach(items)(i => renderItem(program, i, transformation)) *>
          gl.useProgram(Program.None)

      private def renderItem(program: SimpleShaderProgram, item: GameItem, transformation: Transformation) =
        gl.uniformMatrix4fv(program.uniModelViewMatrix, false, transformation.getModelViewMatrix(item)) *>
          ZIO.foreach(item.meshes)(m => renderMesh(program, m))

      private def renderMesh(program: SimpleShaderProgram, mesh: Mesh) =
        mesh.material.texture.fold(IO.unit)(bindTexture) *>
          gl.bindVertexArray(mesh.vao) *>
          gl.enableVertexAttribArray(program.positionAttr) *>
          gl.enableVertexAttribArray(program.texCoordAttr) *>
          gl.enableVertexAttribArray(program.normalsAttr) *>
          gl.drawElements(GL_TRIANGLES, mesh.vertexCount, GL_UNSIGNED_INT, 0) *>
          gl.disableVertexAttribArray(program.positionAttr) *>
          gl.disableVertexAttribArray(program.texCoordAttr) *>
          gl.disableVertexAttribArray(program.normalsAttr) *>
          gl.bindTexture(GL_TEXTURE_2D, Texture.None) *>
          gl.bindVertexArray(VertexArrayObject.None)

      private def bindTexture(texture: Texture) =
        gl.activeTexture(GL_TEXTURE0) *>
          gl.bindTexture(GL_TEXTURE_2D, texture)

    }
  }

  object Live extends Live
}
