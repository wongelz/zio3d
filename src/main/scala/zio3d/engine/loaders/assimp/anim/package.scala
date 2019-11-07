package zio3d.engine.loaders.assimp

import java.nio.file.Path

import zio3d.engine.loaders.LoadingError
import zio3d.engine.loaders.assimp.anim.AnimMeshLoader.AnimMesh
import zio.ZIO
import zio3d.engine.loaders.LoadingError

package object anim {

  final def loadAnimMesh(resourcePath: Path): ZIO[AnimMeshLoader, LoadingError, AnimMesh] =
    ZIO.accessM(_.animMeshLoader.load(resourcePath))

}
