package zio3d.engine

import zio3d.core.math._

final case class Camera(
  position: Vector3,
  yaw: Float,  // degrees
  pitch: Float // degrees
) {

  lazy val front: Vector3 =
    Vector3(
      cos(toRadians(yaw)) * cos(toRadians(pitch)),
      sin(toRadians(pitch)),
      sin(toRadians(yaw)) * cos(toRadians(pitch))
    ).normalize

  lazy val right: Vector3 =
    front.cross(Vector3(0, 1, 0)).normalize

  lazy val up: Vector3 =
    right.cross(front).normalize

  lazy val viewMatrix: Matrix4 =
    Matrix4.lookAt(position, position + front, Vector3(0, 1, 0))

  def withHeight(y: Float) =
    copy(position = Vector3(position.x, y, position.z))

  def rotate(yawDelta: Float, pitchDelta: Float) =
    copy(pitch = pitch + pitchDelta, yaw = yaw + yawDelta)

  def movePosition(xDelta: Float, yDelta: Float, zDelta: Float) =
    copy(
      position = position +
        (front * zDelta) +
        (right * xDelta) +
        (up * yDelta)
    )
}

object Camera {

  def apply(position: Vector3): Camera =
    Camera(position, 0, 0)
}
