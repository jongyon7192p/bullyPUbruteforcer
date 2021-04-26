package io.github.jgcodes.libsm64;

import com.sun.jna.Pointer;
import io.github.jgcodes.libsm64.util.Pointers;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;

public class Savestate {
  private static record Info(byte[] saveData, Pointer dllPointer) {}
  private final List<Info> infoList;
  private final Game game;

  public Savestate(Game game) {
    this.game = game;

    infoList = Arrays.stream(game.version.segments())
      .map(region -> {
        byte[] saveData = new byte[region.size()];
        Pointer dllPointer = Pointers.incr(game.handle(), region.address());
        return new Info(saveData, dllPointer);
      }).toList();
  }

  public void save() {
    for (Info info: infoList) {
      // copy DLL data to save buffer
      info.dllPointer().read(0, info.saveData(), 0, info.saveData().length);
    }
  }
  public void load() {
    for (Info info: infoList) {
      // copy save buffer to DLL
      info.dllPointer().write(0, info.saveData(), 0, info.saveData().length);
    }
  }

  public void dump() {
    Path p = Path.of("dump2.bin").toAbsolutePath();
    System.out.println(p);
    try (FileOutputStream fOut = new FileOutputStream("dump2.bin")) {
      System.out.println("channel open");
      for (Info info: infoList) {
        fOut.write(info.saveData);
      }
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }
}
