package zio3d.engine.loaders

import java.nio.file.Path

import org.lwjgl.opengl.GL11.{GL_TEXTURE_2D, GL_UNPACK_ALIGNMENT, GL_UNSIGNED_BYTE}
import zio.{Has, IO, ZIO, ZLayer}
import zio3d.core.CoreEnv
import zio3d.core.gl.{GL, Texture}
import zio3d.core.images.Images
import zio3d.engine._
import zio3d.engine.loaders.LoadingError.FileLoadError

package object texture {

  type TextureLoader = Has[TextureLoader.Service]

  object TextureLoader extends Serializable {
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

    val live = ZLayer.fromEnvironment[CoreEnv, TextureLoader] { env =>
      Has(new Service {

        private val gl = env.get[GL.Service]
        private val images = env.get[Images.Service]

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
      })
    }
  }

  final val textureLoader: ZIO[TextureLoader, Nothing, TextureLoader.Service] =
    ZIO.access(_.get)

  final def loadTexture(source: Path, flipVertical: Boolean): ZIO[TextureLoader, FileLoadError, Texture] =
    ZIO.accessM(_.get.loadTexture(source, flipVertical))

  final def loadMaterial(materialDefinition: MaterialDefinition): ZIO[TextureLoader, FileLoadError, Material] =
    ZIO.accessM(_.get.loadMaterial(materialDefinition))
}
