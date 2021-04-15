package io.github.jgcodes.libsm64;

import com.sun.jna.Pointer;
import io.github.jgcodes.libsm64.util.Pointers;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

public class Savestate {
  private static record Info(ByteBuffer saveBuffer, ByteBuffer nativeBuffer, int size) {}
  private final List<Info> infoList;
  private final Game game;

  public Savestate(Game game) {
    this.game = game;

    final long base = Pointers.getHandle(game.sm64Lib);

    infoList = Arrays.stream(game.version.segments())
      .map(region -> {
        Pointer p = game.locate("gMarioStates");
        Pointer.nativeValue(p, base + region.address());

        ByteBuffer sBuffer = ByteBuffer.allocate(region.size());
        ByteBuffer nBuffer = p.getByteBuffer(0, region.size());
        return new Info(sBuffer, nBuffer, region.size());
      }).toList();
  }

  public void save() {
    long baseAddress = Pointers.getHandle(game.sm64Lib);
    //dummy call to obtain non-opaque pointer
    Pointer p = game.locate("gMarioStates");
    for (Info info: infoList) {
      info.saveBuffer().clear().put(info.nativeBuffer().flip());
    }
  }
  public void load() {
    long baseAddress = Pointers.getHandle(game.sm64Lib);
    //dummy call to obtain non-opaque pointer
    Pointer p = game.locate("gMarioStates");
    for (Info info: infoList) {
      info.nativeBuffer().clear().put(info.saveBuffer().flip());
    }
  }
}
