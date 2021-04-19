package io.github.jgcodes.bitfs0x.objects;

import java.util.Iterator;

public class BullyStateIterator implements Iterator<BullyState> {
  private int nextSpeed;
  private short nextAngle;

  private BullyState last;

  public BullyStateIterator(float minSpd) {
    this.nextSpeed = Float.floatToIntBits(minSpd);
    this.nextAngle = 0;
  }

  @Override
  public boolean hasNext() {
    return true;
  }

  @Override
  public BullyState next() {
    BullyState result = new BullyState(Float.intBitsToFloat(nextSpeed), nextAngle);
    last = result;

    final short lastAngle = nextAngle;
    if (nextAngle % 16 == 0)
      nextAngle += 1;
    else
      nextAngle += 15;
    // increment speed if angle overflows
    if (Short.compareUnsigned(nextAngle, lastAngle) < 0) {
      nextSpeed++;
    }

    return result;
  }

  /**
   * Returns the value that was returned by the most recent call to {@link BullyStateIterator#next()}.
   * @return the value that was returned by the most recent call to {@code next()}.
   */
  public BullyState last() {
    return last;
  }
}
