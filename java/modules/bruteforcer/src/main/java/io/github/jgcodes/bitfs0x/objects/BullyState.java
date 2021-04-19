package io.github.jgcodes.bitfs0x.objects;

public record BullyState(float speed, short angle) {
  public void apply(BullyHandle bully) {
    bully.hSpeed().setFloat(0, speed);
    bully.yaw1().setShort(0, angle);
    bully.yaw2().setShort(0, angle);
  }
}
