package zio3d.engine.loaders.texture

import java.nio.file.Path

import org.lwjgl.opengl.GL11.{GL_TEXTURE_2D, GL_UNPACK_ALIGNMENT, GL_UNSIGNED_BYTE}
import zio.IO
import zio3d.core.gl.GL
import zio3d.core.gl.GL.Texture
import zio3d.core.images.Images
import zio3d.engine._
import zio3d.engine.loaders.LoadingError.FileLoadError

trait TextureLoader extends GL with Images {
  def textureLoader: TextureLoader.Service
}

object TextureLoader {

  trait Service {

    /**
     * Load a texture from a filename.
     *
     * @param source       to load.
     * @param flipVertical Flips the image vertically, so the first pixel in the output array is the bottom left.
     * @return texture.
     */
    def loadTexture(source: Path, flipVertical: Boolean): IO[FileLoadError, Texture]

    /**
     * Load a material from a definition.
     *
     * @param materialDefinition to load.
     * @return material.
     */
    def loadMaterial(materialDefinition: MaterialDefinition): IO[FileLoadError, Material]
  }

  trait Live extends TextureLoader {

    // dependencies
    val gl: GL.Service
    val images: Images.Service

    val textureLoader = new Service {

      /**
       * Load a texture from a filename.
       *
       * @param source       to load.
       * @param flipVertical Flips the image vertically, so the first pixel in the output array is the bottom left.
       * @return texture.
       */
      override def loadTexture(source: Path, flipVertical: Boolean): IO[FileLoadError, Texture] =
        for {
          img <- images.loadImage(source, flipVertical, 0)

          t <- gl.genTextures
          _ <- gl.bindTexture(GL_TEXTURE_2D, t)
          _ <- gl.pixelStorei(GL_UNPACK_ALIGNMENT, 1)
          _ <- gl.texImage2D(
                GL_TEXTURE_2D,
                0,
                img.format,
                img.width,
                img.height,
                0,
                img.format,
                GL_UNSIGNED_BYTE,
                img.image
              )
          _ <- gl.generateMipMap(GL_TEXTURE_2D)
        } yield t

      /**
       * Load a material from a definition.
       *
       * @param materialDefinition to load.
       * @return material.
       */
      override def loadMaterial(materialDefinition: MaterialDefinition): IO[FileLoadError, Material] =
        materialDefinition.texture match {
          case None => IO.succeed(Material.empty)
          case Some(t) =>
            loadTexture(t.path, t.flipVertical) map { t =>
              Material.textured(t)
            }
        }
    }
  }
}
