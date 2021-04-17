package io.github.jgcodes.bitfs0x.output;

import io.github.jgcodes.bitfs0x.util.FloatVector2;
import io.github.jgcodes.bitfs0x.util.FloatVector3;

import java.io.Closeable;

/**
 * An output for test cases.
 */
public interface Output extends AutoCloseable {
  void output(FloatVector3 targetPos, int frame, FloatVector3 startPos, float startSpeed, short startYaw,
              FloatVector3 finalPos,
               float finalSpeed, short finalYaw) throws Exception;
}
