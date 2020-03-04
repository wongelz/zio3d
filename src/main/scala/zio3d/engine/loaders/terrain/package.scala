package zio3d.engine.loaders

import java.nio.file.Path

import zio.{Has, IO, ZIO, ZLayer}
import zio3d.core.images.{Image, Images}
import zio3d.core.math.Vector3
import zio3d.engine._
import zio3d.engine.loaders.heightmap.HeightMapLoader
import zio3d.engine.shaders.scene.SceneShaderProgram

package object terrain {

  type TerrainLoader = Has[TerrainLoader.Service]

  object TerrainLoader extends Serializable {
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

    val live = ZLayer.fromServices[Images.Service, HeightMapLoader.Service, TerrainLoader.Service] { (images, heightMapLoader) =>
      new Service {

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

        private def generateTerrain(heightMapMesh: HeightMapMesh, i: Image, terrainSize: Int, scale: Float): Terrain = {
          val blocks        = new Array[GameItem](terrainSize * terrainSize)
          val boundingBoxes = Array.ofDim[Box2D](terrainSize, terrainSize)
          for (row <- 0 until terrainSize; col <- 0 until terrainSize) {
            val xDisplacement = (col - (terrainSize - 1) / 2) * scale * HeightMapMesh.xLength
            val zDisplacement = (row - (terrainSize - 1) / 2) * scale * HeightMapMesh.zLength
            val inst          = ItemInstance(Vector3(xDisplacement, 0, zDisplacement), scale)
            val block = GameItem(Model.still(heightMapMesh.mesh))
              .spawn(inst)
            blocks(row * terrainSize + col) = block
            boundingBoxes(row)(col) = getBoundingBox(inst)
          }
          Terrain(blocks.toList, boundingBoxes, heightMapMesh, terrainSize, i.width - 1, i.height - 1)
        }

        private def getBoundingBox(i: ItemInstance): Box2D = {
          val scale    = i.scale
          val position = i.position
          val topLeftX = HeightMapMesh.StartX * scale + position.x
          val topLeftZ = HeightMapMesh.StartZ * scale + position.z
          val width    = Math.abs(HeightMapMesh.StartX * 2) * scale
          val height   = Math.abs(HeightMapMesh.StartZ * 2) * scale
          Box2D(topLeftX, topLeftZ, width, height)
        }
      }
    }
  }

  def loadTerrain(
    program: SceneShaderProgram,
    terrainSize: Int,
    scale: Float,
    minY: Float,
    maxY: Float,
    heightMap: Path,
    textureFile: Path,
    textInc: Int
  ): ZIO[TerrainLoader, LoadingError, Terrain] =
    ZIO.accessM(_.get.load(program, terrainSize, scale, minY, maxY, heightMap, textureFile, textInc))
}
