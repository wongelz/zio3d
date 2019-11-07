package zio3d.core.math

import zio3d.core.math

final case class Matrix4(
  m00: Float,
  m01: Float,
  m02: Float,
  m03: Float,
  m10: Float,
  m11: Float,
  m12: Float,
  m13: Float,
  m20: Float,
  m21: Float,
  m22: Float,
  m23: Float,
  m30: Float,
  m31: Float,
  m32: Float,
  m33: Float
) {

  def *(m: Matrix4): Matrix4 = multiply(m)

  def multiply(m: Matrix4): Matrix4 =
    Matrix4(
      m00 * m.m00 + m10 * m.m01 + m20 * m.m02 + m30 * m.m03,
      m01 * m.m00 + m11 * m.m01 + m21 * m.m02 + m31 * m.m03,
      m02 * m.m00 + m12 * m.m01 + m22 * m.m02 + m32 * m.m03,
      m03 * m.m00 + m13 * m.m01 + m23 * m.m02 + m33 * m.m03,
      m00 * m.m10 + m10 * m.m11 + m20 * m.m12 + m30 * m.m13,
      m01 * m.m10 + m11 * m.m11 + m21 * m.m12 + m31 * m.m13,
      m02 * m.m10 + m12 * m.m11 + m22 * m.m12 + m32 * m.m13,
      m03 * m.m10 + m13 * m.m11 + m23 * m.m12 + m33 * m.m13,
      m00 * m.m20 + m10 * m.m21 + m20 * m.m22 + m30 * m.m23,
      m01 * m.m20 + m11 * m.m21 + m21 * m.m22 + m31 * m.m23,
      m02 * m.m20 + m12 * m.m21 + m22 * m.m22 + m32 * m.m23,
      m03 * m.m20 + m13 * m.m21 + m23 * m.m22 + m33 * m.m23,
      m00 * m.m30 + m10 * m.m31 + m20 * m.m32 + m30 * m.m33,
      m01 * m.m30 + m11 * m.m31 + m21 * m.m32 + m31 * m.m33,
      m02 * m.m30 + m12 * m.m31 + m22 * m.m32 + m32 * m.m33,
      m03 * m.m30 + m13 * m.m31 + m23 * m.m32 + m33 * m.m33
    )

  def *(vec: Vector4): Vector4 = multiply(vec)

  def multiply(vec: Vector4): Vector4 =
    Vector4(
      m00 * vec.x + m10 * vec.y + m20 * vec.z + m30 * vec.w,
      m01 * vec.x + m11 * vec.y + m21 * vec.z + m31 * vec.w,
      m02 * vec.x + m12 * vec.y + m22 * vec.z + m32 * vec.w,
      m03 * vec.x + m13 * vec.y + m23 * vec.z + m33 * vec.w
    )

  def translate(translation: Vector3): Matrix4 =
    this * Matrix4.forTranslation(translation)

  /**
   * @param angle in radians.
   * @return
   */
  def rotateX(angle: Float): Matrix4 = {
    val sin  = math.sin(angle)
    val cos  = math.cos(angle)
    val rm11 = cos
    val rm12 = sin
    val rm21 = -sin
    val rm22 = cos

    // add temporaries for dependent values
    val nm10 = m10 * rm11 + m20 * rm12
    val nm11 = m11 * rm11 + m21 * rm12
    val nm12 = m12 * rm11 + m22 * rm12
    val nm13 = m13 * rm11 + m23 * rm12

    Matrix4(
      m00,
      m01,
      m02,
      m03,
      nm10,
      nm11,
      nm12,
      nm13,
      m10 * rm21 + m20 * rm22,
      m11 * rm21 + m21 * rm22,
      m12 * rm21 + m22 * rm22,
      m13 * rm21 + m23 * rm22,
      m30,
      m31,
      m32,
      m33
    )
  }

  def rotateY(angle: Float): Matrix4 = {
    val sin  = math.sin(angle)
    val cos  = math.cos(angle)
    val rm00 = cos
    val rm02 = -sin
    val rm20 = sin
    val rm22 = cos

    // add temporaries for dependent values
    val nm00 = m00 * rm00 + m20 * rm02
    val nm01 = m01 * rm00 + m21 * rm02
    val nm02 = m02 * rm00 + m22 * rm02
    val nm03 = m03 * rm00 + m23 * rm02
    Matrix4(
      nm00,
      nm01,
      nm02,
      nm03,
      m10,
      m11,
      m12,
      m13,
      m00 * rm20 + m20 * rm22,
      m01 * rm20 + m21 * rm22,
      m02 * rm20 + m22 * rm22,
      m03 * rm20 + m23 * rm22,
      m30,
      m31,
      m32,
      m33
    )
  }

  def rotateZ(angle: Float): Matrix4 =
    rotateTowardsXY(math.sin(angle), math.cos(angle))

  def rotateTowardsXY(dirX: Float, dirY: Float): Matrix4 = {
    val rm00 = dirY
    val rm01 = dirX
    val rm10 = -dirX
    val rm11 = dirY
    val nm00 = m00 * rm00 + m10 * rm01
    val nm01 = m01 * rm00 + m11 * rm01
    val nm02 = m02 * rm00 + m12 * rm01
    val nm03 = m03 * rm00 + m13 * rm01
    Matrix4(
      nm00,
      nm01,
      nm02,
      nm03,
      m00 * rm10 + m10 * rm11,
      m01 * rm10 + m11 * rm11,
      m02 * rm10 + m12 * rm11,
      m03 * rm10 + m13 * rm11,
      m20,
      m21,
      m22,
      m23,
      m30,
      m31,
      m32,
      m33
    )
  }

  def rotate(angle: Float, axis: Vector3): Matrix4 =
    rotate(angle, axis.x, axis.y, axis.z)

  def rotate(angle: Float, x: Float, y: Float, z: Float): Matrix4 = {
    val s    = math.sin(angle)
    val c    = cos(angle)
    val C    = 1.0f - c
    val xx   = x * x
    val xy   = x * y
    val xz   = x * z
    val yy   = y * y
    val yz   = y * z
    val zz   = z * z
    val rm00 = xx * C + c
    val rm01 = xy * C + z * s
    val rm02 = xz * C - y * s
    val rm10 = xy * C - z * s
    val rm11 = yy * C + c
    val rm12 = yz * C + x * s
    val rm20 = xz * C + y * s
    val rm21 = yz * C - x * s
    val rm22 = zz * C + c
    val nm00 = m00 * rm00 + m10 * rm01 + m20 * rm02
    val nm01 = m01 * rm00 + m11 * rm01 + m21 * rm02
    val nm02 = m02 * rm00 + m12 * rm01 + m22 * rm02
    val nm03 = m03 * rm00 + m13 * rm01 + m23 * rm02
    val nm10 = m00 * rm10 + m10 * rm11 + m20 * rm12
    val nm11 = m01 * rm10 + m11 * rm11 + m21 * rm12
    val nm12 = m02 * rm10 + m12 * rm11 + m22 * rm12
    val nm13 = m03 * rm10 + m13 * rm11 + m23 * rm12

    Matrix4(
      nm00,
      nm01,
      nm02,
      nm03,
      nm10,
      nm11,
      nm12,
      nm13,
      m00 * rm20 + m10 * rm21 + m20 * rm22,
      m01 * rm20 + m11 * rm21 + m21 * rm22,
      m02 * rm20 + m12 * rm21 + m22 * rm22,
      m03 * rm20 + m13 * rm21 + m23 * rm22,
      m30,
      m31,
      m32,
      m33
    )
  }

  def rotate(orientation: Quaternion): Matrix4 =
    this * Matrix4.forRotation(orientation)

  def scale(xyz: Float): Matrix4 =
    scale(xyz, xyz, xyz)

  def scale(x: Float, y: Float, z: Float): Matrix4 =
    this * Matrix4.forScale(Vector3(x, y, z))

  def transpose3x3(dest: Matrix4): Matrix4 = {
    val nm00 = m00
    val nm01 = m10
    val nm02 = m20
    val nm10 = m01
    val nm11 = m11
    val nm12 = m21
    val nm20 = m02
    val nm21 = m12
    val nm22 = m22
    dest.copy(
      m00 = nm00,
      m01 = nm01,
      m02 = nm02,
      m10 = nm10,
      m11 = nm11,
      m12 = nm12,
      m20 = nm20,
      m21 = nm21,
      m22 = nm22
    )
  }

  def invert(): Matrix4 = {
    val a = m00 * m11 - m01 * m10
    val b = m00 * m12 - m02 * m10
    val c = m00 * m13 - m03 * m10
    val d = m01 * m12 - m02 * m11
    val e = m01 * m13 - m03 * m11
    val f = m02 * m13 - m03 * m12
    val g = m20 * m31 - m21 * m30
    val h = m20 * m32 - m22 * m30
    val i = m20 * m33 - m23 * m30
    val j = m21 * m32 - m22 * m31
    val k = m21 * m33 - m23 * m31
    val l = m22 * m33 - m23 * m32

    // can get exception if denominator is 0
    val determinant = 1.0f / (a * l - b * k + c * j + d * i - e * h + f * g)
    val nm00        = (m11 * l - m12 * k + m13 * j) * determinant
    val nm01        = (-m01 * l + m02 * k - m03 * j) * determinant
    val nm02        = (m31 * f - m32 * e + m33 * d) * determinant
    val nm03        = (-m21 * f + m22 * e - m23 * d) * determinant
    val nm10        = (-m10 * l + m12 * i - m13 * h) * determinant
    val nm11        = (m00 * l - m02 * i + m03 * h) * determinant
    val nm12        = (-m30 * f + m32 * c - m33 * b) * determinant
    val nm13        = (m20 * f - m22 * c + m23 * b) * determinant
    val nm20        = (m10 * k - m11 * i + m13 * g) * determinant
    val nm21        = (-m00 * k + m01 * i - m03 * g) * determinant
    val nm22        = (m30 * e - m31 * c + m33 * a) * determinant
    val nm23        = (-m20 * e + m21 * c - m23 * a) * determinant
    val nm30        = (-m10 * j + m11 * h - m12 * g) * determinant
    val nm31        = (m00 * j - m01 * h + m02 * g) * determinant
    val nm32        = (-m30 * d + m31 * b - m32 * a) * determinant
    val nm33        = (m20 * d - m21 * b + m22 * a) * determinant

    Matrix4(nm00, nm01, nm02, nm03, nm10, nm11, nm12, nm13, nm20, nm21, nm22, nm23, nm30, nm31, nm32, nm33)
  }
}

