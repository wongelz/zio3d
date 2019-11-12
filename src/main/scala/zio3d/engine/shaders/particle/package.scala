package zio3d.engine.shaders

import zio.ZIO
import zio3d.engine._
import zio3d.engine.loaders.LoadingError
import zio3d.engine.shaders.particle.ParticleShaderInterpreter.ParticleShaderProgram

package object particle {

  final def loadShaderProgram: ZIO[ParticleShaderInterpreter, LoadingError, ParticleShaderProgram] =
    ZIO.accessM(_.particleShaderInterpreter.loadShaderProgram)

  final def render(
    program: ParticleShaderProgram,
    items: Iterable[GameItem],
    transformation: Transformation,
    fixtures: Fixtures
  ): ZIO[ParticleShaderInterpreter, Nothing, Unit] =
    ZIO.accessM(_.particleShaderInterpreter.render(program, items, transformation, fixtures))

  final def loadMesh(
    program: ParticleShaderProgram,
    input: SimpleMeshDefinition
  ): ZIO[ParticleShaderInterpreter, LoadingError, Mesh] =
    ZIO.accessM(_.particleShaderInterpreter.loadMesh(program, input))

  final def cleanup(program: ParticleShaderProgram): ZIO[ParticleShaderInterpreter, Nothing, Unit] =
    ZIO.accessM(_.particleShaderInterpreter.cleanup(program))
}
