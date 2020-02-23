package zio3d.game

import zio.ZIO
import zio.random.Random
import zio3d.core.math.Vector3
import zio3d.engine._
import zio3d.game.hud.HudState

import scala.annotation.tailrec

trait Scene {

  def skyboxItems: List[GameItem]

  def sceneItems: List[GameItem]

  def simpleItems: List[GameItem]

  def particles: List[GameItem]

  def fixtures: Fixtures
}

final case class GameState(
  time: Long,
  terrain: Terrain,
  skybox: GameItem,
  monsters: List[GameItem],
  simpleObjects: List[GameItem],
  sceneObjects: List[GameItem],
  fires: Fires,
  gun: Gun,
  camera: Camera,
  ambientLight: Vector3,
  flashLight: SpotLight,
  fog: Fog,
  hud: HudState
) extends Scene {

  // per millisecond
  final val moveSpeed        = 0.005f
  final val cameraHeight     = 1.8f
  final val mouseSensitivity = 5.0f

  override def skyboxItems = List(skybox)

  override def sceneItems = monsters ++ sceneObjects ++ terrain.blocks

  override def simpleItems = simpleObjects

  override def particles = GameItem(gun.bulletModel, gun.renderItems) :: fires.gameItem :: Nil

  override def fixtures = Fixtures(LightSources(ambientLight, None, Nil, List(flashLight)), fog)

  def nextState(userInput: UserInput, currentTime: Long): ZIO[Random, Nothing, GameState] = {
    val elapsedMillis                                   = currentTime - time
    val (survivingMonsters, destroyedBullets, newFires) = handleBulletCollisions(currentTime, monsters, fires)

    for {
      f <- fires.update(currentTime, elapsedMillis)
      g = if (userInput.mouseButtons.contains(MouseButton.BUTTON_LEFT))
        gun
          .copy(particles = gun.particles.diff(destroyedBullets))
          .update(elapsedMillis)
          .fire(currentTime, camera.position, camera.front)
      else gun.copy(particles = gun.particles.diff(destroyedBullets)).update(elapsedMillis)
      c = nextCamera(userInput, elapsedMillis)
      l = flashLight.withDirection(c.front).withPosition(c.position)
    } yield copy(
      time = currentTime,
      monsters = survivingMonsters.map(_.animate),
      fires = f ++ newFires,
      gun = g,
      camera = c,
      flashLight = l,
      hud = hud.incFrames(currentTime)
    )
  }

  private def handleBulletCollisions(
    time: Long,
    monsters: List[GameItem],
    fires: Fires
  ): (List[GameItem], List[Particle], Fires) =
    monsters.foldLeft((List.empty[GameItem], List.empty[Particle], fires.copy(fires = Nil))) { (acc, m) =>
      val (survivors, destroyedBullets, newFires) =
        handleBulletCollisions(time, m.instances, Nil, Nil, fires.copy(fires = Nil))
      (GameItem(m.model, survivors) :: acc._1, destroyedBullets ++ acc._2, newFires ++ acc._3)
    }

  @tailrec
  private def handleBulletCollisions(
    time: Long,
    mons: List[ItemInstance],
    survivingMons: List[ItemInstance],
    destroyedBullets: List[Particle],
    newFires: Fires
  ): (List[ItemInstance], List[Particle], Fires) =
    mons match {
      case Nil => (survivingMons, destroyedBullets, newFires)
      case m :: ms =>
        gun.particles.find(p => m.aabbContains(p.item.position)) match {
          case Some(p) =>
            handleBulletCollisions(
              time,
              ms,
              survivingMons,
              p :: destroyedBullets,
              newFires.startFire(time, p.item.position)
            )
          case None =>
            handleBulletCollisions(time, ms, m :: survivingMons, destroyedBullets, newFires)
        }
    }

  def nextCamera(userInput: UserInput, elapsedMillis: Long): Camera = {
    val keys           = userInput.keys
    val cursorMovement = userInput.cursorMovement

    // move forward/back
    val dz =
      if (keys.contains(Key.UP)) moveSpeed * elapsedMillis
      else if (keys.contains(Key.DOWN)) -moveSpeed * elapsedMillis
      else 0f

    // strafe
    val dx =
      if (keys.contains(Key.LEFT)) -moveSpeed * elapsedMillis
      else if (keys.contains(Key.RIGHT)) moveSpeed * elapsedMillis
      else 0f

    // still need to look up terrain for y-position...
    val nextCameraProvisional = camera
      .movePosition(dx, 0, dz)

    terrain
      .getHeight(nextCameraProvisional.position)
      .fold(camera)(y => nextCameraProvisional.withHeight(y + cameraHeight))
      .rotate(yawDelta = cursorMovement.x / mouseSensitivity, pitchDelta = -cursorMovement.y / mouseSensitivity)
  }
}
