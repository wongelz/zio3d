package zio3d.core

import java.io.PrintStream

import org.lwjgl.glfw.GLFWKeyCallback
import zio.ZIO

package object glfw {

  final val glfwService: ZIO[GLFW, Nothing, GLFW.Service] =
    ZIO.access(_.glfw)

  final def setErrorPrintStream(printStream: PrintStream): ZIO[GLFW, Nothing, Unit] =
    ZIO.accessM(_.glfw setErrorPrintStream printStream)

  final def setWindowTitle(window: Window, title: String): ZIO[GLFW, Nothing, Unit] =
    ZIO.accessM(_.glfw.setWindowTitle(window, title))

  final val init: ZIO[GLFW, Nothing, Boolean] =
    ZIO.accessM(_.glfw.init)

  final val defaultWindowHints: ZIO[GLFW, Nothing, Unit] =
    ZIO.accessM(_.glfw.defaultWindowHints)

  final def windowHint(hint: Int, value: Int): ZIO[GLFW, Nothing, Unit] =
    ZIO.accessM(_.glfw.windowHint(hint, value))

  final def createWindow(
    width: Int,
    height: Int,
    title: String,
    monitor: Long,
    share: Long
  ): ZIO[GLFW, Nothing, Window] =
    ZIO.accessM(_.glfw.createWindow(width, height, title, monitor, share))

  final def maximizeWindow(window: Window): ZIO[GLFW, Nothing, Unit] =
    ZIO.accessM(_.glfw maximizeWindow window)

  final def setKeyCallback(
    window: Window,
    callback: (Window, Int, Int, Int, Int) => Unit
  ): ZIO[GLFW, Nothing, GLFWKeyCallback] =
    ZIO.accessM(_.glfw.setKeyCallback(window, callback))

  final def freeCallbacks(window: Window): ZIO[GLFW, Nothing, Unit] =
    ZIO.accessM(_.glfw freeCallbacks window)

  final def destroyWindow(window: Window): ZIO[GLFW, Nothing, Unit] =
    ZIO.accessM(_.glfw destroyWindow window)

  final val terminate: ZIO[GLFW, Nothing, Unit] =
    ZIO.accessM(_.glfw.terminate)

  final def setWindowShouldClose(window: Window, value: Boolean): ZIO[GLFW, Nothing, Unit] =
    ZIO.accessM(_.glfw.setWindowShouldClose(window, value))

  final def windowShouldClose(window: Window): ZIO[GLFW, Nothing, Boolean] =
    ZIO.accessM(_.glfw windowShouldClose window)

  final def makeContextCurrent(window: Window): ZIO[GLFW, Nothing, Unit] =
    ZIO.accessM(_.glfw makeContextCurrent window)

  final def swapInterval(interval: Int): ZIO[GLFW, Nothing, Unit] =
    ZIO.accessM(_.glfw swapInterval interval)

  final def showWindow(window: Window): ZIO[GLFW, Nothing, Unit] =
    ZIO.accessM(_.glfw showWindow window)

  final def swapBuffers(window: Window): ZIO[GLFW, Nothing, Unit] =
    ZIO.accessM(_.glfw swapBuffers window)

  final val pollEvents: ZIO[GLFW, Nothing, Unit] =
    ZIO.accessM(_.glfw.pollEvents)

  final def getKey(window: Window, key: Int): ZIO[GLFW, Nothing, Int] =
    ZIO.accessM(_.glfw.getKey(window, key))

  final def getMouseButton(window: Window, button: Int): ZIO[GLFW, Nothing, Int] =
    ZIO.accessM(_.glfw.getMouseButton(window, button))

  final def setInputMode(window: Window, mode: Int, value: Int): ZIO[GLFW, Nothing, Unit] =
    ZIO.accessM(_.glfw.setInputMode(window, mode, value))

  final def getCursorPos(window: Window): ZIO[GLFW, Nothing, CursorPos] =
    ZIO.accessM(_.glfw getCursorPos window)

  final def setCursorPos(window: Window, xpos: Double, ypos: Double): ZIO[GLFW, Nothing, Unit] =
    ZIO.accessM(_.glfw.setCursorPos(window, xpos, ypos))

  final def createStandardCursor(window: Window, shape: Int): ZIO[GLFW, Nothing, Cursor] =
    ZIO.accessM(_.glfw.createStandardCursor(window, shape))

  final def setCursor(window: Window, cursor: Cursor): ZIO[GLFW, Nothing, Unit] =
    ZIO.accessM(_.glfw.setCursor(window, cursor))

  final def getWindowSize(window: Window): ZIO[GLFW, Nothing, WindowSize] =
    ZIO.accessM(_.glfw getWindowSize window)
}
