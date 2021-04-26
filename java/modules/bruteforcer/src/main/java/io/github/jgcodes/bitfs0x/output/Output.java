package io.github.jgcodes.bitfs0x.output;

import io.github.jgcodes.libsm64.math.FloatVector3;

/**
 * An output for test cases.
 */
public interface Output extends AutoCloseable {
  void write(FloatVector3 targetPos, int frame, FloatVector3 startPos, float startSpeed, short startYaw,
             FloatVector3 finalPos,
             float finalSpeed, short finalYaw) throws Exception;
}
