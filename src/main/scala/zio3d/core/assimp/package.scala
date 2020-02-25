package zio3d.core

import java.nio.IntBuffer
import java.nio.file.Path

import org.lwjgl.assimp.Assimp.{aiGetErrorString, aiGetMaterialColor, aiGetMaterialTexture, aiImportFile}
import org.lwjgl.assimp.{AIColor4D, AIMaterial, AIScene, AIString}
import zio._
import zio3d.core.math.Vector4
import zio3d.engine.MaterialDefinition
import zio3d.engine.loaders.LoadingError

package object assimp {

  type Assimp = Has[Assimp.Service]

  object Assimp extends Serializable {
    trait Service {
      def importFile(resourcePath: Path, flags: Int): IO[LoadingError, AIScene]

      def getMaterialTexture(mat: AIMaterial, `type`: Int): UIO[Option[String]]

      def getMaterialColor(mat: AIMaterial, pKey: String, `type`: Int): UIO[Vector4]
    }

    val live = ZLayer.succeed[Assimp.Service] {
      new Service {
        def getErrorString: UIO[Option[String]] =
          UIO.effectTotal { Option(aiGetErrorString()) }

        override def importFile(resourcePath: Path, flags: Int) =
          IO.fromOption(Option(aiImportFile(resourcePath.toString, flags))).flatMapError { _ =>
            getErrorString.map { str =>
              LoadingError.FileLoadError(resourcePath.toString, str.getOrElse("No error reported by Assimp"))
            }
          }

        override def getMaterialTexture(mat: AIMaterial, `type`: Int): UIO[Option[String]] =
          UIO.effectTotal {
            val path = AIString.calloc()
            aiGetMaterialTexture(mat, `type`, 0, path, null.asInstanceOf[IntBuffer], null, null, null, null, null)
            if (path.length() == 0) None else Some(path.dataString())
          }

        override def getMaterialColor(mat: AIMaterial, pKey: String, `type`: Int) =
          UIO.effectTotal {
            val color  = AIColor4D.create()
            val result = aiGetMaterialColor(mat, pKey, `type`, 0, color)
            if (result == 0) {
              Vector4(color.r(), color.g(), color.b(), color.a())
            } else {
              MaterialDefinition.DefaultColour
            }
          }
      }
    }
  }

  final val assimp: ZIO[Assimp, Nothing, Assimp.Service] =
    ZIO.access(_.get)
}
