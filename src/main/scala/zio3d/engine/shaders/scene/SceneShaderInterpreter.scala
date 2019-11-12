package zio3d.engine.shaders.scene

import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL13._
import org.lwjgl.opengl.GL15._
import org.lwjgl.opengl.GL20
import zio.{IO, UIO, ZIO}
import zio3d.core.buffers.Buffers
import zio3d.core.gl.GL
import zio3d.core.gl.GL.{AttribLocation, Program, UniformLocation, _}
import zio3d.engine._
import zio3d.engine.loaders.LoadingError.{ProgramLinkError, ShaderCompileError}
import zio3d.engine.loaders.texture._
import zio3d.engine.shaders.ShaderInterpreter

final case class SceneShaderProgram(
  program: Program,
  uniModelViewMatrix: UniformLocation,
  uniProjectionMatrix: UniformLocation,
  uniJointsMatrix: UniformLocation,
  uniTextureSampler: UniformLocation,
  uniMaterial: MaterialUniform,
  uniSpecularPower: UniformLocation,
  uniAmbientLight: UniformLocation,
  uniPointLight: List[PointLightUniform],
  uniSpotLight: List[SpotLightUniform],
  uniFog: FogUniform,
  positionAttr: AttribLocation,
  texCoordAttr: AttribLocation,
  normalsAttr: AttribLocation,
  jointWeightsAttr: AttribLocation,
  jointIndicesAttr: AttribLocation
)

final case class MaterialUniform(
  ambient: UniformLocation,
  diffuse: UniformLocation,
  specular: UniformLocation,
  hasTexture: UniformLocation,
  reflectance: UniformLocation
)

final case class PointLightUniform(
  colour: UniformLocation,
  position: UniformLocation,
  intensity: UniformLocation,
  attConstant: UniformLocation,
  attLinear: UniformLocation,
  attExponent: UniformLocation
)

final case class SpotLightUniform(
  pl: PointLightUniform,
  conedir: UniformLocation,
  cutoff: UniformLocation
)

final case class FogUniform(
  active: UniformLocation,
  colour: UniformLocation,
  density: UniformLocation
)

/**
 * Shader pipeline for rendering more complex scene objects, with joints and animations.
 */
trait SceneShaderInterpreter {
  def sceneShaderInterpreter: ShaderInterpreter.Service[MeshDefinition, SceneShaderProgram]
}

object SceneShaderInterpreter {

  final val MaxPointLights = 5
  final val MaxSpotLights  = 5

  private val strVertexShader =
    """#version 330
      |
      |const int MAX_WEIGHTS = 4;
      |const int MAX_JOINTS = 150;
      |
      |layout (location=0) in vec3 position;
      |layout (location=1) in vec2 texCoord;
      |layout (location=2) in vec3 vertexNormal;
      |layout (location=3) in vec4 jointWeights;
      |layout (location=4) in ivec4 jointIndices;
      |
      |out vec2 outTexCoord;
      |out vec3 mvVertexNormal;
      |out vec3 mvVertexPos;
      |
      |uniform mat4 jointsMatrix[MAX_JOINTS];
      |uniform mat4 modelViewMatrix;
      |uniform mat4 projectionMatrix;
      |
      |void main()
      |{
      |    vec4 initPos = vec4(0, 0, 0, 0);
      |    vec4 initNormal = vec4(0, 0, 0, 0);
      |    int count = 0;
      |    for(int i = 0; i < MAX_WEIGHTS; i++)
      |    {
      |        float weight = jointWeights[i];
      |        if(weight > 0) {
      |            count++;
      |            int jointIndex = jointIndices[i];
      |            vec4 tmpPos = jointsMatrix[jointIndex] * vec4(position, 1.0);
      |            initPos += weight * tmpPos;
      |
      |            vec4 tmpNormal = jointsMatrix[jointIndex] * vec4(vertexNormal, 0.0);
      |            initNormal += weight * tmpNormal;
      |        }
      |    }
      |    if (count == 0)
      |    {
      |        initPos = vec4(position, 1.0);
      |        initNormal = vec4(vertexNormal, 0.0);
      |    }
      |    vec4 mvPos = modelViewMatrix * initPos;
      |    gl_Position = projectionMatrix * mvPos;
      |    outTexCoord = texCoord;
      |    mvVertexNormal = normalize(modelViewMatrix * vec4(vertexNormal, 0.0)).xyz;
      |    mvVertexPos = mvPos.xyz;
      |}
    """.stripMargin

