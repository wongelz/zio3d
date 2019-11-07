package zio3d.engine

import java.nio.file.Path

final case class SkyboxDefinition(
  vertices: Array[Float],
  textureFront: Path,
  textureBack: Path,
  textureTop: Path,
  textureBottom: Path,
  textureLeft: Path,
  textureRight: Path,
  scale: Float
) {
  def textures = List(textureFront, textureBack, textureTop, textureBottom, textureRight, textureLeft)
}

object SkyboxDefinition {
  val vertices = Array(
    // positions
    -1.0f, 1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f,
    -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, -1.0f,
    -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, -1.0f, 1.0f,
    -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, -1.0f, 1.0f, -1.0f,
    -1.0f, 1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f,
    1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f,
    1.0f, -1.0f, 1.0f
  )

  def apply(
    textureFront: Path,
    textureBack: Path,
    textureTop: Path,
    textureBottom: Path,
    textureLeft: Path,
    textureRight: Path,
    scale: Float
  ): SkyboxDefinition =
    new SkyboxDefinition(
      vertices,
      textureFront,
      textureBack,
      textureTop,
      textureBottom,
      textureLeft,
      textureRight,
      scale
    )
}
