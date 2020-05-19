package zio3d.engine.runtime

import java.util
import java.util.concurrent.{AbstractExecutorService, LinkedBlockingQueue, TimeUnit}

import zio.internal.Executor
import zio.{Exit, FiberFailure, IO, Runtime, ZIO, internal}

import scala.concurrent.ExecutionContext

/**
 * The entry-point for a ZIO application that must run certain effects on the main thread.
 */
trait MainThreadApp {
  private lazy val runtime = Runtime.default

  def run(args: List[String]): ZIO[Any, Nothing, Int]

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
                  val _ = runtime.unsafeRun(fiber.interrupt)
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
  final def unsafeRunContinueMain[E, A](zio: ZIO[Any, E, A]): Exit[E, A] = {
    val result = internal.OneShot.make[Exit[E, A]]

    runtime.unsafeRunAsync(zio) { x: Exit[E, A] =>
      MainThreadApp.MainExecutorService.shutdown()
      result.set(x)
    }

    MainThreadApp.MainExecutorService.run()
    result.get()
  }
}

object MainThreadApp {

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

  /**
   * The main thread is exposed here as an Executor to be used via [[ZIO.lock()]].
   */
  val mainThread: Executor =
    Executor.fromExecutionContext(10)(ExecutionContext.fromExecutorService(MainExecutorService))
}
