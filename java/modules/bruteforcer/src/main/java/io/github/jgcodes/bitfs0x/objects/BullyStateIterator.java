package io.github.jgcodes.bitfs0x.objects;

import io.github.jgcodes.bitfs0x.MultiBullyBruteforcer;

import java.util.Iterator;
import java.util.logging.Logger;

/**
 * A better way to iterate over angles and speeds per bully.
 * It only iterates over all angles that are either 0 or 1 (mod 16).
 * It is also infinite, and stopping must be done externally.
 */
public class BullyStateIterator implements Iterator<BullyState> {
  private int nextSpeed;
  private short nextAngle;

  private long timestamp;

  public BullyStateIterator(float minSpd) {
    this.nextSpeed = Float.floatToIntBits(minSpd);
    this.nextAngle = 0;
    this.timestamp = System.currentTimeMillis();
  }

  @Override
  public boolean hasNext() {
    return true;
  }

  @Override
  public BullyState next() {
    BullyState result = new BullyState(Float.intBitsToFloat(nextSpeed), nextAngle);

    final short lastAngle = nextAngle;
    final int nextIntended;
    if (nextAngle % 16 == 0)
      nextIntended = (lastAngle & 0xFFFF) + 1;
    else
      nextIntended = (lastAngle & 0xFFFF) + 15;

    // If there is overflow, at least one of the upper 16 bits are set
    nextAngle = (short) nextIntended;
    if ((nextIntended & 0xFFFF0000) != 0)
      nextSpeed++;


    return result;
  }
}
