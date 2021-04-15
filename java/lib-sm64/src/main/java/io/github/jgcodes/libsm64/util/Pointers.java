package io.github.jgcodes.libsm64.util;

import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;

public class Pointers {
  public static long getHandle(NativeLibrary lib) {
    String s = lib.toString();
    return Long.parseLong(s.substring(s.lastIndexOf('@') + 1, s.length() - 1));
  }

  public static Pointer incr(Pointer in, int diff) {
    return new Pointer(Pointer.nativeValue(in) + diff);
  }
}
