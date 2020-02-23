package zio3d.engine.loaders

import java.nio.ByteBuffer

import zio.{Has, IO, ZIO, ZLayer}
import zio3d.core.images.Image
import zio3d.core.math.Vector3
import zio3d.engine.HeightMapMesh.{StartX, StartZ, xLength, zLength}
import zio3d.engine.shaders.scene.{SceneShaderInterpreter, SceneShaderProgram}
import zio3d.engine.{HeightMapMesh, MaterialDefinition, MeshDefinition, ShaderEnv}

import scala.collection.mutable.ListBuffer

package object heightmap {

  type HeightMapLoader = Has[HeightMapLoader.Service]

  object HeightMapLoader extends Serializable {
    trait Service {
      def load(
        program: SceneShaderProgram,
        minY: Float,
        maxY: Float,
        heightMapImage: Image,
        materialDefinition: MaterialDefinition,
        textInc: Int
      ): IO[LoadingError, HeightMapMesh]
    }

    val live = ZLayer.fromEnvironment[ShaderEnv, HeightMapLoader] { env =>
      Has(new Service {

        private val sceneShaderInterpreter = env.get[SceneShaderInterpreter.Service]

        override def load(
          program: SceneShaderProgram,
          minY: Float,
          maxY: Float,
          heightMapImage: Image,
          materialDefinition: MaterialDefinition,
          textInc: Int
        ): IO[LoadingError, HeightMapMesh] = {
          val width  = heightMapImage.width
          val height = heightMapImage.height
          val incX   = xLength / (width - 1)
          val incZ   = zLength / (height - 1)

          def getPosition(row: Int, col: Int) = {
            val currentHeight = getHeight(col, row, width, minY, maxY, heightMapImage.image)
            List(StartX + col * incX, currentHeight, StartZ + row * incZ)
          }

          def getTexCoords(row: Int, col: Int) =
            List(textInc.toFloat * col.toFloat / width.toFloat, textInc.toFloat * row.toFloat / height.toFloat)

          def getIndices(row: Int, col: Int) =
            if (col < width - 1 && row < height - 1) {
              val leftTop     = row * width + col
              val leftBottom  = (row + 1) * width + col
              val rightBottom = (row + 1) * width + col + 1
              val rightTop    = row * width + col + 1
              List(rightTop, leftBottom, rightBottom, leftTop, leftBottom, rightTop)
            } else {
              Nil
            }

          def getMeshInput(materialDefinition: MaterialDefinition) = {
            val positions   = ListBuffer[Float]()
            val textCoords  = ListBuffer[Float]()
            val indices     = ListBuffer[Int]()
            val heightArray = Array.ofDim[Float](height, width)
            val normals     = ListBuffer[Float]()
            for (row <- 0 until height; col <- 0 until width) {
              positions ++= getPosition(row, col)
              textCoords ++= getTexCoords(row, col)
              indices ++= getIndices(row, col)
              val currentHeight = getHeight(col, row, width, minY, maxY, heightMapImage.image)
              heightArray(row)(col) = currentHeight

              val n = if (row > 0 && row < height - 1 && col > 0 && col < width - 1) {
                val hL = heightArray(row - 1)(col - 0)
                val hR = heightArray(row + 1)(col + 0)
                val hD = heightArray(row - 0)(col - 1)
                val hU = heightArray(row + 0)(col + 1)

                Vector3(hL - hR, 2.0f, hD - hU).normalize
              } else {
                Vector3(0, 1, 0)
              }
              normals += n.x
              normals += n.y
              normals += n.z
            }
            (
              MeshDefinition(positions.toArray, textCoords.toArray, normals.toArray, indices.toArray, materialDefinition),
              heightArray
            )
          }

          for {
            meshInput <- IO.effectTotal { getMeshInput(materialDefinition) }
            m         <- sceneShaderInterpreter.loadMesh(program, meshInput._1)
          } yield HeightMapMesh(minY, maxY, meshInput._2, m)
        }

        /**
         * Get the height from the colour of the corresponding pixel in the height map image.
         *
         * @param x      x position.
         * @param z      z position.
         * @param width  width of height map.
         * @param minY   min height.
         * @param maxY   max height.
         * @param buffer height map.
         * @return height (y position).
         */
        def getHeight(x: Int, z: Int, width: Int, minY: Float, maxY: Float, buffer: ByteBuffer) = {
          val r = buffer.get(x * 4 + 0 + z * 4 * width)
          val g = buffer.get(x * 4 + 1 + z * 4 * width)
          val b = buffer.get(x * 4 + 2 + z * 4 * width)
          //    val a = buffer.get(x * 4 + 3 + z * 4 * width)
          val argb =
          //      ((0xFF & a) << 24) |
            ((0xFF & r) << 16) |
              ((0xFF & g) << 8) |
              (0xFF & b)

          minY + Math.abs(maxY - minY) * (argb.toFloat / HeightMapMesh.MaxColour)
        }
      })
    }
  }

  final def loadHeightMap(
    program: SceneShaderProgram,
    minY: Float,
    maxY: Float,
    heightMapImage: Image,
    materialDefinition: MaterialDefinition,
    textInc: Int
  ): ZIO[HeightMapLoader, LoadingError, HeightMapMesh] =
    ZIO.accessM(_.get.load(program, minY, maxY, heightMapImage, materialDefinition, textInc))
}
