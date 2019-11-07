package zio3d.core.gl

import java.nio.{ByteBuffer, FloatBuffer, IntBuffer}

import org.lwjgl.opengl._
import org.lwjgl.system.MemoryStack
import zio.{IO, UIO}
import zio3d.core.math.{Matrix4, Vector3, Vector4}

trait GL {
  def gl: GL.Service
}

object GL {
  final case class Shader(ref: Int) extends AnyVal

  final case class Program(ref: Int) extends AnyVal

  final case class VertexArrayObject(ref: Int) extends AnyVal

  object VertexArrayObject {
    final val None = VertexArrayObject(0)
  }

  final case class VertexBufferObject(ref: Int) extends AnyVal

  object VertexBufferObject {
    final val None = VertexBufferObject(0)
  }

  final case class UniformLocation(loc: Int) extends AnyVal

  final case class AttribLocation(loc: Int) extends AnyVal

  object Program {
    final val None = Program(0)
  }

  final case class Texture(value: Int) extends AnyVal

  object Texture {
    final val None = Texture(0)
  }

  trait Service {
    def createCapabilities: UIO[GLCapabilities]

    def enable(target: Int): UIO[Unit]

    def cullFace(mode: Int): UIO[Unit]

    def clearColor(r: Float, g: Float, b: Float, a: Float): UIO[Unit]

    def clear(mask: Int): UIO[Unit]

    def createShader(`type`: Int): UIO[Shader]

    def deleteShader(shader: Shader): UIO[Unit]

    def createProgram: UIO[Program]

    def useProgram(program: Program): UIO[Unit]

    def shaderSource(shader: Shader, src: CharSequence*): UIO[Unit]

    def compileShader(shader: Shader): UIO[Unit]

    def getShaderi(shader: Shader, pname: Int): UIO[Int]

    def getShaderInfoLog(shader: Shader): UIO[String]

    def attachShader(program: Program, shader: Shader): UIO[Unit]

    def linkProgram(program: Program): UIO[Unit]

    def getProgrami(program: Program, pname: Int): UIO[Int]

    def getProgramInfoLog(program: Program): UIO[String]

    def genBuffers: UIO[VertexBufferObject]

    def bindBuffer(target: Int, buffer: VertexBufferObject): UIO[Unit]

    def bufferData(target: Int, data: IntBuffer, usage: Int): UIO[Unit]

    def bufferData(target: Int, data: FloatBuffer, usage: Int): UIO[Unit]

    def getAttribLocation(program: Program, name: String): UIO[AttribLocation]

    def enableVertexAttribArray(index: AttribLocation): UIO[Unit]

    def vertexAttribPointer(
      index: AttribLocation,
      size: Int,
      `type`: Int,
      normalized: Boolean,
      stride: Int,
      pointer: Long
    ): UIO[Unit]

    def drawArrays(mode: Int, first: Int, count: Int): UIO[Unit]

    def drawElements(mode: Int, count: Int, `type`: Int, indices: Long): UIO[Unit]

    def disableVertexAttribArray(index: AttribLocation): UIO[Unit]

    def genVertexArrays(): UIO[VertexArrayObject]

    def bindVertexArray(array: VertexArrayObject): UIO[Unit]

    def bindFragDataLocation(program: Program, colorNumber: Int, name: String): UIO[Unit]

    def uniformMatrix4fv(location: UniformLocation, transpose: Boolean, value: FloatBuffer): UIO[Unit]

    def uniformMatrix4fv(location: UniformLocation, transpose: Boolean, value: Matrix4): UIO[Unit]

    def uniformMatrix4fvs(location: UniformLocation, transpose: Boolean, values: Array[Matrix4]): UIO[Unit]

    def uniform1i(uniform: UniformLocation, value: Int): UIO[Unit]

    def uniform1f(uniform: UniformLocation, value: Float): UIO[Unit]

