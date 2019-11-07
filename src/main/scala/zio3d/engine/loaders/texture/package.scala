package zio3d.engine.loaders

import java.nio.file.Path

import zio.ZIO
import zio3d.core.gl.GL.Texture
import zio3d.engine._
import zio3d.engine.loaders.LoadingError.FileLoadError

package object texture {

  final val textureLoader: ZIO[TextureLoader, Nothing, TextureLoader.Service] =
    ZIO.access(_.textureLoader)

  final def loadTexture(source: Path, flipVertical: Boolean): ZIO[TextureLoader, FileLoadError, Texture] =
    ZIO.accessM(_.textureLoader.loadTexture(source, flipVertical))

  final def loadMaterial(materialDefinition: MaterialDefinition): ZIO[TextureLoader, FileLoadError, Material] =
    ZIO.accessM(_.textureLoader.loadMaterial(materialDefinition))
}
