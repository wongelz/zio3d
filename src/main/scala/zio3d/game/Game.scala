package zio3d.game

import zio.ZIO
import zio3d.core.glfw.WindowSize
import zio3d.engine.UserInput
import zio3d.engine.loaders.LoadingError

/**
 * Template for a 3D rendered application or game.
 *
 * @tparam R renderer.
 * @tparam S state.
 */
trait Game[R, S] {

  type Continue = Boolean

  /**
   * Initialise the renderer. The renderer comprises all shaders used for loading and rendering models
   * in the app.
   * @return renderer.
   */
  def initRenderer: ZIO[GameEnv, LoadingError, R]

  /**
   * Create the initial app state.
   * Models, textures and other items that are part of the app state can be loaded here.
   * @param renderer may be used for loading.
   * @return initial state.
   */
  def initialState(renderer: R): ZIO[GameEnv, LoadingError, S]

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
  ): ZIO[GameEnv, Nothing, Unit]

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
  ): ZIO[GameEnv, Nothing, (S, Continue)]

  def cleanup(renderer: R, state: S): ZIO[GameEnv, Nothing, Unit]
}
