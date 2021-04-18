package io.github.jgcodes.libsm64.math;

public record FloatVector2(float x, float y) {
  /**
   * Instantiates a new {@code FloatVector2}. Works best when statically imported.
   * @param x the x-component
   * @param y the y-component
   * @return a new {@code FloatVector2} with value {@code (x, y)}.
   */
  public static FloatVector2 fvec2(float x, float y) {
    return new FloatVector2(x, y);
  }

  @Override
  public String toString() {
    return String.format("(%f, %f)", x, y);
  }

  public double distTo(FloatVector2 that) {
    return Math.hypot(that.x - this.x, that.y - this.y);
  }
}
