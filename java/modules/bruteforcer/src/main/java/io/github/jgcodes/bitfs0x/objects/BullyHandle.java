package io.github.jgcodes.bitfs0x.objects;

import com.sun.jna.Pointer;
import io.github.jgcodes.libsm64.math.FloatVector3;

public record BullyHandle(
  Pointer x, Pointer y, Pointer z,
  Pointer hSpeed, Pointer yaw1, Pointer yaw2) {

  public void setPosition(FloatVector3 pos) {
    x.setFloat(0, pos.x());
    y.setFloat(0, pos.y());
    z.setFloat(0, pos.z());
  }

  public FloatVector3 getPosition() {
    return new FloatVector3(x.getFloat(0), y.getFloat(0), z.getFloat(0));
  }
}
