package zio3d.engine.loaders.assimp

import java.nio.file.Path

import org.lwjgl.assimp.Assimp._
import org.lwjgl.assimp._
import zio.stream.{Sink, Stream}
import zio._
import zio3d.core.assimp.Assimp
import zio3d.core.math.{Matrix4, Quaternion}
import zio3d.engine.loaders.LoadingError
import zio3d.engine._

import scala.jdk.CollectionConverters._

package object anim {

  type AnimMeshLoader = Has[AnimMeshLoader.Service]

  object AnimMeshLoader extends Serializable {
    trait Service {
      def load(resourcePath: Path): IO[LoadingError, AnimMesh]

      def load(resourcePath: Path, flags: Int): IO[LoadingError, AnimMesh]
    }

    val live = ZLayer.fromService[Assimp.Service, AnimMeshLoader.Service] { assimp =>
      new Service {

        override def load(resourcePath: Path): IO[LoadingError, AnimMesh] =
          load(
            resourcePath,
            aiProcess_GenSmoothNormals | aiProcess_JoinIdenticalVertices | aiProcess_Triangulate
              | aiProcess_FixInfacingNormals | aiProcess_LimitBoneWeights
          )

        override def load(resourcePath: Path, flags: Int): IO[LoadingError, AnimMesh] =
          for {
            s <- assimp.importFile(resourcePath, flags)

            aiMaterials = s.mMaterials()
            materials <- Stream
              .range(0, s.mNumMaterials())
              .mapM(i => processMaterial(AIMaterial.create(aiMaterials.get(i)), resourcePath.getParent))
              .run(Sink.collectAll[MaterialDefinition])

            aiMeshes    = s.mMeshes()
            boneCounter <- Ref.make(0)
            meshBones <- Stream
              .range(0, s.mNumMeshes())
              .mapM(i => processMesh(s, AIMesh.create(aiMeshes.get(i)), materials.toList, boneCounter))
              .run(Sink.collectAll[MeshBones])

            bones = meshBones.flatMap(_.bones)
            //          rootNode  <- processAnimations(s, bones)
            rootTrans  = toMatrix(s.mRootNode().mTransformation())
            animations <- processAnimations(s, bones, rootTrans)

          } yield AnimMesh(meshBones.map(_.mesh).toList, animations)

        private def processMaterial(material: AIMaterial, parentPath: Path) =
          for {
            t <- assimp.getMaterialTexture(material, aiTextureType_DIFFUSE)
            a <- assimp.getMaterialColor(material, AI_MATKEY_COLOR_AMBIENT, aiTextureType_NONE)
            d <- assimp.getMaterialColor(material, AI_MATKEY_COLOR_DIFFUSE, aiTextureType_NONE)
            s <- assimp.getMaterialColor(material, AI_MATKEY_COLOR_SPECULAR, aiTextureType_NONE)
          } yield MaterialDefinition(a, d, s, t.map(p => TextureDefinition(parentPath.resolve(p))), None)

        private def processMesh(
          scene: AIScene,
          mesh: AIMesh,
          materials: List[MaterialDefinition],
          boneCounter: Ref[Int]
        ) =
          for {
            v  <- toVectorArray(mesh.mVertices())
            n  <- toVectorArray(mesh.mNormals())
            t  <- getTextureCoords(mesh)
            i  <- getIndices(mesh)
            b  <- getBones(mesh, boneCounter)
            bw <- getBoneIdsAndWeights(b, mesh.mNumVertices())

            im  = mesh.mMaterialIndex()
            mat = if (im >= 0 && im < materials.length) materials(im) else MaterialDefinition.empty
          } yield MeshBones(
            MeshDefinition(v.toArray, t.toArray, n.toArray, i.toArray, bw._2.toArray, bw._1.toArray, mat),
            b.toList
          )

        private def toVectorArray(buffer: AIVector3D.Buffer) =
          Stream
            .fromIterable[AIVector3D](buffer.iterator().asScala.iterator.to(Iterable))
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

        private def getBones(mesh: AIMesh, boneCounter: Ref[Int]): UIO[Chunk[Bone]] =
          Stream
            .range(0, mesh.mNumBones())
            .mapM(
              i =>
                for {
                  id <- boneCounter.get
                  b  <- createBone(id, AIBone.create(mesh.mBones().get(i)))
                  _  <- boneCounter.update(_ + 1)
                } yield b
            )
            .run(Sink.collectAll[Bone])

        private def getBoneIdsAndWeights(bones: Chunk[Bone], numVertices: Int) = {
          def getWeights(vertexWeightMap: Map[Int, Chunk[VertexWeight]]): Seq[Float] =
            for {
              i             <- 0 until numVertices
              j             <- 0 until MeshDefinition.MaxWeights
              vertexWeights = vertexWeightMap.getOrElse(i, Nil)
              n             = vertexWeights.length
            } yield if (j < n) vertexWeights(j).weight else 0.0f

          def getBoneIds(vertexWeightMap: Map[Int, Chunk[VertexWeight]]): Seq[Int] =
            for {
              i             <- 0 until numVertices
              j             <- 0 until MeshDefinition.MaxWeights
              vertexWeights = vertexWeightMap.getOrElse(i, Nil)
              n             = vertexWeights.length
            } yield if (j < n) vertexWeights(j).boneId else 0

          for {
            ws <- Stream
              .fromIterable(bones)
              .mapConcatChunk(b => Chunk.fromIterable(b.weights))
              .run(Sink.collectAll[VertexWeight])
              .map(_.groupBy(_.vertexId))
          } yield (getBoneIds(ws), getWeights(ws))
        }

        private def createBone(id: Int, bone: AIBone) =
          for {
            ws <- Stream
              .fromIterable[AIVertexWeight](bone.mWeights().iterator().asScala.to(Iterable))
              .map(w => VertexWeight(id, w.mVertexId(), w.mWeight()))
              .run(Sink.collectAll[VertexWeight])
          } yield Bone(id, bone.mName().dataString(), toMatrix(bone.mOffsetMatrix()), ws.toList)

        private def processNodeHierarchy(aiNode: AINode, parentNode: Option[Node]): UIO[Node] =
          for {
            n <- UIO.succeed(Node(aiNode.mName().dataString(), parentNode))
            c <- UIO.foreach(0 until aiNode.mNumChildren())(
              i => processNodeHierarchy(AINode.create(aiNode.mChildren().get(i)), Some(n))
            )
          } yield n.copy(children = c)

        private def processAnimations(scene: AIScene, bones: Chunk[Bone], rootTrans: Matrix4) = {
          val aiRootNode = scene.mRootNode()
          for {
            rootNode <- processNodeHierarchy(aiRootNode, None)
            animations <- UIO.foreach(0 until scene.mNumAnimations())(
              i => buildAnimation(AIAnimation.create(scene.mAnimations().get(i)), bones.toList, rootTrans, rootNode)
            )
          } yield animations
        }

        private def buildAnimation(a: AIAnimation, bones: List[Bone], rootTrans: Matrix4, rootNode: Node) =
          for {
            _      <- populateNodes(a, rootNode)
            frames <- buildAnimationFrames(bones, rootNode, rootTrans)
          } yield Animation(a.mName().dataString(), frames.toArray, a.mDuration())

        private def populateNodes(anim: AIAnimation, rootNode: Node) =
          Stream
            .range(0, anim.mNumChannels())
            .map(i => AINodeAnim.create(anim.mChannels().get(i)))
            .mapM { nodeAnim =>
              getTransformationMatrices(nodeAnim) map { mats =>
                rootNode.findByName(nodeAnim.mNodeName().dataString()) map { n =>
                  n.addTransformations(mats)
                }
              }
            }
            .run(Sink.drain)

        private def getTransformationMatrices(nodeAnim: AINodeAnim) =
          Stream
            .range(0, nodeAnim.mNumPositionKeys())
            .map { i =>
              val posVec   = nodeAnim.mPositionKeys().get(i).mValue()
              val transMat = Matrix4.forTranslation(posVec.x(), posVec.y(), posVec.z())

              val rotQuat = nodeAnim.mRotationKeys().get(i).mValue()
              val quat    = Quaternion(rotQuat.x(), rotQuat.y(), rotQuat.z(), rotQuat.w())

              if (i < nodeAnim.mNumScalingKeys()) {
                val scaleVec = nodeAnim.mScalingKeys().get(i).mValue()
                transMat
                  .rotate(quat)
                  .scale(scaleVec.x(), scaleVec.y(), scaleVec.z())
              } else {
                transMat
                  .rotate(quat)
              }
            }
            .run(Sink.collectAll[Matrix4])

        private def buildAnimationFrames(bones: List[Bone], rootNode: Node, rootTransform: Matrix4) = {
          def getFrame(i: Int) = {
            val matrices = bones.map { b =>
              val node       = rootNode.findByName(b.boneName)
              val boneMatrix = Node.getParentTransforms(node, i)
              rootTransform * boneMatrix * b.offset
            }
            AnimatedFrame(matrices.toArray)
          }

          val numFrames = rootNode.getAnimationFrames()
          Stream
            .range(0, numFrames)
            .map(i => getFrame(i))
            .run(Sink.collectAll[AnimatedFrame])
        }

        private def toMatrix(m: AIMatrix4x4) =
          Matrix4(
            m.a1(),
            m.b1(),
            m.c1(),
            m.d1(),
            m.a2(),
            m.b2(),
            m.c2(),
            m.d2(),
            m.a3(),
            m.b3(),
            m.c3(),
            m.d3(),
            m.a4(),
            m.b4(),
            m.c4(),
            m.d4()
          )
      }
    }
  }

  final def loadAnimMesh(resourcePath: Path): ZIO[AnimMeshLoader, LoadingError, AnimMesh] =
    ZIO.accessM(_.get.load(resourcePath))

}
