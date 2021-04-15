package io.github.jgcodes.bitfs0x.util;

/**
 * A limited vector implementation. Only adds the operations we need.
 */
public record FloatVector3(float x, float y, float z) {
  /**
   * Instantiates a new {@code FloatVector3}. Works best when statically imported.
   * @param x the x-component
   * @param y the y-component
   * @param z the z-component
   * @return a new {@code FloatVector3} with value {@code (x, y, z)}.
   */
  public static FloatVector3 fvec3(float x, float y, float z) {
    return new FloatVector3(x, y, z);
  }

  @Override
  public String toString() {
    return String.format("(%f, %f, %f)", x, y, z);
  }
}
