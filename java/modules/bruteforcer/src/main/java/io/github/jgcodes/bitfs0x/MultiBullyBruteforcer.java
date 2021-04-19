package io.github.jgcodes.bitfs0x;

import com.sun.jna.Pointer;
import io.github.jgcodes.bitfs0x.objects.*;
import io.github.jgcodes.bitfs0x.output.CompoundOutput;
import io.github.jgcodes.bitfs0x.output.DBOutput;
import io.github.jgcodes.bitfs0x.output.PrintStreamOutput;
import io.github.jgcodes.libsm64.Game;
import io.github.jgcodes.libsm64.Game.Version;
import io.github.jgcodes.libsm64.Input;
import io.github.jgcodes.libsm64.M64;
import io.github.jgcodes.libsm64.Savestate;
import io.github.jgcodes.libsm64.math.FloatVector3;
import io.github.jgcodes.libsm64.util.Pointers;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Callable;

import static io.github.jgcodes.libsm64.math.FloatVector3.fvec3;

public class MultiBullyBruteforcer implements Callable<Void> {
  // Only with *Multiple Bullies*:tm:
  private static final List<Integer> bullySlots;
  static {
    List<Integer> bullySlots_tmp = new ArrayList<>(110);
    //adds list(range(24))
    for (int i = 0; i <= 24; i++)
      bullySlots_tmp.add(i);
    bullySlots_tmp.addAll(Arrays.asList(
      26, 28, 29, 30, 32,
      34,
      35, 37, 38, 39, 40, 42,
      48, 49,
      50,
      51,
      52, 53, 54, 55, 56, 57, 58, 60, 61, 63, 64, 65, 66, 67, 87,
      90, 91, 92, 93, 95, 96, 98, 99, 105, 106, 107
    ));
    bullySlots = Collections.unmodifiableList(bullySlots_tmp);
  }

  // Constants
  private static final FloatVector3 startBullyPos = fvec3(-2236, -2950, -566);
  private static final FloatVector3 hackMarioPos = fvec3(-1945, -2918, -715);
  private static final FloatVector3 targetPos = fvec3(-1720, -2910, -460);

  // Command-line args for Picocli
  @Option(names = {"--help", "-h", "-?"}, description = "Display this help list", usageHelp = true)
  boolean sendHelpMsg;

  @Option(names = {"--version", "-v"}, description = "Show the version help", versionHelp = true)
  boolean sendVersionMsg;

  @Option(names = {"--wafel-path", "-wp"}, description = "Path to wafel DLL", required = true)
  Path dllPath;

  @Option(
    names = {"--db-url", "-url"},
    description = "URL of the BitFSQL database",
    required = true
  )
  String dbURL;

  @Option(
    names = {"--db-password", "-pw"},
    description = "Password to the BitFSQL database",
    required = true
  )
  String password;

  @Option(names = "--max-dist", description = "how far out from the target the bully can be",
    defaultValue = "300")
  float maxDist = 300;

  @Option(names="--min-speed", description = "the minimum speed to check", defaultValue = "2000000")
  float minSpeed = 2000000;
  @Option(names="--max-speed", description = "the maximum speed to check", defaultValue = "10000000")
  float maxSpeed = 10000000;

  @Option(names="--max-frames", description = "the maximum number of frames to simulate before giving up")
  int maxFrames = 25;

