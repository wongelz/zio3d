package zio3d.engine.loaders.terrain

import java.nio.file.Path

import zio.{IO, ZIO}
import zio3d.core.images.{Image, Images}
import zio3d.engine._
import zio3d.engine.loaders.LoadingError
import zio3d.engine.loaders.heightmap._
import zio3d.engine.shaders.scene.SceneShaderProgram

trait TerrainLoader extends HeightMapLoader with Images {
  def terrainLoader: TerrainLoader.Service
}

object TerrainLoader {

  trait Service {
    def load(
      program: SceneShaderProgram,
      terrainSize: Int,
      scale: Float,
      minY: Float,
      maxY: Float,
      heightMap: Path,
      textureFile: Path,
      textInc: Int
    ): IO[LoadingError, Terrain]
  }

  trait Live extends HeightMapLoader.Live with Images.Live with TerrainLoader {
    val terrainLoader: Service = new Service {
      override def load(
        program: SceneShaderProgram,
        terrainSize: Int,
        scale: Float,
        minY: Float,
        maxY: Float,
        heightMap: Path,
        textureFile: Path,
        textInc: Int
      ): ZIO[Any, LoadingError, Terrain] =
        for {
          i <- images.loadImage(heightMap, false, 4)
          h <- heightMapLoader.load(program, minY, maxY, i, MaterialDefinition.textured(textureFile, false), textInc)
        } yield generateTerrain(h, i, terrainSize, scale)
    }

    private def generateTerrain(heightMapMesh: HeightMapMesh, i: Image, terrainSize: Int, scale: Float): Terrain = {
      val blocks        = new Array[GameItem](terrainSize * terrainSize)
      val boundingBoxes = Array.ofDim[Box2D](terrainSize, terrainSize)
      for (row <- 0 until terrainSize; col <- 0 until terrainSize) {
        val xDisplacement = (col - (terrainSize - 1) / 2) * scale * HeightMapMesh.xLength
        val zDisplacement = (row - (terrainSize - 1) / 2) * scale * HeightMapMesh.zLength
        val block = GameItem(heightMapMesh.mesh)
          .withScale(scale)
          .withPosition(xDisplacement, 0, zDisplacement)
        blocks(row * terrainSize + col) = block
        boundingBoxes(row)(col) = getBoundingBox(block)
      }
      Terrain(blocks.toList, boundingBoxes, heightMapMesh, terrainSize, i.width - 1, i.height - 1)
    }

    private def getBoundingBox(terrainBlock: GameItem): Box2D = {
      val scale    = terrainBlock.scale
      val position = terrainBlock.position
      val topLeftX = HeightMapMesh.StartX * scale + position.x
      val topLeftZ = HeightMapMesh.StartZ * scale + position.z
      val width    = Math.abs(HeightMapMesh.StartX * 2) * scale
      val height   = Math.abs(HeightMapMesh.StartZ * 2) * scale
      Box2D(topLeftX, topLeftZ, width, height)
    }
  }

  object Live extends Live

}
