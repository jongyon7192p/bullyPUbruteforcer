package io.github.jgcodes.bitfs0x;

import com.sun.jna.Pointer;
import io.github.jgcodes.bitfs0x.output.Output;
import io.github.jgcodes.bitfs0x.output.PrintStreamOutput;
import io.github.jgcodes.bitfs0x.util.FloatVector2;
import io.github.jgcodes.bitfs0x.util.FloatVector3;
import io.github.jgcodes.libsm64.Game;
import io.github.jgcodes.libsm64.Game.Version;
import io.github.jgcodes.libsm64.Input;
import io.github.jgcodes.libsm64.M64;
import io.github.jgcodes.libsm64.Savestate;
import io.github.jgcodes.libsm64.util.Pointers;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import static io.github.jgcodes.bitfs0x.util.FloatVector2.fvec2;
import static io.github.jgcodes.bitfs0x.util.FloatVector3.fvec3;

@Command(
  name = "bully-bruteforcer",
  description = """
    A program which bruteforces sending a bully to space and back so it can hit Mario into a platform.
    It's pretty hard to figure out manually, that's why we're resorting to this.
    """
)
public class BullyBruteforcer implements Callable<Void> {
  // Constants
  private static final FloatVector3 startBullyPos = fvec3(-2236, -2950, -566);
  private static final FloatVector3 hackMarioPos = fvec3(-1945, -2918, -715);
  private static final short startYaw = (short) 355;
  private static final FloatVector2 targetPos = fvec2(-1720, -460);

  // Command-line args for Picocli
  @Option(names = {"--help", "-h", "-?"}, description = "Display this help list", usageHelp = true)
  boolean sendHelpMsg;

  @Parameters(index = "0", description = "Path to a valid Wafel SM64 (J) DLL", paramLabel = "<DLL PATH>")
  Path dllPath;

  @Option(names = "--min-dist", description = "how close to the original position the bully can be", defaultValue = "200")
  float minDist = 200;
  @Option(names = "--max-dist", description = "how far out from the original position the bully can be", defaultValue = "1000")
  float maxDist = 1000;

  @Option(names="--min-speed", description = "the minimum speed to check", defaultValue = "2000000")
  float minSpeed = 2000000;
  @Option(names="--max-speed", description = "the maximum speed to check", defaultValue = "10000000")
  float maxSpeed = 10000000;

  @Option(names="--max-frames", description = "the maximum number of frames to simulate before ruling the value out")
  int maxFrames = 26;

