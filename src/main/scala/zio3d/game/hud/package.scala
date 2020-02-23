package zio3d.game

import java.nio.ByteBuffer
import java.nio.channels.ByteChannel
import java.nio.file.{Files, Path}

import org.lwjgl.nanovg.NVGColor
import org.lwjgl.nanovg.NanoVG.{NVG_ALIGN_CENTER, NVG_ALIGN_TOP}
import org.lwjgl.nanovg.NanoVGGL3.NVG_STENCIL_STROKES
import zio._
import zio3d.core.CoreEnv
import zio3d.core.buffers.Buffers
import zio3d.core.glfw.WindowSize
import zio3d.core.nvg.NVG
import zio3d.engine.loaders.LoadingError

package object hud {

  val FontName = "BOLD"

  type HudRenderer = Has[HudRenderer.Service]

  object HudRenderer extends Serializable {
    trait Service {
      def init(font: Path): IO[LoadingError, HudContext]

      def render(context: HudContext, windowSize: WindowSize, state: HudState): UIO[Unit]
    }

    val live = ZLayer.fromEnvironment[CoreEnv, HudRenderer] { env =>
      Has(new Service {

        private val nvg     = env.get[NVG.Service]
        private val buffers = env.get[Buffers.Service]

        def init(font: Path): IO[LoadingError, HudContext] =
          for {
            n  <- nvg.create(NVG_STENCIL_STROKES)
            f  <- toByteBuffer(font).mapError(th => LoadingError.FileLoadError(font.toString, th.getMessage))
            ok <- nvg.createFontMem(n, FontName, f, 0)
            _  <- IO.fail(LoadingError.FileLoadError(font.toString, "Unable to load font")).when(ok != 0)
          } yield HudContext(n, NVGColor.create(), f)

        def render(hud: HudContext, windowSize: WindowSize, state: HudState): UIO[Unit] =
          for {
            _ <- nvg.beginFrame(hud.vg, windowSize.width, windowSize.height, 1)

            // circle
            _ <- nvg.beginPath(hud.vg)
            _ <- nvg.circle(hud.vg, windowSize.width.toFloat / 2, windowSize.height.toFloat / 2, 20.0f)
            _ <- nvg.fillColor(hud.vg, rgba(0xff, 0xff, 0xff, 50, hud.color))
            _ <- nvg.fill(hud.vg)

            _ <- nvg.fontSize(hud.vg, 25.0f)
            _ <- nvg.fontFace(hud.vg, FontName)
            _ <- nvg.textAlign(hud.vg, NVG_ALIGN_CENTER | NVG_ALIGN_TOP)
            _ <- nvg.fillColor(hud.vg, rgba(0xff, 0xff, 0xff, 255, hud.color))
            _ <- nvg.text(hud.vg, windowSize.width.toFloat - 150, windowSize.height.toFloat - 75, s"${state.fps} fps")

            _ <- nvg.endFrame(hud.vg)
          } yield ()

        private def rgba(r: Int, g: Int, b: Int, a: Int, color: NVGColor) = {
          color.r(r / 255.0f)
          color.g(g / 255.0f)
          color.b(b / 255.0f)
          color.a(a / 255.0f)
          color
        }

        private def toByteBuffer(resource: Path): Task[ByteBuffer] =
          for {
            fc  <- Task.effect { Files.newByteChannel(resource) }
            buf <- buffers.byteBuffer(fc.size().toInt + 1)
            res <- read(fc, buf)
          } yield res

        private def read(fc: ByteChannel, buf: ByteBuffer): Task[ByteBuffer] =
          Task.effect(fc.read(buf)).flatMap { r =>
            if (r == -1) {
              Task.effect(buf.flip().asInstanceOf[ByteBuffer])
            } else {
              read(fc, buf)
            }
          }
      })
    }
  }
  final val hudRenderer: ZIO[HudRenderer, Nothing, HudRenderer.Service] =
    ZIO.access(_.get)

  final def init(font: Path): ZIO[HudRenderer, LoadingError, HudContext] =
    ZIO.accessM(_.get.init(font))

  def render(context: HudContext, windowSize: WindowSize, state: HudState): ZIO[HudRenderer, Nothing, Unit] =
    ZIO.accessM(_.get.render(context, windowSize, state))
}
