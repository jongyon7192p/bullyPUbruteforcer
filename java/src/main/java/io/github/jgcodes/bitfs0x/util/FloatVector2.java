package io.github.jgcodes.bitfs0x.util;

public record FloatVector2(float x, float y) {
  /**
   * Instantiates a new {@code FloatVector3}. Works best when statically imported.
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
}
