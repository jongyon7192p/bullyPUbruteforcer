package io.github.jgcodes.bitfs0x.objects;

import io.github.jgcodes.libsm64.Game;

import java.nio.ByteBuffer;

public class GObjects {
  private GObjects() {}

  public static void copy(Game game, int fromSlot, int toSlot) {
    // 1392 - 160 = 1232
    ByteBuffer fromBuffer = game.objectSlot(fromSlot).getByteBuffer(160, 1232);
    ByteBuffer toBuffer = game.objectSlot(toSlot).getByteBuffer(160, 1232);
    toBuffer.clear().put(fromBuffer.flip());
  }
}
