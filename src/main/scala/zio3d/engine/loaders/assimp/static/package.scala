package zio3d.engine.loaders.assimp

import java.nio.file.Path

import org.lwjgl.assimp.Assimp._
import org.lwjgl.assimp.{AIFace, AIMaterial, AIMesh, AIVector3D}
import zio._
import zio.stream.{Sink, Stream}
import zio3d.core.assimp.Assimp
import zio3d.engine.loaders.LoadingError
import zio3d.engine.{MaterialDefinition, MeshDefinition, TextureDefinition}

import scala.jdk.CollectionConverters._

package object static {

  type StaticMeshLoader = Has[StaticMeshLoader.Service]

  object StaticMeshLoader extends Serializable {
    trait Service {
      def load(resourcePath: Path): IO[LoadingError, List[MeshDefinition]]

      def load(resourcePath: Path, flags: Int): IO[LoadingError, List[MeshDefinition]]
    }

    val live = ZLayer.fromService[Assimp.Service, StaticMeshLoader.Service] { assimp =>
      new Service {

        override def load(resourcePath: Path): IO[LoadingError, List[MeshDefinition]] =
          load(
            resourcePath,
            aiProcess_GenSmoothNormals |
              aiProcess_JoinIdenticalVertices |
              aiProcess_Triangulate |
              aiProcess_FixInfacingNormals
          )

        override def load(resourcePath: Path, flags: Int): IO[LoadingError, List[MeshDefinition]] =
          for {
            s <- assimp.importFile(resourcePath, flags)

            aiMaterials = s.mMaterials()
            materials <- Stream
              .range(0, s.mNumMaterials())
              .mapM(i => processMaterial(AIMaterial.create(aiMaterials.get(i)), resourcePath.getParent))
              .run(Sink.collectAll[MaterialDefinition])

            aiMeshes = s.mMeshes()
            meshes <- Stream
              .range(0, s.mNumMeshes())
              .mapM(i => processMesh(AIMesh.create(aiMeshes.get(i)), materials.toList))
              .run(Sink.collectAll[MeshDefinition])
          } yield meshes.toList

        private def processMaterial(material: AIMaterial, path: Path): IO[LoadingError, MaterialDefinition] =
          for {
            t <- assimp.getMaterialTexture(material, aiTextureType_DIFFUSE)
            a <- assimp.getMaterialColor(material, AI_MATKEY_COLOR_AMBIENT, aiTextureType_NONE)
            d <- assimp.getMaterialColor(material, AI_MATKEY_COLOR_DIFFUSE, aiTextureType_NONE)
            s <- assimp.getMaterialColor(material, AI_MATKEY_COLOR_SPECULAR, aiTextureType_NONE)
          } yield MaterialDefinition(a, d, s, t.map(p => TextureDefinition(path.resolve(p))), None)

        private def processMesh(mesh: AIMesh, materials: List[MaterialDefinition]) =
          for {
            v   <- toVectorArray(mesh.mVertices())
            n   <- toVectorArray(mesh.mNormals())
            t   <- getTextureCoords(mesh)
            i   <- getIndices(mesh)
            im  = mesh.mMaterialIndex()
            mat = if (im >= 0 && im < materials.length) materials(im) else MaterialDefinition.empty
          } yield MeshDefinition(v.toArray, t.toArray, n.toArray, i.toArray, mat)

        private def toVectorArray(buffer: AIVector3D.Buffer) =
          Stream
            .fromIterable[AIVector3D](buffer.iterator().asScala.to(Iterable))
            .mapConcatChunk(v => Chunk(v.x(), v.y(), v.z()))
            .run(Sink.collectAll[Float])

        private def getTextureCoords(mesh: AIMesh) =
          Stream
            .fromIterable[AIVector3D](mesh.mTextureCoords(0).iterator().asScala.to(Iterable))
            .mapConcatChunk(c => Chunk(c.x(), 1 - c.y()))
            .run(Sink.collectAll[Float])

        private def getIndices(mesh: AIMesh) =
          Stream
            .fromIterable[AIFace](mesh.mFaces().iterator().asScala.to(Iterable))
            .map(_.mIndices())
            .flatMap(buf => Stream.unfold(buf)(b => if (b.hasRemaining) Some((b.get(), b)) else None))
            .run(Sink.collectAll[Int])
      }
    }
  }

  final def loadStaticMesh(resourcePath: Path): ZIO[StaticMeshLoader, LoadingError, List[MeshDefinition]] =
    ZIO.accessM(_.get.load(resourcePath))

  final def loadStaticMesh(resourcePath: Path, flags: Int): ZIO[StaticMeshLoader, LoadingError, List[MeshDefinition]] =
    ZIO.accessM(_.get.load(resourcePath, flags))
}
