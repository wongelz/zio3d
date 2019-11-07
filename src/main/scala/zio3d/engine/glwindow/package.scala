package zio3d.engine

import zio.ZIO
import zio3d.core.glfw.Window

package object glwindow {
  def open(title: String, width: Int, height: Int): ZIO[GLWindow, Nothing, Window] =
    ZIO.accessM(_.window.open(title, width, height))

  def close(w: Window): ZIO[GLWindow, Nothing, Unit] =
    ZIO.accessM(_.window.close(w))

  def getUserInput(window: Window, prevInput: Option[UserInput]): ZIO[GLWindow, Nothing, UserInput] =
    ZIO.accessM(_.window.getUserInput(window, prevInput))
}
