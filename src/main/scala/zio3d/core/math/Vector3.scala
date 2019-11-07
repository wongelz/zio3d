package zio3d.core.math

import zio3d.core.math

final case class Vector3(
  x: Float,
  y: Float,
  z: Float
) {

  def +(vec: Vector3): Vector3 =
    add(vec)

  def add(vec: Vector3): Vector3 =
    Vector3(x + vec.x, y + vec.y, z + vec.z)

  def -(vec: Vector3): Vector3 =
    sub(vec)

  def sub(vec: Vector3): Vector3 =
    Vector3(x - vec.x, y - vec.y, z - vec.z)

  def *(vec: Vector3): Vector3 =
    mul(vec)

  def mul(vec: Vector3): Vector3 =
    Vector3(x * vec.x, y * vec.y, z * vec.z)

  def *(scalar: Float): Vector3 =
    mul(scalar)

  def mul(scalar: Float): Vector3 =
    Vector3(x * scalar, y * scalar, z * scalar)

  def cross(v: Vector3): Vector3 =
    Vector3(y * v.z - z * v.y, z * v.x - x * v.z, x * v.y - y * v.x)

  def rotate(quaternion: Quaternion): Vector3 =
    quaternion.transform(x, y, z)

  def lengthSquared: Float =
    x * x + y * y + z * z

  def length: Float =
    math.sqrt(lengthSquared)

  def normalize: Vector3 = {
    val invLength = 1.0f / length
    Vector3(x * invLength, y * invLength, z * invLength)
  }

  def dot(vec: Vector3) =
    x * vec.x + y * vec.y + z * vec.z
}

object Vector3 {
  def apply(): Vector3 =
    origin

  lazy val origin = Vector3(0, 0, 0)
}