  private val strFragmentShader =
    """#version 330
      |
      |const int MAX_POINT_LIGHTS = 5;
      |const int MAX_SPOT_LIGHTS = 5;
      |
      |in vec2 outTexCoord;
      |in vec3 mvVertexNormal;
      |in vec3 mvVertexPos;
      |
      |out vec4 fragColor;
      |
      |struct Attenuation
      |{
      |    float constant;
      |    float linear;
      |    float exponent;
      |};
      |
      |struct PointLight
      |{
      |    vec3 colour;
      |    // Light position is assumed to be in view coordinates
      |    vec3 position;
      |    float intensity;
      |    Attenuation att;
      |};
      |
      |struct SpotLight
      |{
      |    PointLight pl;
      |    vec3 conedir;
      |    float cutoff;
      |};
      |
      |struct DirectionalLight
      |{
      |    vec3 colour;
      |    vec3 direction;
      |    float intensity;
      |};
      |
      |struct Material
      |{
      |    vec4 ambient;
      |    vec4 diffuse;
      |    vec4 specular;
      |    int hasTexture;
      |    float reflectance;
      |};
      |
      |struct Fog
      |{
      |    int activeFog;
      |    vec3 colour;
      |    float density;
      |};
      |
      |uniform sampler2D texture_sampler;
      |uniform vec3 ambientLight;
      |uniform float specularPower;
      |uniform Material material;
      |uniform PointLight pointLights[MAX_POINT_LIGHTS];
      |uniform SpotLight spotLights[MAX_SPOT_LIGHTS];
      |uniform DirectionalLight directionalLight;
      |uniform Fog fog;
      |
      |vec4 ambientC;
      |vec4 diffuseC;
      |vec4 speculrC;
      |
      |void setupColours(Material material, vec2 textCoord)
      |{
      |    if (material.hasTexture == 1)
      |    {
      |        ambientC = texture(texture_sampler, textCoord);
      |        diffuseC = ambientC;
      |        speculrC = ambientC;
      |    }
      |    else
      |    {
      |        ambientC = material.ambient;
      |        diffuseC = material.diffuse;
      |        speculrC = material.specular;
      |    }
      |}
      |
      |vec4 calcLightColour(vec3 light_colour, float light_intensity, vec3 position, vec3 to_light_dir, vec3 normal)
      |{
      |    vec4 diffuseColour = vec4(0, 0, 0, 0);
      |    vec4 specColour = vec4(0, 0, 0, 0);
      |
      |    // Diffuse Light
      |    float diffuseFactor = max(dot(normal, to_light_dir), 0.0);
      |    diffuseColour = diffuseC * vec4(light_colour, 1.0) * light_intensity * diffuseFactor;
      |
      |    // Specular Light
      |    vec3 camera_direction = normalize(-position);
      |    vec3 from_light_dir = -to_light_dir;
      |    vec3 reflected_light = normalize(reflect(from_light_dir , normal));
      |    float specularFactor = max( dot(camera_direction, reflected_light), 0.0);
      |    specularFactor = pow(specularFactor, specularPower);
      |    specColour = speculrC * light_intensity  * specularFactor * material.reflectance * vec4(light_colour, 1.0);
      |
      |    return (diffuseColour + specColour);
      |}
      |
      |vec4 calcPointLight(PointLight light, vec3 position, vec3 normal)
      |{
      |    vec3 light_direction = light.position - position;
      |    vec3 to_light_dir  = normalize(light_direction);
      |    vec4 light_colour = calcLightColour(light.colour, light.intensity, position, to_light_dir, normal);
      |
      |    // Apply Attenuation
      |    float distance = length(light_direction);
      |    float attenuationInv = light.att.constant + light.att.linear * distance +
      |        light.att.exponent * distance * distance;
      |    return light_colour / attenuationInv;
      |}
      |
      |vec4 calcSpotLight(SpotLight light, vec3 position, vec3 normal)
      |{
      |    vec3 light_direction = light.pl.position - position;
      |    vec3 to_light_dir  = normalize(light_direction);
      |    vec3 from_light_dir  = -to_light_dir;
      |    float spot_alfa = dot(from_light_dir, normalize(light.conedir));
      |
      |    vec4 colour = vec4(0, 0, 0, 0);
      |
      |    if ( spot_alfa > light.cutoff )
      |    {
      |        colour = calcPointLight(light.pl, position, normal);
      |        colour *= (1.0 - (1.0 - spot_alfa)/(1.0 - light.cutoff));
      |    }
      |    return colour;
      |}
      |
      |vec4 calcDirectionalLight(DirectionalLight light, vec3 position, vec3 normal)
      |{
      |    return calcLightColour(light.colour, light.intensity, position, normalize(light.direction), normal);
      |}
      |
      |vec4 calcFog(vec3 pos, vec4 colour, Fog fog, vec3 ambientLight, DirectionalLight dirLight)
      |{
      |    vec3 fogColor = fog.colour * (ambientLight + dirLight.colour * dirLight.intensity);
      |    float distance = length(pos);
      |    float fogFactor = 1.0 / exp( (distance * fog.density)* (distance * fog.density));
      |    fogFactor = clamp( fogFactor, 0.0, 1.0 );
      |
      |    vec3 resultColour = mix(fogColor, colour.xyz, fogFactor);
      |    return vec4(resultColour.xyz, colour.w);
      |}
      |
      |void main()
      |{
      |    setupColours(material, outTexCoord);
      |
      |    vec4 diffuseSpecularComp = calcDirectionalLight(directionalLight, mvVertexPos, mvVertexNormal);
      |
      |    for (int i=0; i<MAX_POINT_LIGHTS; i++)
      |    {
      |        if ( pointLights[i].intensity > 0 )
      |        {
      |            diffuseSpecularComp += calcPointLight(pointLights[i], mvVertexPos, mvVertexNormal);
      |        }
      |    }
      |
      |    for (int i=0; i<MAX_SPOT_LIGHTS; i++)
      |    {
      |        if ( spotLights[i].pl.intensity > 0 )
      |        {
      |            diffuseSpecularComp += calcSpotLight(spotLights[i], mvVertexPos, mvVertexNormal);
      |        }
      |    }
      |
      |    fragColor = ambientC * vec4(ambientLight, 1) + diffuseSpecularComp;
      |
      |    if ( fog.activeFog == 1 )
      |    {
      |        fragColor = calcFog(mvVertexPos, fragColor, fog, ambientLight, directionalLight);
      |    }
      |}
    """.stripMargin

