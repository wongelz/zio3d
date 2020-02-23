package zio3d

import zio.ZLayer
import zio3d.core.assimp.Assimp
import zio3d.core.buffers.Buffers
import zio3d.core.gl.GL
import zio3d.core.glfw.GLFW
import zio3d.core.images.Images
import zio3d.core.nvg.NVG

package object core {
  type CoreEnv = Buffers with GL with GLFW with NVG with Images with Assimp

  object CoreEnv {
    val live: ZLayer.NoDeps[Nothing, CoreEnv] =
      Buffers.live ++
        GL.live ++
        (Buffers.live >>> GLFW.live) ++
        NVG.live ++
        Images.live ++
        Assimp.live
  }
}
