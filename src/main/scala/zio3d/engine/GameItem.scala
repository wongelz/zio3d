package zio3d.engine

import zio3d.core.math.{AxisAngle4, Matrix4, Quaternion, Vector3}

final case class Model(
  meshes: List[Mesh],
  animation: Option[Animation]
)

object Model {
  def still(mesh: Mesh): Model =
    Model(List(mesh), None)

  def still(meshes: List[Mesh]): Model =
    Model(meshes, None)

  def animated(meshes: List[Mesh], animation: Animation): Model =
    Model(meshes, Some(animation))
}

final case class GameItem(
  model: Model,
  instances: List[ItemInstance]
) {

  def spawn(i: ItemInstance) =
    copy(instances = i :: instances)

  def animate =
    copy(instances = instances.map(_.animate))
}

object GameItem {
  def apply(model: Model): GameItem =
    GameItem(model, Nil)
}

final case class ItemInstance(
  position: Vector3,
  scale: Float,
  rotation: Quaternion,
  boxSize: Float, // axis-aligned box for very simple collision detection
  modelAnimation: Option[ModelAnimation],
  textureAnimation: Option[TextureAnimation]
) {

  def withPosition(x: Float, y: Float, z: Float): ItemInstance =
    withPosition(Vector3(x, y, z))

  def withPosition(position: Vector3): ItemInstance =
    copy(position = position)

  def withRotation(rotation: Quaternion) =
    copy(rotation = rotation)

  def withRotation(rotation: AxisAngle4) =
    copy(rotation = Quaternion(rotation))

  def withScale(scale: Float) =
    copy(scale = scale)

  def withBoxSize(boxSize: Float) =
    copy(boxSize = boxSize)

  def animate =
    copy(modelAnimation = modelAnimation.map(_.animate))

  def animateTexture(elapsedTime: Long) =
    textureAnimation.fold(this)(a => copy(textureAnimation = Some(a.animate(elapsedTime))))

  def aabbContains(p: Vector3): Boolean =
    p.x >= (position.x - boxSize) && p.x <= (position.x + boxSize) &&
      p.y >= (position.y - boxSize) && p.y <= (position.y + boxSize) &&
      p.z >= (position.z - boxSize) && p.z <= (position.z + boxSize)
}

object ItemInstance {

  def apply(position: Vector3, scale: Float): ItemInstance =
    ItemInstance(position, scale, Quaternion.Zero, 0, None, None)

  def apply(position: Vector3, scale: Float, textureAnimation: TextureAnimation): ItemInstance =
    ItemInstance(position, scale, Quaternion.Zero, 0, None, Some(textureAnimation))

  def apply(position: Vector3, scale: Float, rotation: Quaternion): ItemInstance =
    ItemInstance(position, scale, rotation, 0, None, None)
}

final case class AnimatedFrame(
  jointMatrices: Array[Matrix4]
)

final case class Animation(
  name: String,
  frames: Array[AnimatedFrame],
  duration: Double = 0
)

final case class ModelAnimation(
  frames: Int,
  currentFrame: Int
) {

  def animate: ModelAnimation =
    copy(currentFrame = (currentFrame + 1) % frames)
}

final case class TextureAnimation(
  cols: Int,
  rows: Int,
  textureUpdateMillis: Long,
  currentFrame: Int,
  lastTextureUpdateMillis: Long
) {

  private val frames = cols * rows

  def animate(elapsedTime: Long): TextureAnimation =
    if (lastTextureUpdateMillis + elapsedTime > textureUpdateMillis) {
      copy(currentFrame = (currentFrame + 1) % frames, lastTextureUpdateMillis = 0)
    } else {
      copy(lastTextureUpdateMillis = lastTextureUpdateMillis + elapsedTime)
    }
}

object TextureAnimation {
  def apply(cols: Int, rows: Int, updateMillis: Long): TextureAnimation =
    TextureAnimation(cols, rows, updateMillis, 0, 0)
}
