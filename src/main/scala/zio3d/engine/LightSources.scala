package zio3d.engine

import zio3d.core.math.{Vector3, Vector4}

final case class Attenuation(
  constant: Float,
  linear: Float,
  exponent: Float
)

final case class PointLight(
  color: Vector3,
  position: Vector3,
  intensity: Float,
  attenuation: Attenuation
) {
  def toViewCoordinates(transformation: Transformation): PointLight = {
    val viewPos = Vector4(position.x, position.y, position.z, 1) mul transformation.viewMatrix
    copy(position = Vector3(viewPos.x, viewPos.y, viewPos.z))
  }

  def withPosition(pos: Vector3): PointLight =
    copy(position = pos)
}

final case class DirectionalLight(
  color: Vector3,
  direction: Vector3,
  intensity: Float
)

final case class SpotLight(
  pointLight: PointLight,
  coneDirection: Vector3,
  cutOff: Float
) {
  def toViewCoordinates(transformation: Transformation): SpotLight = {
    val dir = Vector4(coneDirection, 0) mul transformation.viewMatrix
    copy(coneDirection = Vector3(dir.x, dir.y, dir.z), pointLight = pointLight.toViewCoordinates(transformation))
  }

  def withPosition(pos: Vector3) =
    copy(pointLight = pointLight.withPosition(pos))

  def withDirection(dir: Vector3) =
    copy(coneDirection = dir)
}

final case class LightSources(
  ambient: Vector3,
  directionalLight: Option[DirectionalLight],
  point: List[PointLight],
  spot: List[SpotLight]
)
