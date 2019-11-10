package zio3d.game.config

import java.nio.file.Path

import zio3d.core.math.{toRadians, AxisAngle4, Quaternion, Vector2, Vector3}
import zio3d.engine.{Fog, SkyboxDefinition}
import zio3d.game.GameResources.{models, textures}
import zio3d.game.Perspective

final case class GunConfig(
  maxBullets: Int,
  firingRateMillis: Long,
  bulletSpeed: Float,
  bulletTtl: Long
)

final case class FireConfig(
  randomRange: Float,
  maxParticles: Int,
  particleTtl: Long,
  particleSpeed: Vector3,
  particleCreationPeriodMillis: Long,
  particleUpdateTextureMillis: Long,
  animRange: Float
)

final case class GameConfig(
  perspective: Perspective,
  gun: GunConfig,
  fire: FireConfig,
  level: GameLevel
)

object GameConfig {
  val perspective = Perspective(
    fov = toRadians(60.0f),
    zNear = 0.01f,
    zFar = 1000.0f
  )

  val gun = GunConfig(
    maxBullets = 100,
    firingRateMillis = 100,
    bulletSpeed = 20.0f,
    bulletTtl = 1000
  )

  val fire = FireConfig(
    randomRange = 0.3f,
    maxParticles = 100,
    particleTtl = 1500,
    particleSpeed = Vector3(0, 1, 0) * 0.4f,
    particleCreationPeriodMillis = 300,
    particleUpdateTextureMillis = 200,
    animRange = 10
  )

  val live = GameConfig(
    perspective = perspective,
    gun = gun,
    fire = fire,
    GameLevel.level1
  )
}

final case class Instance(
  position: Vector2,
  orientation: Float = 0f
)

final case class GameObject(
  model: Path,
  scale: Float,
  boxSize: Float,
  instances: List[Instance],
  rotation: Quaternion = Quaternion.Zero
)

final case class TerrainDefinition(
  size: Int,
  scale: Float,
  minY: Float,
  maxY: Float,
  heightMap: Path,
  textureFile: Path,
  textInc: Int
)

final case class GameLevel(
  terrain: TerrainDefinition,
  sky: SkyboxDefinition,
  fog: Fog,
  ambientLight: Vector3,
  staticObjects: List[GameObject],
  monsters: List[GameObject],
  startPosition: Vector2
)

object GameLevel {
  val sky = SkyboxDefinition(
    textures.skybox.front,
    textures.skybox.back,
    textures.skybox.up,
    textures.skybox.down,
    textures.skybox.left,
    textures.skybox.right,
    50f
  )

  val terrain = TerrainDefinition(
    3,
    20,
    -0.1f,
    0.1f,
    textures.heightmap,
    textures.terrain,
    40
  )

  val fog = Fog(active = true, Vector3(0.5f, 0.5f, 0.5f), 0.05f)

  val level1 = GameLevel(
    terrain = terrain,
    sky = sky,
    fog = fog,
    ambientLight = Vector3(0.5f, 0.5f, 0.5f),
    staticObjects = List(
      GameObject(
        models.house,
        scale = 0.5f,
        boxSize = 5f,
        instances = List(
          Instance(Vector2(0f, 15f))
        )
      ),
      GameObject(
        models.tree,
        scale = 0.02f,
        rotation = Quaternion(AxisAngle4(toRadians(-90), 1, 0, 0)),
        boxSize = 1f,
        instances = List(
          Instance(Vector2(10f, 10f)),
          Instance(Vector2(-10f, 15f)),
          Instance(Vector2(-11f, 5f)),
          Instance(Vector2(-20f, 14f)),
          Instance(Vector2(-13f, 11f))
        )
      )
    ),
    monsters = List(
      GameObject(
        models.boblamp,
        scale = 0.02f,
        boxSize = 1.0f,
        instances = List(
          Instance(Vector2(-2f, 15f))
        )
      ),
      GameObject(
        models.monster,
        scale = 0.02f,
        boxSize = 1.5f,
        instances =
          List(
            Instance(Vector2(-2f, -8f), toRadians(-90f)),
            Instance(Vector2(2f, -8f), toRadians(-90f))
          ) ++
            createArmyPositions(Vector2(0, -25f), 25, 8, 1.5f).map(p => Instance(p, toRadians(-90))).toList
      )
    ),
    startPosition = Vector2.origin
  )

  def createArmyPositions(pos: Vector2, rows: Int, cols: Int, spacing: Float) = {
    val x = pos.x - (rows * spacing / 2)
    val y = pos.y - (cols * spacing / 2)
    for {
      r <- 0 until rows
      c <- 0 until cols
    } yield Vector2(x + r * spacing, y + c * spacing)
  }
}
