package zio3d.engine

import org.lwjgl.glfw.GLFW._
import org.lwjgl.system.MemoryUtil.NULL
import zio.{Has, UIO, ZIO, ZLayer}
import zio3d.core.glfw.{GLFW, Window}

package object glwindow {

  type GLWindow = Has[GLWindow.Service]

  object GLWindow extends Serializable {
    trait Service {
      def open(title: String, width: Int, height: Int): UIO[Window]

      def close(w: Window): UIO[Unit]

      def getUserInput(w: Window, prevInput: Option[UserInput]): UIO[UserInput]
    }

    val live = ZLayer.fromService[GLFW.Service, GLWindow.Service] { glfw =>
      new Service {

        def open(title: String, width: Int, height: Int) =
          for {
            _ <- initFramework
            w <- glfw.createWindow(width, height, title, NULL, NULL)
            _ <- glfw.makeContextCurrent(w)
            _ <- glfw.swapInterval(1)
            _ <- glfw.maximizeWindow(w)
            _ <- glfw.setInputMode(w, GLFW_CURSOR, GLFW_CURSOR_DISABLED)
            _ <- glfw.focusWindow(w)
            _ <- glfw.showWindow(w)
          } yield w

        private def initFramework: UIO[Unit] =
          glfw.setErrorPrintStream(System.err) *>
            glfw.init *>
            glfw.defaultWindowHints *>
            glfw.windowHint(GLFW_VISIBLE, GLFW_FALSE) *>
            glfw.windowHint(GLFW_SAMPLES, 4) *>
            glfw.windowHint(GLFW_CONTEXT_VERSION_MAJOR, 3) *>
            glfw.windowHint(GLFW_CONTEXT_VERSION_MINOR, 2) *>
            glfw.windowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE) *>
            glfw.windowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE) *>
            glfw.windowHint(GLFW_RESIZABLE, GLFW_TRUE) *>
            glfw.windowHint(GLFW_FOCUSED, GLFW_TRUE)

        def close(w: Window) =
          glfw.freeCallbacks(w) *>
            glfw.destroyWindow(w) *>
            glfw.terminate

        def getUserInput(
          w: Window,
          prevInput: Option[UserInput]
        ): UIO[UserInput] =
          for {
            keys <- getKeys(w)
            btns <- getMouseButtons(w)
            curs <- glfw.getCursorPos(w)
          } yield UserInput(keys, btns, prevInput.fold(curs)(_.currCursor), curs)

        private def getKeys(w: Window) =
          ZIO
            .foreach(Key.values) { k =>
              glfw.getKey(w, k.value) map { e =>
                if (e == 1) Some(k) else None
              }
            }
            .map(_.toSet.flatten)

        private def getMouseButtons(w: Window) =
          ZIO
            .foreach(MouseButton.values) { m =>
              glfw.getMouseButton(w, m.value) map { e =>
                if (e == GLFW_PRESS) Some(m) else None
              }
            }
            .map(_.toSet.flatten)
      }
    }
  }

  def open(title: String, width: Int, height: Int): ZIO[GLWindow, Nothing, Window] =
    ZIO.accessM(_.get.open(title, width, height))

  def close(w: Window): ZIO[GLWindow, Nothing, Unit] =
    ZIO.accessM(_.get.close(w))

  def getUserInput(window: Window, prevInput: Option[UserInput]): ZIO[GLWindow, Nothing, UserInput] =
    ZIO.accessM(_.get.getUserInput(window, prevInput))
}
