package zio3d.engine.loaders

import java.nio.file.Path

import zio.{Has, IO, ZIO, ZLayer}
import zio3d.core.math.Vector3
import zio3d.engine._
import zio3d.engine.shaders.particle.{ParticleShaderInterpreter, ParticleShaderProgram}

package object particles {

  type ParticleLoader = Has[ParticleLoader.Service]

  object ParticleLoader extends Serializable {
    trait Service {
      def loadGun(
        program: ParticleShaderProgram,
        modelFile: Path,
        meshDefinition: SimpleMeshDefinition,
        maxParticles: Int,
        firingRateMillis: Long,
        particleSpeed: Float,
        ttl: Long
      ): IO[LoadingError, Gun]

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
      ): IO[LoadingError, Fires]
    }

    val live = ZLayer.fromFunction[ShaderEnv, ParticleLoader.Service] { env =>
      new Service {

        private val particleShaderInterpreter = env.get[ParticleShaderInterpreter.Service]

        def loadGun(
          program: ParticleShaderProgram,
          modelFile: Path,
          meshDefinition: SimpleMeshDefinition,
          maxParticles: Int,
          firingRateMillis: Long,
          particleSpeed: Float,
          ttl: Long
        ) =
          for {
            mesh <- particleShaderInterpreter.loadMesh(program, meshDefinition)
          } yield Gun(
            Model.still(mesh),
            Particle(ItemInstance(Vector3(0f, 1f, 0f), 1.0f), Vector3.origin, ttl),
            List.empty,
            maxParticles,
            firingRateMillis,
            0,
            particleSpeed
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
        ) =
          for {
            mesh    <- particleShaderInterpreter.loadMesh(program, meshDefinition)
            td      = meshDefinition.material.texture
            texAnim = TextureAnimation(td.fold(1)(_.cols), td.fold(1)(_.rows), textureUpdateMillis)
          } yield Fires(
            Model.still(mesh),
            texAnim,
            FireSettings(
              maxParticles,
              particleSpeed,
              1.0f,
              creationPeriodMillis,
              ttl,
              speedRndRange,
              positionRndRange,
              scaleRndRange,
              animRange
            ),
            List.empty
          )
      }
    }
  }

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
      _.get.loadGun(
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
  ): ZIO[ParticleLoader, LoadingError, Fires] =
    ZIO.accessM(
      _.get.loadFire(
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
