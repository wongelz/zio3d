package zio3d.engine.shaders.simple

import zio3d.core.gl.{AttribLocation, Program, UniformLocation}

final case class SimpleShaderProgram(
  program: Program,
  uniModelViewMatrix: UniformLocation,
  uniProjectionMatrix: UniformLocation,
  uniTextureSampler: UniformLocation,
  positionAttr: AttribLocation,
  texCoordAttr: AttribLocation,
  normalsAttr: AttribLocation
)
