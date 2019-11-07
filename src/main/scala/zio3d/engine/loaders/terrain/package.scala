package zio3d.engine.loaders

import java.nio.file.Path

import zio.ZIO
import zio3d.engine.Terrain
import zio3d.engine.shaders.scene.SceneShaderProgram

package object terrain {
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
    ZIO.accessM(_.terrainLoader.load(program, terrainSize, scale, minY, maxY, heightMap, textureFile, textInc))
}
