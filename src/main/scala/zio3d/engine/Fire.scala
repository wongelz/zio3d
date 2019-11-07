package zio3d.engine

import zio.random.Random
import zio.{random, ZIO}
import zio3d.core.math.Vector3

final case class Fire(
  baseParticle: Particle,
  particles: List[Particle],
  maxParticles: Int,
  creationPeriodMillis: Long,
  lastCreationTime: Long,
  creationTime: Long,
  ttl: Long,
  speedRndRange: Float,
  positionRndRange: Float,
  scaleRndRange: Float,
  animRange: Float
) {

  def duplicate(time: Long, position: Vector3): Fire =
    copy(
      baseParticle = baseParticle.copy(item = baseParticle.item.withPosition(position)),
      particles = Nil,
      creationTime = time,
      lastCreationTime = 0
    )

  def renderItems: List[GameItem] =
    particles.map(_.item)

  def update(now: Long, elapsedTime: Long): ZIO[Random, Nothing, Option[Fire]] =
    if (now > creationTime + ttl) {
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
        n <- createParticle(now)
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

  def createParticle(time: Long): ZIO[Random, Nothing, Option[Particle]] =
    if (time - lastCreationTime >= creationPeriodMillis && particles.length < maxParticles) {
      for {
        rs       <- random.nextBoolean
        sign     = if (rs) 1.0f else -1.0f
        speedInc <- random.nextFloat
        posInc   <- random.nextFloat
        scaleInc <- random.nextFloat
        animInc  <- random.nextLong
      } yield Some(
        Particle.create(
          baseParticle,
          sign * posInc * positionRndRange,
          sign * speedInc * speedRndRange,
          sign * scaleInc * scaleRndRange,
          sign.toLong * (animInc * animRange).toLong
        )
      )
    } else {
      ZIO.succeed(None)
    }

}
