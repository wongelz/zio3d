package zio3d

import zio3d.engine.RenderEnv
import zio3d.game.hud.HudRenderer

package object game {
  type GameEnv = RenderEnv with HudRenderer
}
