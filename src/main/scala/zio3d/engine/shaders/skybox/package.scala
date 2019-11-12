package zio3d.engine.shaders

import zio.ZIO
import zio3d.engine._
import zio3d.engine.loaders.LoadingError
import zio3d.engine.shaders.skybox.SkyboxShaderInterpreter.SkyboxShaderProgram

package object skybox {

  final def loadShaderProgram: ZIO[SkyboxShaderInterpreter, LoadingError, SkyboxShaderProgram] =
    ZIO.accessM(_.skyboxShaderInterpreter.loadShaderProgram)

  final def loadMesh(
    program: SkyboxShaderProgram,
    input: SkyboxDefinition
  ): ZIO[SkyboxShaderInterpreter, LoadingError, Mesh] =
    ZIO.accessM(_.skyboxShaderInterpreter.loadMesh(program, input))

  final def render(
    program: SkyboxShaderProgram,
    items: Iterable[GameItem],
    transformation: Transformation,
    fixtures: Fixtures
  ): ZIO[SkyboxShaderInterpreter, Nothing, Unit] =
    ZIO.accessM(_.skyboxShaderInterpreter.render(program, items, transformation, fixtures))

  final def cleanup(program: SkyboxShaderProgram): ZIO[SkyboxShaderInterpreter, Nothing, Unit] =
    ZIO.accessM(_.skyboxShaderInterpreter.cleanup(program))
}
