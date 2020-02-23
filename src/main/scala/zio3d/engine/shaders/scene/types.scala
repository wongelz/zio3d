package zio3d.engine.shaders.scene

import zio3d.core.gl.{AttribLocation, Program, UniformLocation}

final case class SceneShaderProgram(
  program: Program,
  uniModelViewMatrix: UniformLocation,
  uniProjectionMatrix: UniformLocation,
  uniJointsMatrix: UniformLocation,
  uniTextureSampler: UniformLocation,
  uniMaterial: MaterialUniform,
  uniSpecularPower: UniformLocation,
  uniAmbientLight: UniformLocation,
  uniPointLight: List[PointLightUniform],
  uniSpotLight: List[SpotLightUniform],
  uniFog: FogUniform,
  positionAttr: AttribLocation,
  texCoordAttr: AttribLocation,
  normalsAttr: AttribLocation,
  jointWeightsAttr: AttribLocation,
  jointIndicesAttr: AttribLocation
)

final case class MaterialUniform(
  ambient: UniformLocation,
  diffuse: UniformLocation,
  specular: UniformLocation,
  hasTexture: UniformLocation,
  reflectance: UniformLocation
)

final case class PointLightUniform(
  colour: UniformLocation,
  position: UniformLocation,
  intensity: UniformLocation,
  attConstant: UniformLocation,
  attLinear: UniformLocation,
  attExponent: UniformLocation
)

final case class SpotLightUniform(
  pl: PointLightUniform,
  conedir: UniformLocation,
  cutoff: UniformLocation
)

final case class FogUniform(
  active: UniformLocation,
  colour: UniformLocation,
  density: UniformLocation
)
