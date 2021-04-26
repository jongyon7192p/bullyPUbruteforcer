package io.github.jgcodes.libsm64.util;

import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;

public class Pointers {
  private Pointers() {}

  /**
   * Acquires a native library's base address.
   * @param lib the native library
   * @return the library's handle
   */
  public static long getHandle(NativeLibrary lib) {
    String s = lib.toString();
    return Long.parseLong(s.substring(s.lastIndexOf('@') + 1, s.length() - 1));
  }

  /**
   * Creates a new pointer which is offset from a specified pointer by {@code off} bytes.
   * @param in the pointer
   * @param off the offset
   * @return a pointer offset from {@code in} by {@code off} bytes
   */
  public static Pointer incrNew(Pointer in, int off) {
    return new Pointer(Pointer.nativeValue(in) + off);
  }

  /**
   * Moves the specified pointer by {@code off} bytes.
   * @param in the pointer
   * @param off the offset
   * @return the pointer {@code in} incremented by {@code off}
   */
  public static Pointer incr(Pointer in, int off) {
    Pointer.nativeValue(in, Pointer.nativeValue(in) + off);
    return in;
  }
}
