package zio3d.core.glfw

final case class Window(value: Long) extends AnyVal

final case class WindowSize(width: Int, height: Int)

final case class CursorPos(x: Double, y: Double)

final case class Cursor(value: Long) extends AnyVal
