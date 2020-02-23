package zio3d.core

import java.nio.{ByteBuffer, DoubleBuffer, FloatBuffer, IntBuffer}

import org.lwjgl.BufferUtils
import zio._

package object buffers {

  type Buffers = Has[Buffers.Service]

  object Buffers extends Serializable {

    trait Service {
      def intBuffer(capacity: Int): UIO[IntBuffer]

      def floatBuffer(capacity: Int): UIO[FloatBuffer]

      def doubleBuffer(capacity: Int): UIO[DoubleBuffer]

      def byteBuffer(capacity: Int): UIO[ByteBuffer]

      def intBuffer(data: Array[Int]): UIO[IntBuffer]

      def floatBuffer(data: Array[Float]): UIO[FloatBuffer]

      def byteBuffer(data: Array[Byte]): UIO[ByteBuffer]
    }

    val live = ZLayer.succeed {
      new Service {
        def intBuffer(capacity: Int) =
          IO.effectTotal {
            BufferUtils.createIntBuffer(capacity)
          }

        def floatBuffer(capacity: Int) =
          IO.effectTotal {
            BufferUtils.createFloatBuffer(capacity)
          }

        def doubleBuffer(capacity: Int) =
          IO.effectTotal {
            BufferUtils.createDoubleBuffer(capacity)
          }

        def byteBuffer(capacity: Int) =
          IO.effectTotal {
            BufferUtils.createByteBuffer(capacity)
          }

        def intBuffer(data: Array[Int]) =
          IO.effectTotal {
            BufferUtils
              .createIntBuffer(data.length)
              .put(data)
              .flip()
              .asInstanceOf[IntBuffer]
          }

        def floatBuffer(data: Array[Float]) =
          IO.effectTotal {
            BufferUtils
              .createFloatBuffer(data.length)
              .put(data)
              .flip()
              .asInstanceOf[FloatBuffer]
          }

        def byteBuffer(data: Array[Byte]) =
          IO.effectTotal {
            BufferUtils
              .createByteBuffer(data.length)
              .put(data)
              .flip()
              .asInstanceOf[ByteBuffer]
          }
      }
    }
  }

  final val buffers: ZIO[Buffers, Nothing, Buffers.Service] =
    ZIO.access(_.get)

  final def intBuffer(capacity: Int): ZIO[Buffers, Nothing, IntBuffer] =
    ZIO.accessM(_.get.intBuffer(capacity))

  def floatBuffer(capacity: Int): ZIO[Buffers, Nothing, FloatBuffer] =
    ZIO.accessM(_.get.floatBuffer(capacity))

  final def doubleBuffer(capacity: Int): ZIO[Buffers, Nothing, DoubleBuffer] =
    ZIO.accessM(_.get.doubleBuffer(capacity))

  final def byteBuffer(capacity: Int): ZIO[Buffers, Nothing, ByteBuffer] =
    ZIO.accessM(_.get.byteBuffer(capacity))

  final def intBuffer(data: Array[Int]): ZIO[Buffers, Nothing, IntBuffer] =
    ZIO.accessM(_.get.intBuffer(data))

  final def floatBuffer(data: Array[Float]): ZIO[Buffers, Nothing, FloatBuffer] =
    ZIO.accessM(_.get.floatBuffer(data))

  final def byteBuffer(data: Array[Byte]): ZIO[Buffers, Nothing, ByteBuffer] =
    ZIO.accessM(_.get.byteBuffer(data))
}
