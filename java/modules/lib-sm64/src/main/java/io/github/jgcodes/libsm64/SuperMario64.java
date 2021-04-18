package io.github.jgcodes.libsm64;

import com.sun.jna.Library;

/**
 * A proxy interface, used with JNA to access the Wafel dll.
 */
public interface SuperMario64 extends Library {
  void sm64_init();
  void sm64_update();

}
