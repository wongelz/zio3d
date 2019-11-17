package zio3d.engine.shaders

import zio.ZIO
import zio3d.engine._
import zio3d.engine.loaders.LoadingError

package object scene {

  final def loadShaderProgram: ZIO[SceneShaderInterpreter, LoadingError, SceneShaderProgram] =
    ZIO.accessM(_.sceneShaderInterpreter.loadShaderProgram)

  final def loadMesh(
    program: SceneShaderProgram,
    input: MeshDefinition
  ): ZIO[SceneShaderInterpreter, LoadingError, Mesh] =
    ZIO.accessM(_.sceneShaderInterpreter.loadMesh(program, input))

  final def render(
    program: SceneShaderProgram,
    item: GameItem,
    transformation: Transformation,
    fixtures: Fixtures
  ): ZIO[SceneShaderInterpreter, Nothing, Unit] =
    ZIO.accessM(_.sceneShaderInterpreter.render(program, item, transformation, fixtures))

  final def cleanup(program: SceneShaderProgram): ZIO[SceneShaderInterpreter, Nothing, Unit] =
    ZIO.accessM(_.sceneShaderInterpreter.cleanup(program))
}
