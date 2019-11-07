package zio3d.core.glfw

import java.io.PrintStream

import org.lwjgl.glfw.Callbacks.glfwFreeCallbacks
import org.lwjgl.glfw.GLFW._
import org.lwjgl.glfw.{GLFWErrorCallback, GLFWKeyCallback}
import zio.{IO, UIO}
import zio3d.core.buffers.Buffers

final case class Window(value: Long) extends AnyVal

final case class WindowSize(width: Int, height: Int)

final case class CursorPos(x: Double, y: Double)

final case class Cursor(value: Long) extends AnyVal

trait GLFW {
  def glfw: GLFW.Service
}

object GLFW {

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

  trait Live extends Buffers.Live with GLFW {

    val glfw: Service = new Service {
      def setErrorPrintStream(printStream: PrintStream): UIO[Unit] =
        IO.effectTotal {
          GLFWErrorCallback.createPrint(printStream).set; ()
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
  object Live extends Live
}
