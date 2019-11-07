package zio3d.core.images

import java.nio.ByteBuffer
import java.nio.file.Path

import org.lwjgl.opengl.GL11.{GL_RED, GL_RGB, GL_RGBA}
import org.lwjgl.stb.STBImage
import org.lwjgl.system.MemoryStack
import zio.IO
import zio3d.engine.loaders.LoadingError.FileLoadError

final case class Image(
  image: ByteBuffer,
  width: Int,
  height: Int,
  components: Int
) {

  def format = components match {
    case 1 => GL_RED
    case 3 => GL_RGB
    case 4 => GL_RGBA
  }
}

trait Images {
  def images: Images.Service
}

object Images {
  trait Service {
    def loadImage(source: Path, flipVertical: Boolean, desiredChannels: Int): IO[FileLoadError, Image]
  }

  trait Live extends Images {
    val images: Images.Service = new Service {
      def loadImage(source: Path, flipVertical: Boolean, desiredChannels: Int): IO[FileLoadError, Image] =
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