    def uniform3f(uniform: UniformLocation, value1: Float, value2: Float, value3: Float): UIO[Unit]

    def uniform3f(uniform: UniformLocation, value: Vector3): UIO[Unit]

    def uniform4f(uniform: UniformLocation, value1: Float, value2: Float, value3: Float, value4: Float): UIO[Unit]

    def uniform4f(uniform: UniformLocation, value: Vector4): UIO[Unit]

    def getUniformLocation(program: Program, name: String): UIO[UniformLocation]

    def depthFunc(func: Int): UIO[Unit]

    def depthMask(flag: Boolean): UIO[Unit]

    def blendFunc(sfactor: Int, dfactor: Int): UIO[Unit]

    def genTextures: UIO[Texture]

    def bindTexture(target: Int, texture: Texture): UIO[Unit]

    def activeTexture(texture: Int): UIO[Unit]

    def texParameter(target: Int, pname: Int, param: Int): UIO[Unit]

    def pixelStorei(pname: Int, param: Int): UIO[Unit]

    def texImage2D(
      target: Int,
      level: Int,
      internalFormat: Int,
      width: Int,
      height: Int,
      border: Int,
      format: Int,
      `type`: Int,
      pixels: ByteBuffer
    ): UIO[Unit]

    def generateMipMap(target: Int): UIO[Unit]

    def compileShader(shaderType: Int, src: String): IO[String, Shader]
  }

