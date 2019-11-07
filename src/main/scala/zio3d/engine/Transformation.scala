package zio3d.engine

import zio3d.core.math.Matrix4

final case class Transformation(
  projectionMatrix: Matrix4,
  viewMatrix: Matrix4
) {

  def noTranslation: Transformation =
    copy(viewMatrix = viewMatrix.copy(m30 = 0, m31 = 0, m32 = 0))

  def getModelMatrix(gameItem: GameItem): Matrix4 =
    Matrix4.identity
      .translate(gameItem.position)
      .rotate(gameItem.rotation)
      .scale(gameItem.scale)

  def getModelViewMatrix(gameItem: GameItem): Matrix4 =
    viewMatrix * getModelMatrix(gameItem)
}
