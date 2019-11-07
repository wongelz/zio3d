package zio3d.engine

import zio3d.core.math.Vector3

final case class Fog(
  active: Boolean,
  color: Vector3,
  density: Float
)
