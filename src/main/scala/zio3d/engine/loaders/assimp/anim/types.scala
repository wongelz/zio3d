package zio3d.engine.loaders.assimp.anim

import zio3d.core.math.Matrix4
import zio3d.engine.{Animation, MeshDefinition}

import scala.collection.mutable.ListBuffer

final case class AnimMesh(
  meshes: List[MeshDefinition],
  animations: List[Animation]
)

final case class MeshBones(
  mesh: MeshDefinition,
  bones: List[Bone]
)

final case class VertexWeight(
  boneId: Int,
  vertexId: Int,
  weight: Float
)

final case class Bone(
  boneId: Int,
  boneName: String,
  offset: Matrix4,
  weights: List[VertexWeight]
)


final case class Node(
  name: String,
  parent: Option[Node],
  children: List[Node] = List.empty,
  transformations: ListBuffer[Matrix4] = new ListBuffer()
) {

  def findByName(targetName: String): Option[Node] =
    if (targetName == name) {
      Some(this)
    } else {
      children.map(_.findByName(targetName)).find(_.isDefined).flatten
    }

  def getAnimationFrames(): Int = {
    val numFrames = transformations.size
    children.foldLeft(numFrames)((f, c) => Math.max(c.getAnimationFrames(), f))
  }

  def addTransformations(ts: List[Matrix4]) = {
    transformations ++= ts
    this
  }
}

object Node {
  def getParentTransforms(node: Option[Node], framePos: Int): Matrix4 =
    node match {
      case None => Matrix4.identity
      case Some(n) =>
        val parentTransform = getParentTransforms(n.parent, framePos)
        val transformations = n.transformations.toList
        val transSize       = transformations.length
        val nodeTransform = if (framePos < transSize) {
          transformations(framePos)
        } else if (transSize > 0) {
          transformations(transSize - 1)
        } else {
          Matrix4.identity
        }
        parentTransform * nodeTransform
    }
}
