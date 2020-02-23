package zio3d.core

import java.nio.file.Path

import org.lwjgl.stb.STBImage
import org.lwjgl.system.MemoryStack
import zio3d.engine.loaders.LoadingError.FileLoadError
import zio.{Has, IO, ZIO, ZLayer}

package object images {

  type Images = Has[Images.Service]

  object Images extends Serializable {

    trait Service {
      def loadImage(source: Path, flipVertical: Boolean, desiredChannels: Int): IO[FileLoadError, Image]
    }

    val live = ZLayer.succeed {
      new Service {
        override def loadImage(source: Path, flipVertical: Boolean, desiredChannels: Int): IO[FileLoadError, Image] =
          IO.effectTotal(MemoryStack.stackPush()).bracket(s => IO.effectTotal(s.close())) { stack =>
            IO.effectTotal {
              val w    = stack.mallocInt(1)
              val h    = stack.mallocInt(1)
              val comp = stack.mallocInt(1)

              STBImage.stbi_set_flip_vertically_on_load(flipVertical)
              Option(STBImage.stbi_load(source.toString, w, h, comp, desiredChannels)) map { i =>
                Image(i, w.get(), h.get(), comp.get())
              }
            }.flatMap {
              case Some(i) => IO.succeed(i)
              case None    => IO.fail(FileLoadError(source.toString, STBImage.stbi_failure_reason()))
            }
          }
      }
    }
  }

  final val images: ZIO[Images, Nothing, Images.Service] =
    ZIO.access(_.get)

  final def loadImage(source: Path, flipVertical: Boolean, desiredChannels: Int): ZIO[Images, FileLoadError, Image] =
    ZIO.accessM(_.get.loadImage(source, flipVertical, desiredChannels))
}
