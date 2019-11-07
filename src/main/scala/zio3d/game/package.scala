package zio3d

import zio3d.engine.RenderEnv
import zio3d.game.config.GameConfig

package object game {
  type GameEnv = RenderEnv with GameConfig
}
