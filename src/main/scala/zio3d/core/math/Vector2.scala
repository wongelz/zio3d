package zio3d.core.math

final case class Vector2(
  x: Float,
  y: Float
)

object Vector2 {
  def apply(): Vector2 =
    origin

  lazy val origin = Vector2(0, 0)
}
