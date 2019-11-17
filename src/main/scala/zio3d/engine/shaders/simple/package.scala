package zio3d.engine.shaders

import zio.ZIO
import zio3d.engine._
import zio3d.engine.loaders.LoadingError
import zio3d.engine.shaders.simple.SimpleShaderInterpreter.SimpleShaderProgram

package object simple {

  final def loadShaderProgram: ZIO[SimpleShaderInterpreter, LoadingError, SimpleShaderProgram] =
    ZIO.accessM(_.simpleShaderInterpreter.loadShaderProgram)

  final def loadMesh(
    program: SimpleShaderProgram,
    input: SimpleMeshDefinition
  ): ZIO[SimpleShaderInterpreter, LoadingError, Mesh] =
    ZIO.accessM(_.simpleShaderInterpreter.loadMesh(program, input))

  final def render(
    program: SimpleShaderProgram,
    item: GameItem,
    transformation: Transformation,
    fixtures: Fixtures
  ): ZIO[SimpleShaderInterpreter, Nothing, Unit] =
    ZIO.accessM(_.simpleShaderInterpreter.render(program, item, transformation, fixtures))

  final def cleanup(program: SimpleShaderProgram): ZIO[SimpleShaderInterpreter, Nothing, Unit] =
    ZIO.accessM(_.simpleShaderInterpreter.cleanup(program))
}