  //call method
  @Override
  public Void call() throws Exception {
    // Load SM64 and .m64
    Game game = new Game(Version.JP, dllPath);
    M64 m64 = new M64(BullyBruteforcer.class.getResourceAsStream("/assets/1Key_4_21_13_Padded.m64"));

    // final int backupFrame;

    Savestate st = new Savestate(game);

    // advance to frame 3286
    List<Input> inputs = m64.getInputs();
    for (int frame = 0; frame < inputs.size(); frame++) {
      game.advance(inputs.get(frame));

      if (frame == 3286) {
        final Pointer objPool = game.locate("gObjectPool");
        // Deactivate everything except the bully and the tilting platforms
        for (int obj = 0; obj < 108; obj++) {
          switch (obj) {
            case 27, 83, 84 -> {}
            default -> {
              short activeFlag = objPool.getShort(obj * 1392 + 180);
              objPool.setShort(obj * 1392 + 180, (short) (activeFlag & 0xFFFE));
            }
          }
        }
        st.save();
        // backupFrame = frame + 1;
        break;
      }
    }
    // Initialize all pointers
    final Pointer
      marioX, marioY, marioZ,
      bullyX, bullyY, bullyZ,
      bullyHSpeed, bullyYaw1, bullyYaw2; {
        final Pointer marioPtr = game.locate("gMarioStates");
        marioX = Pointers.incr(marioPtr, 60);
        marioY = Pointers.incr(marioPtr, 64);
        marioZ = Pointers.incr(marioPtr, 68);

        final Pointer bullyPtr = Pointers.incr(game.locate("gObjectPool"), 27 * 1392);

        bullyX = Pointers.incr(bullyPtr, 240);
        bullyY = Pointers.incr(bullyPtr, 244);
        bullyZ = Pointers.incr(bullyPtr, 248);
        bullyHSpeed = Pointers.incr(bullyPtr, 264);
        bullyYaw1 = Pointers.incr(bullyPtr, 280);
        bullyYaw2 = Pointers.incr(bullyPtr, 292);
    }

    // Print bully state to stderr
    st.load();
    System.err.printf("""
        Bully initial state:
        Pos: (%s, %s, %s)
        H speed: %s
        Yaw values: %s, %s
              
              
        """,
      bullyX.getFloat(0),
      bullyY.getFloat(0),
      bullyZ.getFloat(0),
      bullyHSpeed.getFloat(0),
      bullyYaw1.getShort(0) & 0xFFFF,
      bullyYaw2.getShort(0) & 0xFFFF
    );

    // Bruteforce :)
    final Set<FloatVector3> positions = new HashSet<>();
    try (PrintStream fileOut = new PrintStream(new FileOutputStream("results.txt"))) {
      final List<Output> outputs = List.of(
        new PrintStreamOutput(System.out),
        new PrintStreamOutput(fileOut)
      );

      // Iterate over all valid floats from minSpeed to maxSpeed
      for (int i = Float.floatToIntBits(minSpeed); i < Float.floatToIntBits(maxSpeed); i++) {
        final float bullySpeed = Float.intBitsToFloat(i);
        if (i % 512 == 0)
          System.err.printf("%d -> %f\n", i, bullySpeed);

        //reset bully pos/angle
        bullyX.setFloat(0, startBullyPos.x());
        bullyY.setFloat(0, startBullyPos.y());
        bullyZ.setFloat(0, startBullyPos.z());

        bullyHSpeed.setFloat(0, bullySpeed);
        bullyYaw1.setShort(0, startYaw);
        bullyYaw2.setShort(0, startYaw);

        for (int frame = 0; frame < maxFrames; frame++) {
          // hack mario in place and advance
          marioX.setFloat(0, hackMarioPos.x());
          marioY.setFloat(0, hackMarioPos.y());
          marioZ.setFloat(0, hackMarioPos.z());
          game.advance();

          // calculate new bully pos variables
          FloatVector3 newBullyPos = fvec3(
            bullyX.getFloat(0),
            bullyY.getFloat(0),
            bullyZ.getFloat(0)
          );

          // determine horizontal distance to (-1720, -460)
          final double dist = Math.hypot(bullyX.getFloat(0) + 1720d, bullyZ.getFloat(0) + 460d);

          if (dist < 300) {
            if (!positions.contains(newBullyPos)) {
            /*System.out.printf("Start pos: %s, Speed: %f; Yaw: %d; Frame: %d; New pos: %s; Dist. to target: %f; " +
                "Dist/Speed: %f; Yaw: %d\n",
              startBullyPos, bullySpeed, startYaw,
              frame + 1, newBullyPos, dist, dist/bullySpeed,
              bullyYaw1.getShort(0)
            );*/
              for (Output output: outputs) {
                output.output(
                  targetPos, frame,
                  startBullyPos, bullySpeed, startYaw,
                  newBullyPos, bullyHSpeed.getFloat(0), bullyYaw1.getShort(0)
                );
              }
            }
            positions.add(newBullyPos);
          }
        }
      }
    }

    return null;
  }

  public static void main(String[] args) {
    CommandLine cl = new CommandLine(new BullyBruteforcer());
    cl.setExitCodeExceptionMapper(exc -> {
      if (exc instanceof FileNotFoundException) {
        return 1;
      }
      else if (exc instanceof IOException) {
        return 2;
      }
      return 0;
    });
    int exitCode = cl.execute(args);
    System.exit(exitCode);
  }
}
