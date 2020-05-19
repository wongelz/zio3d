package zio3d.engine

import zio3d.core.glfw.WindowSize
import zio3d.core.math.Matrix4

final case class Perspective(
  fov: Float,
  zNear: Float,
  zFar: Float
) {

  def getTransformation(windowSize: WindowSize, camera: Camera): Transformation = {
    val aspectRatio = windowSize.width.toFloat / windowSize.height.toFloat
    Transformation(
      Matrix4.forPerspective(fov, aspectRatio, zNear, zFar),
      camera.viewMatrix
    )
  }
}
