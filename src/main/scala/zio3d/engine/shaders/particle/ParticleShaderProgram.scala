package zio3d.engine.shaders.particle

import zio3d.core.gl.{AttribLocation, Program, UniformLocation}

final case class ParticleShaderProgram(
  program: Program,
  uniModelViewMatrix: UniformLocation,
  uniProjectionMatrix: UniformLocation,
  uniTextureSampler: UniformLocation,
  uniTexXOffset: UniformLocation,
  uniTexYOffset: UniformLocation,
  uniNumCols: UniformLocation,
  uniNumRows: UniformLocation,
  positionAttr: AttribLocation,
  texCoordAttr: AttribLocation,
  normalsAttr: AttribLocation
)
