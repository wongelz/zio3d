package zio3d.core.gl

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
