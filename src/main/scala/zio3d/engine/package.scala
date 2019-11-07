package zio3d

import zio.blocking.Blocking
import zio.clock.Clock
import zio.random.Random
import zio3d.core.buffers.Buffers
import zio3d.core.gl.GL
import zio3d.core.glfw.GLFW
import zio3d.engine.glwindow.GLWindow
import zio3d.engine.loaders.assimp.StaticMeshLoader
import zio3d.engine.loaders.assimp.anim.AnimMeshLoader
import zio3d.engine.loaders.particles.ParticleLoader
import zio3d.engine.loaders.terrain.TerrainLoader
import zio3d.engine.shaders.particle.ParticleShaderInterpreter
import zio3d.engine.shaders.scene.SceneShaderInterpreter
import zio3d.engine.shaders.simple.SimpleShaderInterpreter
import zio3d.engine.shaders.skybox.SkyboxShaderInterpreter
import zio3d.game.hud.HudRenderer

package object engine {

  type RenderEnv = Clock
    with Buffers
    with GL
    with GLFW
    with SimpleShaderInterpreter
    with SkyboxShaderInterpreter
    with SceneShaderInterpreter
    with ParticleShaderInterpreter
    with TerrainLoader
    with HudRenderer
    with StaticMeshLoader
    with AnimMeshLoader
    with Blocking
    with Random
    with ParticleLoader
    with GLWindow
}
