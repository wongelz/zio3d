package zio3d.engine.shaders

import zio.{IO, UIO}
import zio3d.engine._
import zio3d.engine.loaders.LoadingError

/**
 * ShaderInterpreter contains the logic for a single shader program, typically consisting
 * of a vertex shader and a fragment shader.
 * @tparam M mesh type.
 * @tparam P shader program.
 */
trait ShaderInterpreter[M, P] {
  def shaderService: ShaderInterpreter.Service[M, P]
}

object ShaderInterpreter {

  /**
   * @tparam M mesh input.
   * @tparam P shader program.
   */
  trait Service[M, P] {

    /**
     * Create a new shader program, compiling the required shaders and linking to the program.
     * @return program.
     */
    def loadShaderProgram: IO[LoadingError, P]

    /**
     * Use the shader program defined by this pipeline to load a mesh for the given mesh definition/input.
     * @param program shader program.
     * @param input mesh definition.
     * @return mesh.
     */
    def loadMesh(program: P, input: M): IO[LoadingError, Mesh]

    /**
     * Use the shader program defined by this pipeline to render the given items.
     *
     * @param program        shader program.
     * @param item to render.
     * @param transformation to apply.
     * @param fixtures environment fixtures (light, fog, etc).
     * @return .
     */
    def render(
      program: P,
      item: GameItem,
      transformation: Transformation,
      fixtures: Fixtures
    ): UIO[Unit]

    def cleanup(
      program: P
    ): UIO[Unit]
  }
}
