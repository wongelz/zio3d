package zio3d.engine

import zio3d.core.math.Vector3

final case class Particle(
  item: ItemInstance,
  speed: Vector3,
  ttl: Long
) {

  def update(elapsedTime: Long): Option[Particle] = {
    val ttlRemaining = ttl - elapsedTime
    if (ttlRemaining > 0) {
      val delta = elapsedTime / 1000.0f
      val dx    = speed.x * delta
      val dy    = speed.y * delta
      val dz    = speed.z * delta
      val pos   = item.position

      Some(
        copy(
          item
            .withPosition(pos.x + dx, pos.y + dy, pos.z + dz)
            .animateTexture(elapsedTime),
          ttl = ttlRemaining
        )
      )
    } else {
      None
    }
  }
}

object Particle {
  def create(
    baseParticle: Particle,
    posInc: Float,
    speedInc: Float,
    scaleInc: Float
  ): Particle = {
    val item = baseParticle.item
      .withPosition(baseParticle.item.position + Vector3(posInc, posInc, posInc))
      .withScale(baseParticle.item.scale * scaleInc)
    Particle(item, baseParticle.speed, baseParticle.ttl)
  }
}
