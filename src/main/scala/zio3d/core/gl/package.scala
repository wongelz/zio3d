package zio3d.core

import org.lwjgl.opengl.GLCapabilities
import zio.ZIO

package object gl {

  final val glService: ZIO[GL, Nothing, GL.Service] =
    ZIO.access(_.gl)

  final def createCapabilities: ZIO[GL, Nothing, GLCapabilities] =
    ZIO.accessM(_.gl.createCapabilities)

  final def enable(target: Int): ZIO[GL, Nothing, Unit] =
    ZIO.accessM(_.gl.enable(target))

  final def clearColor(r: Float, g: Float, b: Float, a: Float): ZIO[GL, Nothing, Unit] =
    ZIO.accessM(_.gl.clearColor(r, g, b, a))

  final def clear(mask: Int): ZIO[GL, Nothing, Unit] =
    ZIO.accessM(_.gl.clear(mask))
}
