package zio3d.engine

import zio3d.core.gl.GL.{Texture, VertexArrayObject, VertexBufferObject}
import zio3d.core.math.Vector4

final case class Material(
  ambientColour: Vector4,
  diffuseColour: Vector4,
  specularColour: Vector4,
  reflectance: Float,
  texture: Option[Texture],
  normalMap: Option[Texture]
)

object Material {
  def DefaultColour = Vector4(1.0f, 1.0f, 1.0f, 1.0f)

  def empty =
    new Material(DefaultColour, DefaultColour, DefaultColour, 0, None, None)

  def textured(texture: Texture) =
    new Material(DefaultColour, DefaultColour, DefaultColour, 0, Some(texture), None)

  def textured(texture: Texture, normalMap: Texture) =
    new Material(DefaultColour, DefaultColour, DefaultColour, 0, Some(texture), Some(normalMap))
}

final case class Mesh(
  vao: VertexArrayObject,
  vbos: List[VertexBufferObject],
  vertexCount: Int,
  material: Material
)
