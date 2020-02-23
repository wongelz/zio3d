package zio3d

import zio.ZLayer
import zio.blocking.Blocking
import zio.clock.Clock
import zio.random.Random
import zio3d.core.CoreEnv
import zio3d.core.assimp.Assimp
import zio3d.core.images.Images
import zio3d.engine.glwindow.GLWindow
import zio3d.engine.loaders.assimp.anim.AnimMeshLoader
import zio3d.engine.loaders.assimp.static.StaticMeshLoader
import zio3d.engine.loaders.heightmap.HeightMapLoader
import zio3d.engine.loaders.particles.ParticleLoader
import zio3d.engine.loaders.terrain.TerrainLoader
import zio3d.engine.loaders.texture.TextureLoader
import zio3d.engine.shaders.particle.ParticleShaderInterpreter
import zio3d.engine.shaders.scene.SceneShaderInterpreter
import zio3d.engine.shaders.simple.SimpleShaderInterpreter
import zio3d.engine.shaders.skybox.SkyboxShaderInterpreter

package object engine {

  type SysEnv = Clock with Blocking with Random

  object SysEnv {
    val live: ZLayer.NoDeps[Nothing, SysEnv] =
      Clock.live ++
        Blocking.live ++
        Random.live
  }

  type PreShaderEnv = SysEnv with CoreEnv with TextureLoader

  object PreShaderEnv {
    private val textureLoader: ZLayer.NoDeps[Nothing, TextureLoader] =
      CoreEnv.live >>> TextureLoader.live

    val live: ZLayer.NoDeps[Nothing, PreShaderEnv] =
      SysEnv.live ++
        CoreEnv.live ++
        textureLoader
  }

  type ShaderEnv = PreShaderEnv
    with SimpleShaderInterpreter
    with SkyboxShaderInterpreter
    with SceneShaderInterpreter
    with ParticleShaderInterpreter

  object ShaderEnv {
    val live: ZLayer.NoDeps[Nothing, ShaderEnv] =
      PreShaderEnv.live ++
        (PreShaderEnv.live >>> ((SimpleShaderInterpreter.live) ++
          SkyboxShaderInterpreter.live ++
          SceneShaderInterpreter.live ++
          ParticleShaderInterpreter.live))
  }

  type RenderEnv = ShaderEnv
    with TerrainLoader
    with StaticMeshLoader
    with AnimMeshLoader
    with ParticleLoader
    with GLWindow

  object RenderEnv {
    private val heightMapLoader: ZLayer.NoDeps[Nothing, HeightMapLoader] =
      ShaderEnv.live >>> HeightMapLoader.live
    private val terrainLoader: ZLayer.NoDeps[Nothing, TerrainLoader] =
      (heightMapLoader ++ Images.live) >>> TerrainLoader.live

    val live: ZLayer.NoDeps[Nothing, RenderEnv] =
      ShaderEnv.live ++
        terrainLoader ++
        (Assimp.live >>> StaticMeshLoader.live) ++
        (Assimp.live >>> AnimMeshLoader.live) ++
        (ShaderEnv.live >>> ParticleLoader.live) ++
        (CoreEnv.live >>> GLWindow.live)
  }
}
