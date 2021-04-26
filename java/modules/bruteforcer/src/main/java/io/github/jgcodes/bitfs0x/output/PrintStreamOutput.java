package io.github.jgcodes.bitfs0x.output;

import io.github.jgcodes.libsm64.math.FloatVector3;

import java.io.PrintStream;

public class PrintStreamOutput implements Output {
  private final PrintStream ps;

  public PrintStreamOutput(PrintStream ps) {
    this.ps = ps;
  }

  @Override
  public void write(FloatVector3 targetPos, int frame, FloatVector3 startPos, float startSpeed, short startYaw,
                    FloatVector3 finalPos, float finalSpeed, short finalYaw) {
    System.out.printf("""
        Target: %s Frame: %d
        Initial │ Pos: %s Speed: %f Yaw: %d
        ────────┼───────────────────────────────────────────────────────────────────────────
        Final   │ Pos: %s Speed: %f Yaw: %d
        ────────┴───────────────────────────────────────────────────────────────────────────
        Distance to target: %f
        
        """,
      targetPos, frame,
      startPos, startSpeed, startYaw & 0xFFFF,
      finalPos, finalSpeed, finalYaw & 0xFFFF,
      finalPos.hDist(targetPos)
    );
  }

  @Override
  public void close() {
    if ((ps == System.out) || (ps == System.err)) return;
    ps.close();
  }
}
