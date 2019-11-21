package zio3d.game.hud

import java.nio.ByteBuffer
import java.nio.channels.ByteChannel
import java.nio.file.{Files, Path}

import org.lwjgl.nanovg.NVGColor
import org.lwjgl.nanovg.NanoVG.{NVG_ALIGN_CENTER, NVG_ALIGN_TOP}
import org.lwjgl.nanovg.NanoVGGL3.NVG_STENCIL_STROKES
import zio.{IO, Task, UIO}
import zio3d.core.buffers.Buffers
import zio3d.core.glfw.WindowSize
import zio3d.core.nvg.{NVGContext, _}
import zio3d.engine.loaders.LoadingError

trait HudRenderer {

  def hudRenderer: HudRenderer.Service
}

object HudRenderer {

  final case class HudContext(
    vg: NVGContext,
    color: NVGColor,
    fontBuffer: ByteBuffer // pin the buffer here so it doesn't get GC'ed
  )

  final case class HudState(
    frameStartMillis: Long,
    frameCount: Int,
    fps: Int
  ) {

    def incFrames(now: Long): HudState =
      if (now - frameStartMillis > 1000) {
        HudState(now, 0, frameCount)
      } else {
        copy(frameCount = frameCount + 1)
      }
  }

  object HudState {
    def initial(startMillis: Long): HudState =
      HudState(startMillis, 0, 0)
  }

  trait Service {
    def init(font: Path): IO[LoadingError, HudContext]

    def render(context: HudContext, windowSize: WindowSize, state: HudState): UIO[Unit]
  }

  trait Live extends HudRenderer {

    // dependencies
    val buffers: Buffers.Service
    val nvg: NVG.Service

    val FontName = "BOLD"

    final val hudRenderer = new Service {
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
    }
  }
}
