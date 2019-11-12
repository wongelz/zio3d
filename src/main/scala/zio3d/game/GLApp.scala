package zio3d.game

import zio.ZIO
import zio3d.core.glfw.WindowSize
import zio3d.engine.loaders.LoadingError
import zio3d.engine.{RenderEnv, UserInput}

/**
 * Template for a 3D rendered application or game.
 *
 * @tparam R renderer.
 * @tparam S state.
 */
trait GLApp[R, S] {

  type Continue = Boolean

  /**
   * Initialise the renderer. The renderer comprises all shaders used for loading and rendering models
   * in the app.
   * @return renderer.
   */
  def initRenderer: ZIO[RenderEnv, LoadingError, R]

  /**
   * Create the initial app state.
   * Models, textures and other items that are part of the app state can be loaded here.
   * @param renderer may be used for loading.
   * @return initial state.
   */
  def initialState(renderer: R): ZIO[RenderEnv, LoadingError, S]

  /**
   * Render the current game state into a single frame.
   * @param windowSize window size.
   * @param renderer renderer.
   * @param state current state, to render.
   */
  def render(
    windowSize: WindowSize,
    renderer: R,
    state: S
  ): ZIO[RenderEnv, Nothing, Unit]

  /**
   * Advance the game state.
   * @param state current state, to render.
   * @param userInput user input.
   * @param currentTime millis since epoch.
   * @return next state, and whether to continue the application.
   */
  def nextState(
    state: S,
    userInput: UserInput,
    currentTime: Long
  ): ZIO[RenderEnv, Nothing, (S, Continue)]

  def cleanup(renderer: R): ZIO[RenderEnv, Nothing, Unit]
}
