package zio3d.engine.loaders

import java.nio.file.Path

import zio.ZIO
import zio3d.core.math.Vector3
import zio3d.engine.shaders.particle.ParticleShaderInterpreter.ParticleShaderProgram
import zio3d.engine.{Fire, Gun, SimpleMeshDefinition}

package object particles {
  def loadGun(
    program: ParticleShaderProgram,
    modelFile: Path,
    meshDefinition: SimpleMeshDefinition,
    maxParticles: Int,
    firingRateMillis: Long,
    particleSpeed: Float,
    ttl: Long
  ): ZIO[ParticleLoader, LoadingError, Gun] =
    ZIO.accessM(
      _.particleLoader.loadGun(
        program,
        modelFile,
        meshDefinition,
        maxParticles,
        firingRateMillis,
        particleSpeed,
        ttl
      )
    )

  def loadFire(
    program: ParticleShaderProgram,
    modelFile: Path,
    meshDefinition: SimpleMeshDefinition,
    maxParticles: Int,
    particleSpeed: Vector3,
    ttl: Long,
    creationPeriodMillis: Long,
    textureUpdateMillis: Long,
    speedRndRange: Float,
    positionRndRange: Float,
    scaleRndRange: Float,
    animRange: Float
  ): ZIO[ParticleLoader, LoadingError, Fire] =
    ZIO.accessM(
      _.particleLoader.loadFire(
        program,
        modelFile,
        meshDefinition,
        maxParticles,
        particleSpeed,
        ttl,
        creationPeriodMillis,
        textureUpdateMillis,
        speedRndRange,
        positionRndRange,
        scaleRndRange,
        animRange
      )
    )
}
