package zio3d.engine

import zio3d.core.math.{Vector2, Vector3}

import scala.annotation.tailrec

final case class Terrain(
  blocks: List[GameItem],
  boundingBoxes: Array[Array[Box2D]],
  heightMapMesh: HeightMapMesh,
  terrainSize: Int,
  verticesPerCol: Int,
  verticesPerRow: Int
) {

  def getPosition(x: Float, z: Float): Option[Vector3] =
    getPosition(Vector3(x, 0, z))

  def getPosition(position: Vector2): Option[Vector3] =
    getPosition(Vector3(position.x, 0, position.y))

  def getPosition(position: Vector3): Option[Vector3] =
    getHeight(position) map { h =>
      Vector3(position.x, h, position.z)
    }

  /**
   * Return the height of the terrain at the given world position, or None if the position is not on the terrain.
   * @param position position.
   * @return height.
   */
  def getHeight(position: Vector3): Option[Float] = {
    @tailrec
    def loop(row: Int, col: Int): Option[Float] = {
      val boundingBox = boundingBoxes(row)(col)
      if (boundingBox.contains(position.x, position.z)) {
        val terrainBlock = blocks(row * terrainSize + col)
        val triangle     = getTriangle(position, boundingBox, terrainBlock)
        Some(interpolateHeight(triangle(0), triangle(1), triangle(2), position.x, position.z))
      } else {
        if (col == 0) {
          if (row == 0) {
            None
          } else {
            loop(row - 1, terrainSize - 1)
          }
        } else {
          loop(row, col - 1)
        }
      }
    }

    loop(terrainSize - 1, terrainSize - 1)
  }

  private def getTriangle(position: Vector3, boundingBox: Box2D, terrainBlock: GameItem): Array[Vector3] = {
    val cellWidth  = boundingBox.width / verticesPerCol.toFloat
    val cellHeight = boundingBox.height / verticesPerRow.toFloat
    val col        = ((position.x - boundingBox.x) / cellWidth).toInt
    val row        = ((position.z - boundingBox.y) / cellHeight).toInt

    val t1 = Vector3(
      boundingBox.x + col * cellWidth,
      getWorldHeight(row + 1, col, terrainBlock),
      boundingBox.y + (row + 1) * cellHeight
    )
    val t2 = Vector3(
      boundingBox.x + (col + 1) * cellWidth,
      getWorldHeight(row, col + 1, terrainBlock),
      boundingBox.y + row * cellHeight
    )
    val t0 = if (position.z < getDiagonalZCoord(t1.x, t1.z, t2.x, t2.z, position.x)) {
      Vector3(boundingBox.x + col * cellWidth, getWorldHeight(row, col, terrainBlock), boundingBox.y + row * cellHeight)
    } else {
      Vector3(
        boundingBox.x + (col + 1) * cellWidth,
        getWorldHeight(row + 2, col + 1, terrainBlock),
        boundingBox.y + (row + 1) * cellHeight
      )
    }
    Array(t0, t1, t2)
  }

  private def getDiagonalZCoord(x1: Float, z1: Float, x2: Float, z2: Float, x: Float): Float =
    ((z1 - z2) / (x1 - x2)) * (x - x1) + z1

  private def getWorldHeight(row: Int, col: Int, gameItem: GameItem) = {
    val y = heightMapMesh.getHeight(row, col)
    y * gameItem.instances.head.scale + gameItem.instances.head.position.y
  }

  private def interpolateHeight(pA: Vector3, pB: Vector3, pC: Vector3, x: Float, z: Float) = {
    // Plane equation ax+by+cz+d=0
    val a = (pB.y - pA.y) * (pC.z - pA.z) - (pC.y - pA.y) * (pB.z - pA.z)
    val b = (pB.z - pA.z) * (pC.x - pA.x) - (pC.z - pA.z) * (pB.x - pA.x)
    val c = (pB.x - pA.x) * (pC.y - pA.y) - (pC.x - pA.x) * (pB.y - pA.y)
    val d = -(a * pA.x + b * pA.y + c * pA.z)
    // y = (-d -ax -cz) / b
    (-d - a * x - c * z) / b
  }
}
