package zio3d.core

import java.nio.{ByteBuffer, DoubleBuffer, FloatBuffer, IntBuffer}

import zio.ZIO

package object buffers {

  final val buffers: ZIO[Buffers, Nothing, Buffers.Service] =
    ZIO.access(_.buffers)

  final def intBuffer(capacity: Int): ZIO[Buffers, Nothing, IntBuffer] =
    ZIO.accessM(_.buffers.intBuffer(capacity))

  def floatBuffer(capacity: Int): ZIO[Buffers, Nothing, FloatBuffer] =
    ZIO.accessM(_.buffers.floatBuffer(capacity))

  final def doubleBuffer(capacity: Int): ZIO[Buffers, Nothing, DoubleBuffer] =
    ZIO.accessM(_.buffers.doubleBuffer(capacity))

  final def byteBuffer(capacity: Int): ZIO[Buffers, Nothing, ByteBuffer] =
    ZIO.accessM(_.buffers.byteBuffer(capacity))

  final def intBuffer(data: Array[Int]): ZIO[Buffers, Nothing, IntBuffer] =
    ZIO.accessM(_.buffers.intBuffer(data))

  final def floatBuffer(data: Array[Float]): ZIO[Buffers, Nothing, FloatBuffer] =
    ZIO.accessM(_.buffers.floatBuffer(data))

  final def byteBuffer(data: Array[Byte]): ZIO[Buffers, Nothing, ByteBuffer] =
    ZIO.accessM(_.buffers.byteBuffer(data))
}
