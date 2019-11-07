package zio3d.engine.shaders

import zio.IO
import zio3d.core.buffers.Buffers
import zio3d.core.gl.GL
import zio3d.engine.loaders.LoadingError
import zio3d.engine._

/**
 * ShaderInterpreter contains the logic for a single shader program, typically consisting
 * of a vertex shader and a fragment shader.
 * @tparam M mesh type.
 * @tparam P shader program.
 */
trait ShaderInterpreter[M, P] extends GL with Buffers {
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
     * @param items to render.
     * @param transformation to apply.
     * @param fixtures environment fixtures (light, fog, etc).
     * @return .
     */
    def render(
      program: P,
      items: Iterable[GameItem],
      transformation: Transformation,
      fixtures: Fixtures
    ): IO[Nothing, Unit]
  }
}
