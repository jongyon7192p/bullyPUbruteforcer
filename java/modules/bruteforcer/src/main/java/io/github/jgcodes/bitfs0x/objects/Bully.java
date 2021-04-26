package io.github.jgcodes.bitfs0x.objects;

import com.sun.jna.Pointer;
import io.github.jgcodes.libsm64.math.FloatVector3;

public class BullyHandle {
  private static final int
    POS_X = 240,
    POS_Y = 244,
    POS_Z = 248,
    H_SPEED = 264,
    YAW_1 = 280,
    ACTION = 28

  final Pointer obj;

  public BullyHandle(Pointer obj) {
    this.obj = obj;
  }


  public void setPosition(FloatVector3 pos) {
    obj.setFloat(240, pos.x());
    obj.setFloat(244, pos.y());
    obj.setFloat(248, pos.z());
  }

  public FloatVector3 getPosition() {
    return new FloatVector3(obj.getFloat(240), obj.getFloat(240), obj.getFloat(0));
  }
}
