package zio3d.engine.loaders

/**
 * An error may only occur during the loading of the game.
 */
sealed trait LoadingError

object LoadingError {
  case class ShaderCompileError(message: String)              extends LoadingError
  case class ProgramLinkError(message: String)                extends LoadingError
  case class FileLoadError(filename: String, message: String) extends LoadingError
}
