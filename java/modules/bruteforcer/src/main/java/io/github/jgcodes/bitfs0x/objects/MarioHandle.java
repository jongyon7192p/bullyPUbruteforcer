package io.github.jgcodes.bitfs0x.objects;

import com.sun.jna.Pointer;
import io.github.jgcodes.libsm64.math.FloatVector3;

public record MarioHandle(Pointer x, Pointer y, Pointer z) {
  public void setPosition(FloatVector3 pos) {
    x.setFloat(0, pos.x());
    y.setFloat(0, pos.y());
    z.setFloat(0, pos.z());
  }
}
