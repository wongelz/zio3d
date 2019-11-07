package zio3d.core.buffers

import java.nio.{ByteBuffer, DoubleBuffer, FloatBuffer, IntBuffer}

import org.lwjgl.BufferUtils
import zio.{IO, UIO}

trait Buffers {
  def buffers: Buffers.Service
}

object Buffers {

  trait Service {
    def intBuffer(capacity: Int): UIO[IntBuffer]

    def floatBuffer(capacity: Int): UIO[FloatBuffer]

    def doubleBuffer(capacity: Int): UIO[DoubleBuffer]

    def byteBuffer(capacity: Int): UIO[ByteBuffer]

    def intBuffer(data: Array[Int]): UIO[IntBuffer]

    def floatBuffer(data: Array[Float]): UIO[FloatBuffer]

    def byteBuffer(data: Array[Byte]): UIO[ByteBuffer]
  }

  trait Live extends Buffers {
    val buffers = new Buffers.Service {
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

  object Live extends Live
}
