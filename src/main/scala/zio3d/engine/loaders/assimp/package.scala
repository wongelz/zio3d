package zio3d.engine.loaders

import java.nio.file.Path

import zio.ZIO
import zio3d.engine.MeshDefinition

package object assimp {

  final def loadStaticMesh(resourcePath: Path): ZIO[StaticMeshLoader, LoadingError, List[MeshDefinition]] =
    ZIO.accessM(_.staticMeshLoader.load(resourcePath))

  final def loadStaticMesh(resourcePath: Path, flags: Int): ZIO[StaticMeshLoader, LoadingError, List[MeshDefinition]] =
    ZIO.accessM(_.staticMeshLoader.load(resourcePath, flags))
}
