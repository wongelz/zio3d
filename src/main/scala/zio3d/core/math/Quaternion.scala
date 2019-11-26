package zio3d.core.math

final case class Quaternion(x: Float, y: Float, z: Float, w: Float) {
  def vector: Vector4 = Vector4(x, y, z, w)

  def *(rhs: Quaternion): Quaternion = multiply(rhs)

  def multiply(rhs: Quaternion): Quaternion =
    Quaternion(
      this.w * rhs.x + this.x * rhs.w + this.y * rhs.z - this.z * rhs.y,
      this.w * rhs.y + this.y * rhs.w + this.z * rhs.x - this.x * rhs.z,
      this.w * rhs.z + this.z * rhs.w + this.x * rhs.y - this.y * rhs.x,
      this.w * rhs.w - this.x * rhs.x - this.y * rhs.y - this.z * rhs.z
    )

  def transform(x: Float, y: Float, z: Float): Vector3 = {
    val w2  = this.w * this.w
    val x2  = this.x * this.x
    val y2  = this.y * this.y
    val z2  = this.z * this.z
    val zw  = this.z * this.w
    val xy  = this.x * this.y
    val xz  = this.x * this.z
    val yw  = this.y * this.w
    val yz  = this.y * this.z
    val xw  = this.x * this.w
    val m00 = w2 + x2 - z2 - y2
    val m01 = xy + zw + zw + xy
    val m02 = xz - yw + xz - yw
    val m10 = -zw + xy - zw + xy
    val m11 = y2 - z2 + w2 - x2
    val m12 = yz + yz + xw + xw
    val m20 = yw + xz + xz + yw
    val m21 = yz + yz - xw - xw
    val m22 = z2 - y2 - x2 + w2

    Vector3(m00 * x + m10 * y + m20 * z, m01 * x + m11 * y + m21 * z, m02 * x + m12 * y + m22 * z)
  }
}

object Quaternion {

  val Zero = Quaternion(0, 0, 0, 1)
}
