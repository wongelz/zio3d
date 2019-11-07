package zio3d.engine

/**
 * A 2D box defined by its corner (top-left) coordinates, width and height.
 */
final case class Box2D(
  x: Float,
  y: Float,
  width: Float,
  height: Float
) {

  def contains(x2: Float, y2: Float): Boolean =
    x2 >= x &&
      y2 >= y &&
      x2 < x + width &&
      y2 < y + height
}
