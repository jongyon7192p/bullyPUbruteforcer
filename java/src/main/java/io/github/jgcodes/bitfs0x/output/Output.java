package io.github.jgcodes.bitfs0x.output;

import io.github.jgcodes.bitfs0x.util.FloatVector2;
import io.github.jgcodes.bitfs0x.util.FloatVector3;

/**
 * An output for test cases.
 */
public interface Output {
  void output(FloatVector2 targetPos, int frame, FloatVector3 startPos, float startSpeed, short startYaw,
              FloatVector3 finalPos,
               float finalSpeed, short finalYaw);
}
