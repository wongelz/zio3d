package zio3d.engine

import java.nio.file.Path

import zio3d.core.math.Vector4

final case class TextureDefinition(
  path: Path,
  flipVertical: Boolean,
  cols: Int,
  rows: Int
)

object TextureDefinition {
  def apply(path: Path): TextureDefinition =
    TextureDefinition(path, flipVertical = false)

  def apply(path: Path, flipVertical: Boolean): TextureDefinition =
    TextureDefinition(path, flipVertical, 1, 1)
}

final case class MaterialDefinition(
  ambientColour: Vector4,
  diffuseColour: Vector4,
  specularColour: Vector4,
  texture: Option[TextureDefinition],
  normalMap: Option[Path]
)

object MaterialDefinition {
  def DefaultColour = Vector4(1.0f, 1.0f, 1.0f, 1.0f)

  def empty =
    new MaterialDefinition(DefaultColour, DefaultColour, DefaultColour, None, None)

  def textured(texture: Path, textureFlipVertical: Boolean) =
    new MaterialDefinition(
      DefaultColour,
      DefaultColour,
      DefaultColour,
      Some(TextureDefinition(texture, textureFlipVertical)),
      None
    )

  def textured(texture: Path, normalMap: Option[Path], textureFlipVertical: Boolean) =
    new MaterialDefinition(
      DefaultColour,
      DefaultColour,
      DefaultColour,
      Some(TextureDefinition(texture, textureFlipVertical)),
      normalMap
    )

  def animatedTextured(texture: Path, textureFlipVertical: Boolean, cols: Int, rows: Int) =
    new MaterialDefinition(
      DefaultColour,
      DefaultColour,
      DefaultColour,
      Some(TextureDefinition(texture, textureFlipVertical, cols, rows)),
      None
    )
}

final case class MeshDefinition(
  positions: Array[Float],
  texCoords: Array[Float],
  normals: Array[Float],
  indices: Array[Int],
  weights: Array[Float],
  jointIndices: Array[Int],
  material: MaterialDefinition
)

object MeshDefinition {
  val MaxWeights = 4

  def apply(positions: Array[Float]): MeshDefinition =
    MeshDefinition(positions, Array.empty, Array.empty, Array.empty, Array.empty, Array.empty, MaterialDefinition.empty)

  def apply(
    positions: Array[Float],
    texCoords: Array[Float],
    normals: Array[Float],
    indices: Array[Int],
    material: MaterialDefinition
  ): MeshDefinition =
    MeshDefinition(
      positions,
      texCoords,
      normals,
      indices,
      Array.fill(MaxWeights * positions.length / 3)(0.0f),
      Array.fill(MaxWeights * positions.length / 3)(0),
      material
    )
}

final case class SimpleMeshDefinition(
  positions: Array[Float],
  texCoords: Array[Float],
  normals: Array[Float],
  indices: Array[Int],
  material: MaterialDefinition
)

object SimpleMeshDefinition {
  def fromMeshDefinition(m: MeshDefinition) =
    SimpleMeshDefinition(
      m.positions,
      m.texCoords,
      m.normals,
      m.indices,
      m.material
    )

  object Rectangle {
    val positions =
      Array(
        -0.5f, 0.5f, 0.0f, -0.5f, -0.5f, 0.0f, 0.5f, -0.5f, 0.0f, 0.5f, 0.5f, 0.0f
      )

    val texCoords =
      Array(
        0f, 1f, 0f, 0f, 1f, 0f, 1f, 1f
      )

    val normals =
      Array(
        0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 1f
      )

    val indices =
      Array(
        0, 1, 3, 3, 1, 2
      )
  }

  def image2D(textureFile: Path, flipVertical: Boolean): SimpleMeshDefinition =
    SimpleMeshDefinition(
      Rectangle.positions,
      Rectangle.texCoords,
      Rectangle.normals,
      Rectangle.indices,
      MaterialDefinition.textured(textureFile, flipVertical)
    )

  def animatedImage2D(textureFile: Path, flipVertical: Boolean, cols: Int, rows: Int): SimpleMeshDefinition =
    SimpleMeshDefinition(
      Rectangle.positions,
      Rectangle.texCoords,
      Rectangle.normals,
      Rectangle.indices,
      MaterialDefinition.animatedTextured(textureFile, flipVertical, cols, rows)
    )

}