  val SpecularPower = 10.0f

  trait Live extends GL.Live with TextureLoader.Live with Buffers.Live with SceneShaderInterpreter {
    val sceneShaderInterpreter = new ShaderInterpreter.Service[MeshDefinition, SceneShaderProgram] {

      def loadShaderProgram =
        for {
          vs <- gl.compileShader(GL20.GL_VERTEX_SHADER, strVertexShader).mapError(ShaderCompileError)
          fs <- gl.compileShader(GL20.GL_FRAGMENT_SHADER, strFragmentShader).mapError(ShaderCompileError)
          p  <- gl.createProgram
          _  <- gl.attachShader(p, vs)
          _  <- gl.attachShader(p, fs)
          _  <- gl.linkProgram(p)
          _  <- gl.useProgram(p)
          ls <- gl.getProgrami(p, GL20.GL_LINK_STATUS)
          _  <- gl.getProgramInfoLog(p).flatMap(log => IO.fail(ProgramLinkError(log))).when(ls == 0)

          uniProjectionMatrix <- gl.getUniformLocation(p, "projectionMatrix")
          uniModelViewMatrix  <- gl.getUniformLocation(p, "modelViewMatrix")
          uniJointsMatrix     <- gl.getUniformLocation(p, "jointsMatrix")
          uniTexSampler       <- gl.getUniformLocation(p, "textureSampler")

          uniMaterial    <- createMaterialUniform(p, "material")
          uniPointLights <- createPointLightUniforms(p, "pointLights", MaxPointLights)
          uniSpotLights  <- createSpotLightUniforms(p, "spotLights", MaxSpotLights)
          uniFog         <- createFogUniform(p, "fog")

          uniSpecularPower <- gl.getUniformLocation(p, "specularPower")
          uniAmbientLight  <- gl.getUniformLocation(p, "ambientLight")

          positionAttr     <- gl.getAttribLocation(p, "position")
          texCoordAttr     <- gl.getAttribLocation(p, "texCoord")
          normalAttr       <- gl.getAttribLocation(p, "vertexNormal")
          jointWeightsAttr <- gl.getAttribLocation(p, "jointWeights")
          jointIndicesAttr <- gl.getAttribLocation(p, "jointIndices")

        } yield SceneShaderProgram(
          p,
          uniModelViewMatrix,
          uniProjectionMatrix,
          uniJointsMatrix,
          uniTexSampler,
          uniMaterial,
          uniSpecularPower,
          uniAmbientLight,
          uniPointLights,
          uniSpotLights,
          uniFog,
          positionAttr,
          texCoordAttr,
          normalAttr,
          jointWeightsAttr,
          jointIndicesAttr
        )

      private def createMaterialUniform(p: Program, name: String) =
        for {
          a <- gl.getUniformLocation(p, s"$name.ambient")
          d <- gl.getUniformLocation(p, s"$name.diffuse")
          s <- gl.getUniformLocation(p, s"$name.specular")
          t <- gl.getUniformLocation(p, s"$name.hasTexture")
          r <- gl.getUniformLocation(p, s"$name.reflectance")
        } yield MaterialUniform(a, d, s, t, r)

      private def createPointLightUniforms(p: Program, name: String, n: Int) =
        IO.foreach(0 until n) { i =>
          createPointLightUniform(p, s"$name[$i]")
        }

      private def createPointLightUniform(p: Program, name: String) =
        for {
          co <- gl.getUniformLocation(p, s"$name.colour")
          ps <- gl.getUniformLocation(p, s"$name.position")
          in <- gl.getUniformLocation(p, s"$name.intensity")
          ac <- gl.getUniformLocation(p, s"$name.att.constant")
          al <- gl.getUniformLocation(p, s"$name.att.linear")
          ae <- gl.getUniformLocation(p, s"$name.att.exponent")
        } yield PointLightUniform(co, ps, in, ac, al, ae)

      private def createSpotLightUniforms(p: GL.Program, name: String, n: Int) =
        IO.foreach(0 until n) { i =>
          for {
            pl <- createPointLightUniform(p, s"$name[$i].pl")
            cd <- gl.getUniformLocation(p, s"$name[$i].conedir")
            ct <- gl.getUniformLocation(p, s"$name[$i].cutoff")
          } yield SpotLightUniform(pl, cd, ct)
        }

      private def createFogUniform(p: Program, name: String) =
        for {
          a <- gl.getUniformLocation(p, s"$name.activeFog")
          c <- gl.getUniformLocation(p, s"$name.colour")
          d <- gl.getUniformLocation(p, s"$name.density")
        } yield FogUniform(a, c, d)

      def loadMesh(program: SceneShaderProgram, input: MeshDefinition) =
        for {
          vao <- gl.genVertexArrays()
          _   <- gl.bindVertexArray(vao)

          posVbo <- gl.genBuffers
          posBuf <- buffers.floatBuffer(input.positions)
          _      <- gl.bindBuffer(GL_ARRAY_BUFFER, posVbo)
          _      <- gl.bufferData(GL_ARRAY_BUFFER, posBuf, GL_STATIC_DRAW)
          _      <- gl.vertexAttribPointer(program.positionAttr, 3, GL_FLOAT, false, 0, 0)

          texVbo <- gl.genBuffers
          texBuf <- buffers.floatBuffer(input.texCoords)
          _      <- gl.bindBuffer(GL_ARRAY_BUFFER, texVbo)
          _      <- gl.bufferData(GL_ARRAY_BUFFER, texBuf, GL_STATIC_DRAW)
          _      <- gl.vertexAttribPointer(program.texCoordAttr, 2, GL_FLOAT, false, 0, 0)

          norVbo <- gl.genBuffers
          norBuf <- buffers.floatBuffer(input.normals)
          _      <- gl.bindBuffer(GL_ARRAY_BUFFER, norVbo)
          _      <- gl.bufferData(GL_ARRAY_BUFFER, norBuf, GL_STATIC_DRAW)
          _      <- gl.vertexAttribPointer(program.normalsAttr, 3, GL_FLOAT, false, 0, 0)

          weiVbo <- gl.genBuffers
          weiBuf <- buffers.floatBuffer(input.weights)
          _      <- gl.bindBuffer(GL_ARRAY_BUFFER, weiVbo)
          _      <- gl.bufferData(GL_ARRAY_BUFFER, weiBuf, GL_STATIC_DRAW)
          _      <- gl.vertexAttribPointer(program.jointWeightsAttr, 4, GL_FLOAT, false, 0, 0)

          joiVbo <- gl.genBuffers
          joiBuf <- buffers.intBuffer(input.jointIndices)
          _      <- gl.bindBuffer(GL_ARRAY_BUFFER, joiVbo)
          _      <- gl.bufferData(GL_ARRAY_BUFFER, joiBuf, GL_STATIC_DRAW)
          _      <- gl.vertexAttribPointer(program.jointIndicesAttr, 4, GL_FLOAT, false, 0, 0)

          indVbo <- gl.genBuffers
          indBuf <- buffers.intBuffer(input.indices)
          _      <- gl.bindBuffer(GL_ELEMENT_ARRAY_BUFFER, indVbo)
          _      <- gl.bufferData(GL_ELEMENT_ARRAY_BUFFER, indBuf, GL_STATIC_COPY)

          _ <- gl.bindBuffer(GL_ARRAY_BUFFER, VertexBufferObject.None)
          _ <- gl.bindVertexArray(VertexArrayObject.None)

          m <- textureLoader.loadMaterial(input.material)
        } yield Mesh(vao, List(posVbo, texVbo, weiVbo, joiVbo, indVbo), input.indices.length, m)

      def render(
        program: SceneShaderProgram,
        items: Iterable[GameItem],
        transformation: Transformation,
        fixtures: Fixtures
      ) =
        gl.useProgram(program.program) *>
          gl.uniformMatrix4fv(program.uniProjectionMatrix, false, transformation.projectionMatrix) *>
          gl.uniform1f(program.uniSpecularPower, SpecularPower) *>
          gl.uniform3f(program.uniAmbientLight, fixtures.lighting.ambient) *>
          ZIO.foreach(fixtures.lighting.point.zipWithIndex) {
            case (l, i) => setUniform(program.uniPointLight(i), l.toViewCoordinates(transformation))
          } *>
          ZIO.foreach(fixtures.lighting.spot.zipWithIndex) {
            case (l, i) => setUniform(program.uniSpotLight(i), l.toViewCoordinates(transformation))
          } *>
          gl.uniform1i(program.uniTextureSampler, 0) *>
          setUniform(program.uniFog, fixtures.fog) *>
          ZIO.foreach(items)(i => renderItem(program, i, transformation)) *>
          gl.useProgram(Program.None)

      private def renderItem(program: SceneShaderProgram, item: GameItem, transformation: Transformation) =
        gl.uniformMatrix4fv(program.uniModelViewMatrix, false, transformation.getModelViewMatrix(item)) *>
          item.animation.fold(IO.unit)(
            a => gl.uniformMatrix4fvs(program.uniJointsMatrix, false, a.getCurrentFrame.jointMatrices)
          ) *>
          ZIO.foreach(item.meshes)(m => renderMesh(program, m))

      private def renderMesh(program: SceneShaderProgram, mesh: Mesh) =
        mesh.material.texture.fold(IO.unit)(bindTexture) *>
          mesh.material.normalMap.fold(IO.unit)(bindNormalMap) *>
          setUniform(program.uniMaterial, mesh.material) *>
          gl.bindVertexArray(mesh.vao) *>
          gl.enableVertexAttribArray(program.positionAttr) *>
          gl.enableVertexAttribArray(program.texCoordAttr) *>
          gl.enableVertexAttribArray(program.normalsAttr) *>
          gl.enableVertexAttribArray(program.jointWeightsAttr) *>
          gl.enableVertexAttribArray(program.jointIndicesAttr) *>
          gl.drawElements(GL_TRIANGLES, mesh.vertexCount, GL_UNSIGNED_INT, 0) *>
          gl.disableVertexAttribArray(program.positionAttr) *>
          gl.disableVertexAttribArray(program.texCoordAttr) *>
          gl.disableVertexAttribArray(program.normalsAttr) *>
          gl.disableVertexAttribArray(program.jointWeightsAttr) *>
          gl.disableVertexAttribArray(program.jointIndicesAttr) *>
          gl.bindTexture(GL_TEXTURE_2D, Texture.None) *>
          gl.bindVertexArray(VertexArrayObject.None)

      private def bindTexture(texture: Texture) =
        gl.activeTexture(GL_TEXTURE0) *>
          gl.bindTexture(GL_TEXTURE_2D, texture)

      private def bindNormalMap(normalMap: Texture) =
        gl.activeTexture(GL_TEXTURE1) *>
          gl.bindTexture(GL_TEXTURE_2D, normalMap)

      private def setUniform(uni: MaterialUniform, material: Material) =
        gl.uniform4f(uni.ambient, material.ambientColour) *>
          gl.uniform4f(uni.diffuse, material.diffuseColour) *>
          gl.uniform4f(uni.specular, material.specularColour) *>
          gl.uniform1i(uni.hasTexture, material.texture.fold(0)(_ => 1)) *>
          gl.uniform1f(uni.reflectance, material.reflectance)

      private def setUniform(uni: PointLightUniform, pointLight: PointLight): UIO[Unit] =
        gl.uniform3f(uni.colour, pointLight.color) *>
          gl.uniform3f(uni.position, pointLight.position) *>
          gl.uniform1f(uni.intensity, pointLight.intensity) *>
          gl.uniform1f(uni.attConstant, pointLight.attenuation.constant) *>
          gl.uniform1f(uni.attLinear, pointLight.attenuation.linear) *>
          gl.uniform1f(uni.attExponent, pointLight.attenuation.exponent)

      private def setUniform(uni: SpotLightUniform, spotLight: SpotLight): UIO[Unit] =
        setUniform(uni.pl, spotLight.pointLight) *>
          gl.uniform3f(uni.conedir, spotLight.coneDirection) *>
          gl.uniform1f(uni.cutoff, spotLight.cutOff)

      private def setUniform(uni: FogUniform, fog: Fog): UIO[Unit] =
        gl.uniform1i(uni.active, if (fog.active) 1 else 0) *>
          gl.uniform3f(uni.colour, fog.color) *>
          gl.uniform1f(uni.density, fog.density)

      def cleanup(program: SceneShaderProgram) =
        gl.deleteProgram(program.program)
    }
  }

  object Live extends Live

}
