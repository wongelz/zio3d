package zio3d.engine

import zio.random.Random
import zio.{random, ZIO}
import zio3d.core.math.Vector3

final case class FireSettings(
  maxParticles: Int,
  particleSpeed: Vector3,
  particleScale: Float,
  creationPeriodMillis: Long,
  ttl: Long,
  speedRndRange: Float,
  positionRndRange: Float,
  scaleRndRange: Float,
  animRange: Float
)

final case class Fires(
  model: Model, // model for all fires
  textureAnimation: TextureAnimation,
  settings: FireSettings,
  fires: List[Fire]
) {
  def gameItem: GameItem = {
    val items = for {
      f <- fires
      p <- f.particles
    } yield p.item
    GameItem(model, items)
  }

  def update(now: Long, elapsedTime: Long): ZIO[Random, Nothing, Fires] =
    ZIO.foreach(fires)(_.update(settings, now, elapsedTime)) map { fs =>
      copy(fires = fs.flatten)
    }

  def startFire(time: Long, position: Vector3): Fires =
    copy(
      fires = Fire(
        Particle(
          ItemInstance(position, settings.particleScale, textureAnimation),
          settings.particleSpeed,
          settings.ttl
        ),
        Nil,
        time,
        0
      ) :: Nil
    )

  def ++(o: Fires): Fires =
    copy(fires = fires ++ o.fires)
}

final case class Fire(
  baseParticle: Particle,
  particles: List[Particle],
  creationTime: Long,
  lastCreationTime: Long
) {

  def update(settings: FireSettings, now: Long, elapsedTime: Long): ZIO[Random, Nothing, Option[Fire]] =
    if (now > creationTime + settings.ttl) {
      if (particles.isEmpty) {
        ZIO.succeed(None)
      } else {
        ZIO.succeed(
          Some(
            copy(
              particles = updateParticles(elapsedTime)
            )
          )
        )
      }
    } else {
      for {
        n <- createParticle(settings, now)
      } yield Some(
        copy(
          particles = n.toList ++ updateParticles(elapsedTime),
          lastCreationTime =
            if (n.isEmpty) lastCreationTime
            else now
        )
      )
    }

  private def updateParticles(elapsedTime: Long) =
    particles.foldLeft(List.empty[Particle]) { (ps, p) =>
      p.update(elapsedTime).fold(ps)(_ :: ps)
    }

  def createParticle(settings: FireSettings, time: Long): ZIO[Random, Nothing, Option[Particle]] =
    if (time - lastCreationTime >= settings.creationPeriodMillis && particles.length < settings.maxParticles) {
      for {
        rs       <- random.nextBoolean
        sign     = if (rs) 1.0f else -1.0f
        speedInc <- random.nextFloat
        posInc   <- random.nextFloat
        scaleInc <- random.nextFloat
      } yield Some(
        Particle.create(
          baseParticle,
          sign * posInc * settings.positionRndRange,
          sign * speedInc * settings.speedRndRange,
          sign * scaleInc * settings.scaleRndRange
        )
      )
    } else {
      ZIO.succeed(None)
    }

}