object Matrix4 {

  lazy val identity = Matrix4(1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1)

  def apply(): Matrix4 = identity

  def forTranslation(translation: Vector3): Matrix4 =
    forTranslation(translation.x, translation.y, translation.z)

  def forTranslation(x: Float, y: Float, z: Float): Matrix4 =
    Matrix4(1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, x, y, z, 1)

  def forScale(scale: Vector3): Matrix4 =
    Matrix4(scale.x, 0, 0, 0, 0, scale.y, 0, 0, 0, 0, scale.z, 0, 0, 0, 0, 1)

  def forRotation(q: Quaternion): Matrix4 = {
    val Quaternion(x, y, z, w) = q
    val x2                     = x + x
    val y2                     = y + y
    val z2                     = z + z

    val xx = x * x2
    val xy = x * y2
    val xz = x * z2

    val yy = y * y2
    val yz = y * z2
    val zz = z * z2

    val wx = w * x2
    val wy = w * y2
    val wz = w * z2

    Matrix4(
      1 - (yy + zz),
      xy + wz,
      xz - wy,
      0,
      xy - wz,
      1 - (xx + zz),
      yz + wx,
      0,
      xz + wy,
      yz - wx,
      1 - (xx + yy),
      0,
      0,
      0,
      0,
      1
    )
  }

