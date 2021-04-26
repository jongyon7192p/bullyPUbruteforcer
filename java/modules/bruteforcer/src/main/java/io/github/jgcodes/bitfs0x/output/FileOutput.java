package io.github.jgcodes.bitfs0x.output;

import io.github.jgcodes.libsm64.math.FloatVector3;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

public class FileOutput implements Output {
  private final PrintWriter pw;
  private final FileWriter fw;

  public FileOutput(String path) throws IOException {
    fw = new FileWriter(path);
    pw = new PrintWriter(fw);
  }

  @Override
  public void write(FloatVector3 targetPos, int frame, FloatVector3 startPos, float startSpeed, short startYaw,
                    FloatVector3 finalPos, float finalSpeed, short finalYaw) throws Exception {
    pw.printf("""
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
      finalPos.hDist(targetPos));
  }

  @Override
  public void close() throws Exception {
    pw.close();
    fw.close();
  }
}
