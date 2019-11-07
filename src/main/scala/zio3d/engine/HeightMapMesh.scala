package zio3d.engine

import zio3d.engine.HeightMapMesh.HeightArray

final case class HeightMapMesh(
  minY: Float,
  maxY: Float,
  heightArray: HeightArray,
  mesh: Mesh
) {

  def getHeight(row: Int, col: Int): Float =
    if (row >= 0 && row < heightArray.length &&
        col >= 0 && col < heightArray(row).length) {
      heightArray(row)(col)
    } else {
      0
    }
}

object HeightMapMesh {
  type HeightArray = Array[Array[Float]]
  val MaxColour = 255f * 255f * 255f
  val StartX    = -0.5f
  val StartZ    = -0.5f

  def xLength = Math.abs(-StartX * 2)

  def zLength = Math.abs(-StartZ * 2)
}
