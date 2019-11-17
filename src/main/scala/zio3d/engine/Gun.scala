package zio3d.engine

import zio3d.core.math.Vector3

final case class Gun(
  bulletModel: Model,
  baseParticle: Particle,
  particles: List[Particle],
  maxParticles: Int,
  firingRateMillis: Long,
  lastFireTime: Long,
  speed: Float
) {
  def renderItems: List[ItemInstance] =
    particles.map(_.item)

  def update(elapsedTime: Long): Gun =
    copy(particles = updateParticles(elapsedTime))

  private def updateParticles(elapsedTime: Long) =
    particles.foldLeft(List.empty[Particle]) { (ps, p) =>
      p.update(elapsedTime).fold(ps)(_ :: ps)
    }

  def fire(time: Long, from: Vector3, direction: Vector3): Gun =
    if (time - lastFireTime >= firingRateMillis && particles.length < maxParticles) {
      val newParticle = baseParticle.copy(
        item = baseParticle.item.withPosition(from),
        speed = direction * speed
      )
      copy(particles = newParticle :: particles, lastFireTime = time)
    } else {
      this
    }

}
