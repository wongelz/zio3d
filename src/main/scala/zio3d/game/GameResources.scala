package zio3d.game

import java.nio.file.{Path, Paths}

object GameResources {

  object models {
    final val boblamp = unsafePath("/models/bob/boblamp.md5mesh")

    final val house = unsafePath("/models/house/house.obj")

    final val monster = unsafePath("/models/monster/monster.md5mesh")

    final val particle = unsafePath("/models/particle/particle.obj")

    final val tree = unsafePath("/models/tree/palm_tree.FBX")
  }

  object textures {
    final val heightmap = unsafePath("/textures/heightmap.png")

    final val terrain = unsafePath("/textures/terrain.png")

    object fire {
      final val image = unsafePath("/models/particle/particle_anim.png")
      final val cols  = 4
      final val rows  = 4
    }

    final val bullet = unsafePath("/models/particle/particle.png")

    object skybox {
      final val front = unsafePath("/models/skybox/drakeq_ft.tga")
      final val back  = unsafePath("/models/skybox/drakeq_bk.tga")
      final val up    = unsafePath("/models/skybox/drakeq_up.tga")
      final val down  = unsafePath("/models/skybox/drakeq_dn.tga")
      final val right = unsafePath("/models/skybox/drakeq_rt.tga")
      final val left  = unsafePath("/models/skybox/drakeq_lf.tga")
    }
  }

  object fonts {
    final val regular = unsafePath("/fonts/OpenSans-Regular.ttf")

    final val bold = unsafePath("/fonts/OpenSans-Bold.ttf")
  }

  def unsafePath(name: String): Path =
    path(name).getOrElse(throw new RuntimeException(s"Resource not found: $name"))

  // note: as assimp is a C++ library, must provide path to raw file on disk, not resource in jar
  def path(name: String): Option[Path] =
    Option(Paths.get("./src/main/resources", name))
}
