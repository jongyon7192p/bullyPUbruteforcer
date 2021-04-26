package io.github.jgcodes.bitfs0x.output;

import io.github.jgcodes.libsm64.math.FloatVector3;

import java.util.Collection;
import java.util.List;

public class CompoundOutput implements Output {
  private final List<Output> outputs;

  public CompoundOutput(Output... outputs) {
    this.outputs = List.of(outputs);
  }
  public CompoundOutput(Collection<Output> outputs) {
    this.outputs = List.copyOf(outputs);
  }

  @Override
  public void write(FloatVector3 targetPos, int frame, FloatVector3 startPos, float startSpeed, short startYaw,
                    FloatVector3 finalPos, float finalSpeed, short finalYaw) throws Exception {
    for (Output output: outputs)
      output.write(targetPos, frame, startPos, startSpeed, startYaw, finalPos, finalSpeed, finalYaw);
  }

  @Override
  public void close() throws Exception {
    for (Output output: outputs)
      output.close();
  }
}
