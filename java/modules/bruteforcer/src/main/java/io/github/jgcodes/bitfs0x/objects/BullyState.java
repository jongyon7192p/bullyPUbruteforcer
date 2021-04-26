package io.github.jgcodes.bitfs0x.objects;

public record BullyState(float speed, short angle) {
  public void apply(Bully bully) {
    bully.setHSpeed(speed);
    bully.setYaw1(angle);
    bully.setYaw2(angle);
  }
}
