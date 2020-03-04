package zio3d.core

import java.io.PrintStream

import org.lwjgl.glfw.Callbacks.glfwFreeCallbacks
import org.lwjgl.glfw.GLFW._
import org.lwjgl.glfw.{GLFWErrorCallback, GLFWKeyCallback}
import zio._
import zio3d.core.buffers.Buffers

package object glfw {

  type GLFW = Has[GLFW.Service]

  object GLFW extends Serializable {

    trait Service {
      def setErrorPrintStream(printStream: PrintStream): UIO[Unit]

      def setWindowTitle(window: Window, title: String): UIO[Unit]

      def init: UIO[Boolean]

      def defaultWindowHints: UIO[Unit]

      def windowHint(hint: Int, value: Int): UIO[Unit]

      def createWindow(width: Int, height: Int, title: String, monitor: Long, share: Long): UIO[Window]

      def maximizeWindow(window: Window): UIO[Unit]

      def setKeyCallback(window: Window, callback: (Window, Int, Int, Int, Int) => Unit): UIO[GLFWKeyCallback]

      def freeCallbacks(window: Window): UIO[Unit]

      def destroyWindow(window: Window): UIO[Unit]

      def terminate: UIO[Unit]

      def setWindowShouldClose(window: Window, value: Boolean): UIO[Unit]

      def windowShouldClose(window: Window): UIO[Boolean]

      def makeContextCurrent(window: Window): UIO[Unit]

      def swapInterval(interval: Int): UIO[Unit]

      def showWindow(window: Window): UIO[Unit]

      def focusWindow(window: Window): UIO[Unit]

      def swapBuffers(window: Window): UIO[Unit]

      def pollEvents: UIO[Unit]

      def getKey(window: Window, key: Int): UIO[Int]

      def getMouseButton(window: Window, button: Int): UIO[Int]

      def setInputMode(window: Window, mode: Int, value: Int): UIO[Unit]

      def getCursorPos(window: Window): UIO[CursorPos]

      def setCursorPos(window: Window, xpos: Double, ypos: Double): UIO[Unit]

      def createStandardCursor(window: Window, shape: Int): UIO[Cursor]

      def setCursor(window: Window, cursor: Cursor): UIO[Unit]

      def getWindowSize(window: Window): UIO[WindowSize]
    }

    val live = ZLayer.fromService[Buffers.Service, GLFW.Service] { buffers =>
      new Service {

        def setErrorPrintStream(printStream: PrintStream): UIO[Unit] =
          IO.effectTotal {
            GLFWErrorCallback.createPrint(printStream).set;
            ()
          }

        def setWindowTitle(window: Window, title: String): UIO[Unit] =
          IO.effectTotal {
            glfwSetWindowTitle(window.value, title)
          }

        def init: UIO[Boolean] =
          IO.effectTotal {
            glfwInit()
          }

        def defaultWindowHints: UIO[Unit] =
          IO.effectTotal {
            glfwDefaultWindowHints()
          }

        def windowHint(hint: Int, value: Int): UIO[Unit] =
          IO.effectTotal {
            glfwWindowHint(hint, value)
          }

        def createWindow(width: Int, height: Int, title: String, monitor: Long, share: Long): UIO[Window] =
          IO.effectTotal {
            Window(glfwCreateWindow(width, height, title, monitor, share))
          }

        def maximizeWindow(window: Window): UIO[Unit] =
          IO.effectTotal {
            glfwMaximizeWindow(window.value)
          }

        def setKeyCallback(window: Window, callback: (Window, Int, Int, Int, Int) => Unit): UIO[GLFWKeyCallback] =
          IO.effectTotal {
            glfwSetKeyCallback(
              window.value,
              (window: Long, key: Int, scancode: Int, action: Int, mods: Int) =>
                callback(Window(window), key, scancode, action, mods)
            )
          }

        def freeCallbacks(window: Window): UIO[Unit] =
          IO.effectTotal {
            glfwFreeCallbacks(window.value)
          }

        def destroyWindow(window: Window): UIO[Unit] =
          IO.effectTotal {
            glfwDestroyWindow(window.value)
          }

        def terminate: UIO[Unit] =
          IO.effectTotal {
            glfwTerminate()
          }

        def setWindowShouldClose(window: Window, value: Boolean): UIO[Unit] =
          IO.effectTotal {
            glfwSetWindowShouldClose(window.value, value)
          }

        def windowShouldClose(window: Window): UIO[Boolean] =
          IO.effectTotal {
            glfwWindowShouldClose(window.value)
          }

        def makeContextCurrent(window: Window): UIO[Unit] =
          IO.effectTotal {
            glfwMakeContextCurrent(window.value)
          }

        def swapInterval(interval: Int): UIO[Unit] =
          IO.effectTotal {
            glfwSwapInterval(interval)
          }

        def showWindow(window: Window): UIO[Unit] =
          IO.effectTotal {
            glfwShowWindow(window.value)
          }

        def focusWindow(window: Window): UIO[Unit] =
          IO.effectTotal {
            glfwFocusWindow(window.value)
          }

        def swapBuffers(window: Window): UIO[Unit] =
          IO.effectTotal {
            glfwSwapBuffers(window.value)
          }

        def pollEvents: UIO[Unit] =
          IO.effectTotal {
            glfwPollEvents()
          }

        def getKey(window: Window, key: Int): UIO[Int] =
          IO.effectTotal {
            glfwGetKey(window.value, key)
          }

        def getMouseButton(window: Window, button: Int): UIO[Int] =
          IO.effectTotal {
            glfwGetMouseButton(window.value, button)
          }

        def setInputMode(window: Window, mode: Int, value: Int): UIO[Unit] =
          IO.effectTotal {
            glfwSetInputMode(window.value, mode, value)
          }

        def getCursorPos(window: Window): UIO[CursorPos] =
          for {
            x <- buffers.doubleBuffer(1)
            y <- buffers.doubleBuffer(1)
            _ <- IO.effectTotal {
              glfwGetCursorPos(window.value, x, y)
            }
          } yield CursorPos(x.get(), y.get())

        def setCursorPos(window: Window, xpos: Double, ypos: Double): UIO[Unit] =
          IO.effectTotal {
            glfwSetCursorPos(window.value, xpos, ypos)
          }

        def createStandardCursor(window: Window, shape: Int): UIO[Cursor] =
          IO.effectTotal {
            Cursor(glfwCreateStandardCursor(shape))
          }

        def setCursor(window: Window, cursor: Cursor): UIO[Unit] =
          IO.effectTotal {
            glfwSetCursor(window.value, cursor.value)
          }

        def getWindowSize(window: Window): UIO[WindowSize] =
          for {
            w <- buffers.intBuffer(1)
            h <- buffers.intBuffer(1)
            _ <- IO.effectTotal {
              glfwGetWindowSize(window.value, w, h)
            }
          } yield WindowSize(w.get(), h.get())
      }
    }
  }

  final val glfwService: ZIO[GLFW, Nothing, GLFW.Service] =
    ZIO.access(_.get)

  final def setErrorPrintStream(printStream: PrintStream): ZIO[GLFW, Nothing, Unit] =
    ZIO.accessM(_.get setErrorPrintStream printStream)

  final def setWindowTitle(window: Window, title: String): ZIO[GLFW, Nothing, Unit] =
    ZIO.accessM(_.get.setWindowTitle(window, title))

  final val init: ZIO[GLFW, Nothing, Boolean] =
    ZIO.accessM(_.get.init)

  final val defaultWindowHints: ZIO[GLFW, Nothing, Unit] =
    ZIO.accessM(_.get.defaultWindowHints)

  final def windowHint(hint: Int, value: Int): ZIO[GLFW, Nothing, Unit] =
    ZIO.accessM(_.get.windowHint(hint, value))

  final def createWindow(
    width: Int,
    height: Int,
    title: String,
    monitor: Long,
    share: Long
  ): ZIO[GLFW, Nothing, Window] =
    ZIO.accessM(_.get.createWindow(width, height, title, monitor, share))

  final def maximizeWindow(window: Window): ZIO[GLFW, Nothing, Unit] =
    ZIO.accessM(_.get maximizeWindow window)

  final def setKeyCallback(
    window: Window,
    callback: (Window, Int, Int, Int, Int) => Unit
  ): ZIO[GLFW, Nothing, GLFWKeyCallback] =
    ZIO.accessM(_.get.setKeyCallback(window, callback))

  final def freeCallbacks(window: Window): ZIO[GLFW, Nothing, Unit] =
    ZIO.accessM(_.get freeCallbacks window)

  final def destroyWindow(window: Window): ZIO[GLFW, Nothing, Unit] =
    ZIO.accessM(_.get destroyWindow window)

  final val terminate: ZIO[GLFW, Nothing, Unit] =
    ZIO.accessM(_.get.terminate)

  final def setWindowShouldClose(window: Window, value: Boolean): ZIO[GLFW, Nothing, Unit] =
    ZIO.accessM(_.get.setWindowShouldClose(window, value))

  final def windowShouldClose(window: Window): ZIO[GLFW, Nothing, Boolean] =
    ZIO.accessM(_.get windowShouldClose window)

  final def makeContextCurrent(window: Window): ZIO[GLFW, Nothing, Unit] =
    ZIO.accessM(_.get makeContextCurrent window)

  final def swapInterval(interval: Int): ZIO[GLFW, Nothing, Unit] =
    ZIO.accessM(_.get swapInterval interval)

  final def showWindow(window: Window): ZIO[GLFW, Nothing, Unit] =
    ZIO.accessM(_.get showWindow window)

  final def swapBuffers(window: Window): ZIO[GLFW, Nothing, Unit] =
    ZIO.accessM(_.get swapBuffers window)

  final val pollEvents: ZIO[GLFW, Nothing, Unit] =
    ZIO.accessM(_.get.pollEvents)

  final def getKey(window: Window, key: Int): ZIO[GLFW, Nothing, Int] =
    ZIO.accessM(_.get.getKey(window, key))

  final def getMouseButton(window: Window, button: Int): ZIO[GLFW, Nothing, Int] =
    ZIO.accessM(_.get.getMouseButton(window, button))

  final def setInputMode(window: Window, mode: Int, value: Int): ZIO[GLFW, Nothing, Unit] =
    ZIO.accessM(_.get.setInputMode(window, mode, value))

  final def getCursorPos(window: Window): ZIO[GLFW with Buffers, Nothing, CursorPos] =
    ZIO.accessM(_.get[GLFW.Service].getCursorPos(window))

  final def setCursorPos(window: Window, xpos: Double, ypos: Double): ZIO[GLFW, Nothing, Unit] =
    ZIO.accessM(_.get.setCursorPos(window, xpos, ypos))

  final def createStandardCursor(window: Window, shape: Int): ZIO[GLFW, Nothing, Cursor] =
    ZIO.accessM(_.get.createStandardCursor(window, shape))

  final def setCursor(window: Window, cursor: Cursor): ZIO[GLFW, Nothing, Unit] =
    ZIO.accessM(_.get.setCursor(window, cursor))

  final def getWindowSize(window: Window): ZIO[GLFW with Buffers, Nothing, WindowSize] =
    ZIO.accessM(_.get[GLFW.Service].getWindowSize(window))
}
