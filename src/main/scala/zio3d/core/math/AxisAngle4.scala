package zio3d.core.math

final case class AxisAngle4(
  angle: Float, // radians
  x: Float,
  y: Float,
  z: Float
) {

  def normalize: AxisAngle4 = {
    val invLength = 1.0f / sqrt(x * x + y * y + z * z)
    AxisAngle4(angle, x * invLength, y * invLength, z * invLength)
  }
}

object AxisAngle4 {
  private val `2PI` = Math.PI + Math.PI

  def apply(angle: Float, x: Float, y: Float, z: Float): AxisAngle4 = {
    val ang = (if (angle < 0.0) `2PI` + angle % `2PI` else angle.toDouble) % `2PI`
    new AxisAngle4(ang.toFloat, x, y, z)
  }
}
