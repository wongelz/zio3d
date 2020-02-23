package zio3d.core.images

import java.nio.ByteBuffer

import org.lwjgl.opengl.GL11.{GL_RED, GL_RGB, GL_RGBA}

final case class Image(
  image: ByteBuffer,
  width: Int,
  height: Int,
  components: Int
) {

  def format = components match {
    case 1 => GL_RED
    case 3 => GL_RGB
    case 4 => GL_RGBA
  }
}
