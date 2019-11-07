package zio3d.game

import java.nio.file.Path

import zio.ZIO
import zio3d.core.glfw.WindowSize
import zio3d.engine.loaders.LoadingError
import zio3d.game.hud.HudRenderer.{HudContext, HudState}

package object hud {

  final val hudRenderer: ZIO[HudRenderer, Nothing, HudRenderer.Service] =
    ZIO.access(_.hudRenderer)

  final def init(font: Path): ZIO[HudRenderer, LoadingError, HudContext] =
    ZIO.accessM(_.hudRenderer.init(font))

  def render(context: HudContext, windowSize: WindowSize, state: HudState): ZIO[HudRenderer, Nothing, Unit] =
    ZIO.accessM(_.hudRenderer.render(context, windowSize, state))
}
