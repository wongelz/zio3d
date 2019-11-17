package zio3d.engine

import zio3d.core.math.Matrix4

final case class Transformation(
  projectionMatrix: Matrix4,
  viewMatrix: Matrix4
) {

  def noTranslation: Transformation =
    copy(viewMatrix = viewMatrix.copy(m30 = 0, m31 = 0, m32 = 0))

  def getModelMatrix(i: ItemInstance): Matrix4 =
    Matrix4.identity
      .translate(i.position)
      .rotate(i.rotation)
      .scale(i.scale)

  def getModelViewMatrix(i: ItemInstance): Matrix4 =
    viewMatrix * getModelMatrix(i)
}
