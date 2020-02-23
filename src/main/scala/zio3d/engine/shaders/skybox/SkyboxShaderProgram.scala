package zio3d.engine.shaders.skybox

import zio3d.core.gl.{AttribLocation, Program, UniformLocation}

final case class SkyboxShaderProgram(
  program: Program,
  uniModelViewMatrix: UniformLocation,
  uniProjectionMatrix: UniformLocation,
  uniSkybox: UniformLocation,
  uniAmbientLight: UniformLocation,
  positionAttr: AttribLocation,
  texCoordAttr: AttribLocation
)
