package zio3d.engine.loaders

import zio.ZIO
import zio3d.core.images.Image
import zio3d.engine.shaders.scene.SceneShaderProgram
import zio3d.engine.{HeightMapMesh, MaterialDefinition}

package object heightmap {

  final def loadHeightMap(
    program: SceneShaderProgram,
    minY: Float,
    maxY: Float,
    heightMapImage: Image,
    materialDefinition: MaterialDefinition,
    textInc: Int
  ): ZIO[HeightMapLoader, LoadingError, HeightMapMesh] =
    ZIO.accessM(_.heightMapLoader.load(program, minY, maxY, heightMapImage, materialDefinition, textInc))
}
