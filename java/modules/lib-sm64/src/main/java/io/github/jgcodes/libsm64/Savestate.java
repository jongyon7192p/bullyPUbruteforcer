package io.github.jgcodes.libsm64;

import com.sun.jna.Pointer;
import io.github.jgcodes.libsm64.util.Pointers;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

public class Savestate {
  private static record Info(ByteBuffer saveBuffer, ByteBuffer nativeBuffer) {}
  private final List<Info> infoList;
  private final Game game;

  public Savestate(Game game) {
    this.game = game;

    infoList = Arrays.stream(game.version.segments())
      .map(region -> {
        Pointer p = game.pointVirtual(region.address());

        ByteBuffer sBuffer = ByteBuffer.allocate(region.size());
        ByteBuffer nBuffer = p.getByteBuffer(0, region.size());
        return new Info(sBuffer, nBuffer);
      }).toList();
  }

  public void save() {
    for (Info info: infoList) {
      // copy DLL data to save buffer
      info.saveBuffer().clear().put(info.nativeBuffer().flip());
    }
  }
  public void load() {
    for (Info info: infoList) {
      // copy save buffer to DLL
      info.nativeBuffer().clear().put(info.saveBuffer().flip());
    }
  }
}
