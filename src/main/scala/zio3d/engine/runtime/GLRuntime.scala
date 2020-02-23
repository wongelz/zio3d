package zio3d.engine.runtime

import java.util
import java.util.concurrent.{AbstractExecutorService, LinkedBlockingQueue, TimeUnit}

import zio.internal.{Executor, Platform}
import zio.{Exit, FiberFailure, IO, Runtime, ZIO, internal}

import scala.concurrent.ExecutionContext

/**
 * The entry-point for a LWJGL application running on ZIO.
 */
trait GLRuntime[R] extends Runtime[R] {
  override val platform: Platform = Platform.default

  def run(args: List[String]): ZIO[R, Nothing, Int]

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
  final def unsafeRunContinueMain[E, A](zio: ZIO[R, E, A]): Exit[E, A] = {
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
        val work = workQueue.poll(1, TimeUnit.MILLISECONDS)
        if (work != null) {
          work.run()
        }
      }
  }

  val mainThread: Executor =
    Executor.fromExecutionContext(10)(ExecutionContext.fromExecutorService(MainExecutorService))
}
