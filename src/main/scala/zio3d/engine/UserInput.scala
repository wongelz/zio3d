package zio3d.engine

import org.lwjgl.glfw.GLFW._
import zio3d.core.glfw.CursorPos
import zio3d.core.math.Vector2

sealed abstract class Key(val value: Int)
object Key {
  case object UP    extends Key(GLFW_KEY_W)
  case object DOWN  extends Key(GLFW_KEY_S)
  case object LEFT  extends Key(GLFW_KEY_A)
  case object RIGHT extends Key(GLFW_KEY_D)
  case object ESC   extends Key(GLFW_KEY_ESCAPE)

  val values: List[Key] = List(UP, DOWN, LEFT, RIGHT, ESC)
}

sealed abstract class MouseButton(val value: Int)
object MouseButton {
  case object BUTTON_LEFT  extends MouseButton(GLFW_MOUSE_BUTTON_LEFT)
  case object BUTTON_RIGHT extends MouseButton(GLFW_MOUSE_BUTTON_RIGHT)

  val values: List[MouseButton] = List(BUTTON_LEFT, BUTTON_RIGHT)
}

final case class UserInput(
  keys: Set[Key],
  mouseButtons: Set[MouseButton],
  prevCursor: CursorPos,
  currCursor: CursorPos
) {

  lazy val cursorMovement =
    Vector2((currCursor.x - prevCursor.x).toFloat, (currCursor.y - prevCursor.y).toFloat)
}
