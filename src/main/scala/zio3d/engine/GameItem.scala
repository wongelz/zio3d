package zio3d.engine

import zio3d.core.math.{AxisAngle4, Matrix4, Quaternion, Vector3}

final case class GameItem(
  meshes: List[Mesh],
  position: Vector3,
  scale: Float,
  rotation: Quaternion,
  boxSize: Float, // axis-aligned box for very simple collision detection
  animation: Option[Animation],
  textureAnimation: Option[TextureAnimation]
) {

  def withPosition(x: Float, y: Float, z: Float): GameItem =
    withPosition(Vector3(x, y, z))

  def withPosition(position: Vector3): GameItem =
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
    animation.fold(this)(a => copy(animation = Some(a.animate)))

  def animateTexture(elapsedTime: Long) =
    textureAnimation.fold(this)(a => copy(textureAnimation = Some(a.animate(elapsedTime))))

  def aabbContains(p: Vector3): Boolean =
    p.x >= (position.x - boxSize) && p.x <= (position.x + boxSize) &&
      p.y >= (position.y - boxSize) && p.y <= (position.y + boxSize) &&
      p.z >= (position.z - boxSize) && p.z <= (position.z + boxSize)
}

object GameItem {
  def apply(mesh: Mesh): GameItem =
    GameItem(List(mesh), Vector3(0, 0, -2), 0.5f, Quaternion.Zero, 0, None, None)

  def apply(mesh: Mesh, textureAnimation: TextureAnimation): GameItem =
    GameItem(List(mesh), Vector3(0, 0, -2), 0.5f, Quaternion.Zero, 0, None, Some(textureAnimation))

  def apply(meshes: List[Mesh], animation: Animation): GameItem =
    GameItem(meshes, Vector3(0, 0, -2), 0.5f, Quaternion.Zero, 0, Some(animation), None)
}

final case class AnimatedFrame(
  jointMatrices: Array[Matrix4]
)

final case class Animation(
  name: String,
  frames: Array[AnimatedFrame],
  duration: Double = 0,
  currentFrame: Int = 0
) {

  def animate =
    copy(currentFrame = (currentFrame + 1) % frames.length)

  def getCurrentFrame =
    frames(currentFrame)
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
