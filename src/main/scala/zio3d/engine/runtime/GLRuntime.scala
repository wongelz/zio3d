package zio3d.engine.runtime

import java.util
import java.util.concurrent.{AbstractExecutorService, LinkedBlockingQueue, TimeUnit}

import zio.blocking.Blocking
import zio.clock.Clock
import zio.internal.{Executor, Platform, PlatformLive}
import zio.random.Random
import zio.{internal, Exit, FiberFailure, IO, Runtime, ZIO}
import zio3d.core.buffers.Buffers
import zio3d.core.gl.GL
import zio3d.core.glfw.GLFW
import zio3d.engine.RenderEnv
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

import scala.concurrent.ExecutionContext

/**
 * The entry-point for a LWJGL application running on ZIO.
 */
trait GLRuntime extends Runtime[RenderEnv] {
  val Platform: Platform = PlatformLive.Default

  override val Environment: RenderEnv = new Clock.Live with Buffers.Live with GL.Live with GLFW.Live
  with SimpleShaderInterpreter.Live with SkyboxShaderInterpreter.Live with SceneShaderInterpreter.Live
  with ParticleShaderInterpreter with TerrainLoader.Live with HudRenderer.Live with StaticMeshLoader.Live
  with AnimMeshLoader.Live with Blocking.Live with Random.Live with ParticleLoader.Live with GLWindow.Live

  def run(args: List[String]): ZIO[RenderEnv, Nothing, Int]

  /**
   * The Scala main function, intended to be called only by the Scala runtime.
   */
  final def main(args0: Array[String]): Unit =
    sys.exit(
      unsafeRunContinueMain(
        for {
          fiber <- run(args0.toList).fork
          _ <- IO.effectTotal(java.lang.Runtime.getRuntime.addShutdownHook(new Thread {
                override def run(): Unit = {
                  val _ = unsafeRunSync(fiber.interrupt)
                }
              }))
          result <- fiber.join
        } yield result
      ).getOrElse(c => throw FiberFailure(c))
    )

  /**
   * Executes the effect while the main thread works through any work directed to it.
   * In particular, some GLFW methods need to be executed from the main thread.
   */
  final def unsafeRunContinueMain[E, A](zio: ZIO[RenderEnv, E, A]): Exit[E, A] = {
    val result = internal.OneShot.make[Exit[E, A]]

    unsafeRunAsync(zio) { x: Exit[E, A] =>
      GLRuntime.MainExecutorService.shutdown()
      result.set(x)
    }

    GLRuntime.MainExecutorService.run()
    result.get()
  }
}

object GLRuntime {

  private[runtime] object MainExecutorService extends AbstractExecutorService {

    private val workQueue = new LinkedBlockingQueue[Runnable]()

    @volatile private var terminated = false

    def shutdown(): Unit =
      terminated = true

    def isShutdown: Boolean =
      terminated

    def isTerminated: Boolean =
      terminated

    @throws[InterruptedException]
    def awaitTermination(theTimeout: Long, theUnit: TimeUnit): Boolean = {
      shutdown()
      terminated
    }

    def shutdownNow: util.List[Runnable] = new util.ArrayList[Runnable]()

    def execute(theCommand: Runnable): Unit =
      workQueue.put(theCommand)

    /**
     * Must be called synchronously from the main thread.
     */
    private[runtime] def run(): Unit =
      while (!terminated) {
        val work = workQueue.poll()
        if (work != null) {
          work.run()
        }
      }
  }

  val mainThread: Executor =
    Executor.fromExecutionContext(10)(ExecutionContext.fromExecutorService(MainExecutorService))
}