  @Override
  public Void call() throws Exception {
    Game game = new Game(Version.JP, dllPath);
    M64 m64 = new M64(MultiBullyBruteforcer.class.getResourceAsStream("/assets/1Key_4_21_13_Padded.m64"));

    Savestate st = new Savestate(game);

    // Load and run the modified 1-Key TAS up to BitFS.
    List<Input> inputs = m64.getInputs();
    for (int frame = 0; frame < inputs.size(); frame++) {
      game.advance(inputs.get(frame));

      // This is the "special
      if (frame == 3285) {
        final Pointer objPool = game.locate("gObjectPool");
        /*
         * Deactivate everything that isn't Mario, the bully or the tilting platform.
         * This prevents overwriting, as well as serving as an optimization.
         */
        for (int obj = 0; obj < 108; obj++) {
          switch (obj) {
            case 27, 83, 84, 89 -> {}
            default -> {
              short activeFlag = objPool.getShort(obj * 1392 + 180);
              objPool.setShort(obj * 1392 + 180, (short) (activeFlag & 0xFFFE));
            }
          }
        }
        /*
         * Make all the other objects behave like THE bully.
         * If this were rendered, we'd see coins, goombas, platforms, etc. flying at PU speed.
         */
        for (int slot: bullySlots) {
          GObjects.copy(game, 27, slot);
        }
        //Savestate and break.
        st.save();
        break;
      }
    }

    // Initialize Mario's pointers.
    final MarioHandle mario; {
      final Pointer gMarioStates = game.locate("gMarioStates");
      mario = new MarioHandle(
        Pointers.incrNew(gMarioStates, 60),
        Pointers.incrNew(gMarioStates, 64),
        Pointers.incrNew(gMarioStates, 68)
      );
    }

    // Initialize and cache the bully pointers.
    final List<BullyHandle> bullies = bullySlots.stream().map(slot -> {
      Pointer slotPtr = game.objectSlot(slot);
      return new BullyHandle(
        Pointers.incrNew(slotPtr, 240),
        Pointers.incrNew(slotPtr, 244),
        Pointers.incrNew(slotPtr, 248),
        Pointers.incrNew(slotPtr, 264),
        Pointers.incrNew(slotPtr, 280),
        Pointers.incrNew(slotPtr, 292)
      );
    }).toList();

    // Bruteforce!
    try (CompoundOutput output = new CompoundOutput(
      new PrintStreamOutput(System.out),
      new PrintStreamOutput(new PrintStream(new FileOutputStream("results.txt"))),
      new DBOutput(dbURL, password)
    )) {
      BullyStateIterator stateIterator = new BullyStateIterator(minSpeed);
      List<BullyState> states = Arrays.asList(new BullyState[bullies.size()]);

      boolean flag = true;
      long timestamp = System.currentTimeMillis();
      for (int counter = 1; flag; counter++) {
        /*if (counter % 1000 == 0) {
          long next = System.currentTimeMillis();
          System.err.printf("Completed last 1000 iterations in %d ms | Avg. speed %f iters/s\n",
            next - timestamp, (1000.0 / (next - timestamp) * 1000));
          timestamp = next;
        }*/
        //Initialize the bullies with the values we want
        for (int i = 0; i < bullies.size(); i++) {
          final BullyHandle bully = bullies.get(i);

          BullyState state = stateIterator.next();
          if (state.speed() >= maxSpeed) flag = false;

          bully.setPosition(startBullyPos);
          state.apply(bully);

          states.set(i, state);
        }

        for (int frame = 0; frame < maxFrames; frame++) {
          // Freeze Mario on the tilting platform to make it solid
          mario.setPosition(hackMarioPos);
          game.advance();

          // Check if any of the bullies are candidates
          for (int i = 0; i < bullies.size(); i++) {
            final BullyHandle bully = bullies.get(i);
            final BullyState state = states.get(i);
            FloatVector3 bullyPos = bully.getPosition();

            if (bullyPos.hDist(targetPos) <= maxDist) {
              output.output(
                targetPos, frame,
                startBullyPos, state.speed(), state.angle(),
                bullyPos, bully.hSpeed().getFloat(0), bully.yaw1().getShort(0)
              );
            }
          }
        }
      }
    }
    return null;
  }

  public static void main(String[] args) {
    CommandLine cl = new CommandLine(new MultiBullyBruteforcer());
    cl.setExitCodeExceptionMapper(exc -> {
      if (exc instanceof FileNotFoundException) {
        return 2;
      }
      else if (exc instanceof IOException) {
        return 3;
      }
      else if (exc instanceof SQLException) {
        return 4;
      }
      else if (exc != null) {
        return 1;
      }
      return 0;
    });
    int exitCode = cl.execute(args);
    System.exit(exitCode);
  }
}
