package zio3d.engine.loaders.particles

import java.nio.file.Path

import zio.IO
import zio.random.Random
import zio3d.core.math.Vector3
import zio3d.engine._
import zio3d.engine.loaders.LoadingError
import zio3d.engine.loaders.assimp.StaticMeshLoader
import zio3d.engine.loaders.texture.TextureLoader
import zio3d.engine.shaders.particle.ParticleShaderInterpreter
import zio3d.engine.shaders.particle.ParticleShaderInterpreter.ParticleShaderProgram

trait ParticleLoader {
  def particleLoader: ParticleLoader.Service
}

object ParticleLoader {
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
    ): IO[LoadingError, Fire]
  }

  trait Live
      extends StaticMeshLoader.Live
      with ParticleShaderInterpreter.Live
      with TextureLoader.Live
      with Random.Live
      with ParticleLoader {

    val particleLoader = new Service {

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
        } yield Fire(
          Model.still(mesh),
          Particle(ItemInstance(Vector3(0f, 1f, 0f), 1.0f, texAnim), particleSpeed, ttl),
          List.empty,
          maxParticles,
          creationPeriodMillis,
          0L,
          0L,
          ttl,
          speedRndRange,
          positionRndRange,
          scaleRndRange,
          animRange
        )
    }
  }
}
