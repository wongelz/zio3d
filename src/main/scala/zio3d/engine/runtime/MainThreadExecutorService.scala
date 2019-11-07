package zio3d.engine.runtime

import java.util
import java.util.concurrent.{AbstractExecutorService, LinkedBlockingQueue}

class MainThreadExecutorService extends AbstractExecutorService {

  import java.util.concurrent.TimeUnit

  @volatile private var terminated = false

  private val workQueue = new LinkedBlockingQueue[Runnable]()

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

  def run() =
    while (!terminated) {
      val work = workQueue.poll()
      if (work != null) {
        work.run()
      }
    }
}