  trait Live extends GL {
    val gl: GL.Service = new Service {
      def createCapabilities: UIO[GLCapabilities] =
        IO.effectTotal { org.lwjgl.opengl.GL.createCapabilities() }

      def enable(target: Int): UIO[Unit] =
        IO.effectTotal { GL11.glEnable(target) }

      def cullFace(mode: Int): UIO[Unit] =
        IO.effectTotal { GL11.glCullFace(mode) }

      def clearColor(r: Float, g: Float, b: Float, a: Float): UIO[Unit] =
        IO.effectTotal { GL11.glClearColor(r, g, b, a) }

      def clear(mask: Int): UIO[Unit] =
        IO.effectTotal { GL11.glClear(mask) }

      def createShader(`type`: Int): UIO[Shader] =
        IO.effectTotal { Shader(GL20.glCreateShader(`type`)) }

      def deleteShader(shader: Shader): UIO[Unit] =
        IO.effectTotal { GL20.glDeleteShader(shader.ref) }

      def createProgram: UIO[Program] =
        IO.effectTotal { Program(GL20.glCreateProgram()) }

      def useProgram(program: Program): UIO[Unit] =
        IO.effectTotal { GL20.glUseProgram(program.ref) }

      def shaderSource(shader: Shader, src: CharSequence*): UIO[Unit] =
        IO.effectTotal { GL20.glShaderSource(shader.ref, src: _*) }

      def compileShader(shader: Shader): UIO[Unit] =
        IO.effectTotal { GL20.glCompileShader(shader.ref) }

      def getShaderi(shader: Shader, pname: Int): UIO[Int] =
        IO.effectTotal { GL20.glGetShaderi(shader.ref, pname) }

      def getShaderInfoLog(shader: Shader): UIO[String] =
        IO.effectTotal { GL20.glGetShaderInfoLog(shader.ref) }

      def attachShader(program: Program, shader: Shader): UIO[Unit] =
        IO.effectTotal { GL20.glAttachShader(program.ref, shader.ref) }

      def linkProgram(program: Program): UIO[Unit] =
        IO.effectTotal { GL20.glLinkProgram(program.ref) }

      def getProgrami(program: Program, pname: Int): UIO[Int] =
        IO.effectTotal { GL20.glGetProgrami(program.ref, pname) }

      def getProgramInfoLog(program: Program): UIO[String] =
        IO.effectTotal { GL20.glGetProgramInfoLog(program.ref) }

      def genBuffers: UIO[VertexBufferObject] =
        IO.effectTotal { VertexBufferObject(GL15.glGenBuffers()) }

      def bindBuffer(target: Int, buffer: VertexBufferObject): UIO[Unit] =
        IO.effectTotal { GL15.glBindBuffer(target, buffer.ref) }

      def bufferData(target: Int, data: IntBuffer, usage: Int): UIO[Unit] =
        IO.effectTotal { GL15.glBufferData(target, data, usage) }

      def bufferData(target: Int, data: FloatBuffer, usage: Int): UIO[Unit] =
        IO.effectTotal { GL15.glBufferData(target, data, usage) }

      def getAttribLocation(program: Program, name: String): UIO[AttribLocation] =
        IO.effectTotal { AttribLocation(GL20.glGetAttribLocation(program.ref, name)) }

      def enableVertexAttribArray(index: AttribLocation): UIO[Unit] =
        IO.effectTotal { GL20.glEnableVertexAttribArray(index.loc) }

      def vertexAttribPointer(
        index: AttribLocation,
        size: Int,
        `type`: Int,
        normalized: Boolean,
        stride: Int,
        pointer: Long
      ): UIO[Unit] =
        IO.effectTotal { GL20.glVertexAttribPointer(index.loc, size, `type`, normalized, stride, pointer) }

      def drawArrays(mode: Int, first: Int, count: Int): UIO[Unit] =
        IO.effectTotal { GL11.glDrawArrays(mode, first, count) }

      def drawElements(mode: Int, count: Int, `type`: Int, indices: Long) =
        IO.effectTotal { GL11.glDrawElements(mode, count, `type`, indices) }

      def disableVertexAttribArray(index: AttribLocation): UIO[Unit] =
        IO.effectTotal { GL20.glDisableVertexAttribArray(index.loc) }

      def genVertexArrays(): UIO[VertexArrayObject] =
        IO.effectTotal { VertexArrayObject(GL30.glGenVertexArrays()) }

      def bindVertexArray(array: VertexArrayObject): UIO[Unit] =
        IO.effectTotal { GL30.glBindVertexArray(array.ref) }

      def bindFragDataLocation(program: Program, colorNumber: Int, name: String): UIO[Unit] =
        IO.effectTotal { GL30.glBindFragDataLocation(program.ref, colorNumber, name) }

      def uniformMatrix4fv(location: UniformLocation, transpose: Boolean, value: FloatBuffer): UIO[Unit] =
        IO.effectTotal { GL20.glUniformMatrix4fv(location.loc, transpose, value) }

      def uniformMatrix4fv(location: UniformLocation, transpose: Boolean, m: Matrix4): UIO[Unit] =
        IO.effectTotal(MemoryStack.stackPush()).bracket(s => IO.effectTotal(s.close())) { stack =>
          IO.effectTotal {
            val buffer = stack.mallocFloat(16)
            put(buffer, m, 0)
            GL20.glUniformMatrix4fv(location.loc, false, buffer)
          }
        }

      def uniformMatrix4fvs(location: UniformLocation, transpose: Boolean, matrices: Array[Matrix4]): UIO[Unit] =
        IO.effectTotal(MemoryStack.stackPush()).bracket(s => IO.effectTotal(s.close())) { stack =>
          IO.effectTotal {
            val length = matrices.length
            val buffer = stack.mallocFloat(16 * length)
            for (i <- 0 until length) {
              put(buffer, matrices(i), 16 * i)
            }
            GL20.glUniformMatrix4fv(location.loc, false, buffer)
          }
        }

      private def put(buffer: FloatBuffer, m: Matrix4, offset: Int) = {
        buffer.put(offset + 0, m.m00)
        buffer.put(offset + 1, m.m01)
        buffer.put(offset + 2, m.m02)
        buffer.put(offset + 3, m.m03)
        buffer.put(offset + 4, m.m10)
        buffer.put(offset + 5, m.m11)
        buffer.put(offset + 6, m.m12)
        buffer.put(offset + 7, m.m13)
        buffer.put(offset + 8, m.m20)
        buffer.put(offset + 9, m.m21)
        buffer.put(offset + 10, m.m22)
        buffer.put(offset + 11, m.m23)
        buffer.put(offset + 12, m.m30)
        buffer.put(offset + 13, m.m31)
        buffer.put(offset + 14, m.m32)
        buffer.put(offset + 15, m.m33)
      }

      def uniform1i(uniform: UniformLocation, value: Int): UIO[Unit] =
        IO.effectTotal { GL20.glUniform1i(uniform.loc, value) }

      def uniform1f(uniform: UniformLocation, value: Float): UIO[Unit] =
        IO.effectTotal { GL20.glUniform1f(uniform.loc, value) }

      def uniform3f(uniform: UniformLocation, value1: Float, value2: Float, value3: Float): UIO[Unit] =
        IO.effectTotal { GL20.glUniform3f(uniform.loc, value1, value2, value3) }

      def uniform3f(uniform: UniformLocation, value: Vector3): UIO[Unit] =
        uniform3f(uniform, value.x, value.y, value.z)

      def uniform4f(uniform: UniformLocation, value1: Float, value2: Float, value3: Float, value4: Float): UIO[Unit] =
        IO.effectTotal { GL20.glUniform4f(uniform.loc, value1, value2, value3, value4) }

      def uniform4f(uniform: UniformLocation, value: Vector4): UIO[Unit] =
        uniform4f(uniform, value.x, value.y, value.z, value.w)

      def getUniformLocation(program: Program, name: String): UIO[UniformLocation] =
        IO.effectTotal { UniformLocation(GL20.glGetUniformLocation(program.ref, name)) }

      def depthFunc(func: Int): UIO[Unit] =
        IO.effectTotal { GL11.glDepthFunc(func) }

      def depthMask(flag: Boolean): UIO[Unit] =
        IO.effectTotal { GL11.glDepthMask(flag) }

      def blendFunc(sfactor: Int, dfactor: Int): UIO[Unit] =
        IO.effectTotal { GL11.glBlendFunc(sfactor, dfactor) }

      def genTextures: UIO[Texture] =
        IO.effectTotal { Texture(GL11.glGenTextures()) }

      def bindTexture(target: Int, texture: Texture): UIO[Unit] =
        IO.effectTotal { GL11.glBindTexture(target, texture.value) }

      def activeTexture(texture: Int): UIO[Unit] =
        IO.effectTotal { GL13.glActiveTexture(texture) }

      def texParameter(target: Int, pname: Int, param: Int): UIO[Unit] =
        IO.effectTotal { GL11.glTexParameteri(target, pname, param) }

      def pixelStorei(pname: Int, param: Int): UIO[Unit] =
        IO.effectTotal { GL11.glPixelStorei(pname, param) }

      def texImage2D(
        target: Int,
        level: Int,
        internalFormat: Int,
        width: Int,
        height: Int,
        border: Int,
        format: Int,
        `type`: Int,
        pixels: ByteBuffer
      ) =
        IO.effectTotal {
          GL11.glTexImage2D(target, level, internalFormat, width, height, border, format, `type`, pixels)
        }

      def generateMipMap(target: Int): UIO[Unit] =
        IO.effectTotal { GL30.glGenerateMipmap(target) }

      def compileShader(shaderType: Int, src: String) =
        for {
          s  <- gl.createShader(shaderType)
          _  <- gl.shaderSource(s, src)
          _  <- gl.compileShader(s)
          ok <- gl.getShaderi(s, GL20.GL_COMPILE_STATUS)
          _  <- gl.getShaderInfoLog(s).flatMap(log => IO.fail(log)).when(ok == 0)
        } yield s
    }
  }

  object Live extends Live
}
