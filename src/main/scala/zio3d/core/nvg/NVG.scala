package zio3d.core.nvg

import java.nio.ByteBuffer

import org.lwjgl.nanovg.NVGColor
import org.lwjgl.nanovg.NanoVG._
import org.lwjgl.nanovg.NanoVGGL3._
import zio.UIO

final case class NVGContext(value: Long) extends AnyVal

trait NVG {
  def nvg: NVG.Service
}

object NVG {

  trait Service {
    def create(flags: Int): UIO[NVGContext]

    def beginFrame(ctx: NVGContext, windowWidth: Int, windowHeight: Int, devicePixelRatio: Float): UIO[Unit]

    def endFrame(ctx: NVGContext): UIO[Unit]

    def createFontMem(ctx: NVGContext, name: String, data: ByteBuffer, freeData: Int): UIO[Int]

    def beginPath(ctx: NVGContext): UIO[Unit]

    def rect(ctx: NVGContext, x: Float, y: Float, w: Float, h: Float): UIO[Unit]

    def fillColor(ctx: NVGContext, color: NVGColor): UIO[Unit]

    def fill(ctx: NVGContext): UIO[Unit]

    def circle(ctx: NVGContext, cx: Float, cy: Float, radius: Float): UIO[Unit]

    def fontSize(ctx: NVGContext, size: Float): UIO[Unit]

    def fontFace(ctx: NVGContext, font: String): UIO[Unit]

    def textAlign(ctx: NVGContext, align: Int): UIO[Unit]

    def text(ctx: NVGContext, x: Float, y: Float, string: String): UIO[Float]
  }

  trait Live extends NVG {
    val nvg: NVG.Service = new Service {
      def create(flags: Int): UIO[NVGContext] =
        UIO.effectTotal { NVGContext(nvgCreate(flags)) }

      def beginFrame(ctx: NVGContext, windowWidth: Int, windowHeight: Int, devicePixelRatio: Float): UIO[Unit] =
        UIO.effectTotal { nvgBeginFrame(ctx.value, windowWidth.toFloat, windowHeight.toFloat, devicePixelRatio) }

      def endFrame(ctx: NVGContext): UIO[Unit] =
        UIO.effectTotal { nvgEndFrame(ctx.value) }

      def createFontMem(ctx: NVGContext, name: String, data: ByteBuffer, freeData: Int): UIO[Int] =
        UIO.effectTotal { nvgCreateFontMem(ctx.value, name, data, freeData) }

      def beginPath(ctx: NVGContext): UIO[Unit] =
        UIO.effectTotal { nvgBeginPath(ctx.value) }

      def rect(ctx: NVGContext, x: Float, y: Float, w: Float, h: Float): UIO[Unit] =
        UIO.effectTotal { nvgRect(ctx.value, x, y, w, h) }

      def fillColor(ctx: NVGContext, color: NVGColor): UIO[Unit] =
        UIO.effectTotal { nvgFillColor(ctx.value, color) }

      def fill(ctx: NVGContext): UIO[Unit] =
        UIO.effectTotal { nvgFill(ctx.value) }

      def circle(ctx: NVGContext, cx: Float, cy: Float, radius: Float): UIO[Unit] =
        UIO.effectTotal { nvgCircle(ctx.value, cx, cy, radius) }

      def fontSize(ctx: NVGContext, size: Float): UIO[Unit] =
        UIO.effectTotal { nvgFontSize(ctx.value, size) }

      def fontFace(ctx: NVGContext, font: String): UIO[Unit] =
        UIO.effectTotal { nvgFontFace(ctx.value, font) }

      def textAlign(ctx: NVGContext, align: Int): UIO[Unit] =
        UIO.effectTotal { nvgTextAlign(ctx.value, align) }

      def text(ctx: NVGContext, x: Float, y: Float, string: String): UIO[Float] =
        UIO.effectTotal { nvgText(ctx.value, x, y, string) }
    }
  }
}
