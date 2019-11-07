package zio3d.core

import java.nio.file.Path

import zio3d.engine.loaders.LoadingError.FileLoadError
import zio.ZIO

package object images {

  final val images: ZIO[Images, Nothing, Images.Service] =
    ZIO.access(_.images)

  final def loadImage(source: Path, flipVertical: Boolean, desiredChannels: Int): ZIO[Images, FileLoadError, Image] =
    ZIO.accessM(_.images.loadImage(source, flipVertical, desiredChannels))
}
