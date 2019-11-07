package zio3d.core.assimp

import java.nio.IntBuffer
import java.nio.file.Path

import org.lwjgl.assimp.Assimp._
import org.lwjgl.assimp.{AIColor4D, AIMaterial, AIScene, AIString}
import zio.{IO, UIO}
import zio3d.core.math.Vector4
import zio3d.engine.MaterialDefinition
import zio3d.engine.loaders.LoadingError

trait Assimp {
  def assimp: Assimp.Service
}

object Assimp {
  trait Service {
    def importFile(resourcePath: Path, flags: Int): IO[LoadingError, AIScene]

    def getMaterialTexture(mat: AIMaterial, `type`: Int): UIO[Option[String]]

    def getMaterialColor(mat: AIMaterial, pKey: String, `type`: Int): UIO[Vector4]
  }

  trait Live extends Assimp {
    val assimp = new Service {
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
