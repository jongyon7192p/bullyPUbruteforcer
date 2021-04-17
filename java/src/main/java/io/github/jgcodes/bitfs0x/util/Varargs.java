package io.github.jgcodes.bitfs0x.util;

public class Varargs {
  @SafeVarargs
  public static <T> T[] array(T... things) {return things;}
}