  def forPerspective(fovRad: Float, aspect: Float, near: Float, far: Float): Matrix4 = {
    val fov = 1 / math.tan(fovRad / 2f)
    Matrix4(
      fov / aspect,
      0,
      0,
      0,
      0,
      fov,
      0,
      0,
      0,
      0,
      (far + near) / (near - far),
      -1,
      0,
      0,
      (2 * far * near) / (near - far),
      0
    )
  }

  def lookAt(eye: Vector3, center: Vector3, up: Vector3) = {
    val dx = eye.x - center.x
    val dy = eye.y - center.y
    val dz = eye.z - center.z

    val invDirLen = 1.0f / sqrt(dx * dx + dy * dy + dz * dz)
    val ndx       = dx * invDirLen
    val ndy       = dy * invDirLen
    val ndz       = dz * invDirLen

    val lx = up.y * ndz - up.z * ndy
    val ly = up.z * ndx - up.x * ndz
    val lz = up.x * ndy - up.y * ndx

    val invLeftLen = 1.0f / sqrt(lx * lx + ly * ly + lz * lz)
    val nlx        = lx * invLeftLen
    val nly        = ly * invLeftLen
    val nlz        = lz * invLeftLen

    val upnx = ndy * nlz - ndz * nly
    val upny = ndz * nlx - ndx * nlz
    val upnz = ndx * nly - ndy * nlx

    Matrix4(
      nlx,
      upnx,
      ndx,
      0.0f,
      nly,
      upny,
      ndy,
      0.0f,
      nlz,
      upnz,
      ndz,
      0.0f,
      -(nlx * eye.x + nly * eye.y + nlz * eye.z),
      -(upnx * eye.x + upny * eye.y + upnz * eye.z),
      -(ndx * eye.x + ndy * eye.y + ndz * eye.z),
      1.0f
    )
  }
}
