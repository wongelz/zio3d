package zio3d.game.hud

import java.nio.ByteBuffer

import org.lwjgl.nanovg.NVGColor
import zio3d.core.nvg.NVGContext

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
