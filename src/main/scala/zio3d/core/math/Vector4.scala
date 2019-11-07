package zio3d.core.math

final case class Vector4(
  x: Float,
  y: Float,
  z: Float,
  w: Float
) {

  def mul(mat: Matrix4): Vector4 =
    Vector4(
      mat.m00 * x + mat.m10 * y + mat.m20 * z + mat.m30 * w,
      mat.m01 * x + mat.m11 * y + mat.m21 * z + mat.m31 * w,
      mat.m02 * x + mat.m12 * y + mat.m22 * z + mat.m32 * w,
      w
    )
}

object Vector4 {
  def apply(vector3: Vector3, w: Float): Vector4 =
    Vector4(vector3.x, vector3.y, vector3.z, w)
}
