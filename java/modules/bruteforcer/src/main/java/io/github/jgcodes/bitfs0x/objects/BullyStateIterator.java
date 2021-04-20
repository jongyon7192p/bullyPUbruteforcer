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
    if (nextAngle % 16 == 0)
      nextAngle += 1;
    else
      nextAngle += 15;
    // increment speed if angle overflows
    if (Short.compareUnsigned(nextAngle, lastAngle) < 0) {
      long now = System.currentTimeMillis();
      Logger.getLogger(MultiBullyBruteforcer.class.getCanonicalName())
        .info(String.format("Done float %f in %d ms", Float.intBitsToFloat(nextSpeed), now - timestamp));
      timestamp = now;
      nextSpeed++;
    }

    return result;
  }
}
