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

  def quaternion: Quaternion = {
    val s = sin(angle * 0.5f)
    val c = cos(angle * 0.5f)
    Quaternion(
      x * s,
      y * s,
      z * s,
      c
    )
  }
}

object AxisAngle4 {
  private val `2PI` = Math.PI + Math.PI

  def apply(angle: Float, x: Float, y: Float, z: Float): AxisAngle4 = {
    val ang = (if (angle < 0.0) `2PI` + angle % `2PI` else angle.toDouble) % `2PI`
    new AxisAngle4(ang.toFloat, x, y, z)
  }

  def x(angle: Float): AxisAngle4 =
    apply(angle, 1, 0, 0)

  def y(angle: Float): AxisAngle4 =
    apply(angle, 0, 1, 0)

  def z(angle: Float): AxisAngle4 =
    apply(angle, 0, 0, 1)
}
