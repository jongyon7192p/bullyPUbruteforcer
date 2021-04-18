package io.github.jgcodes.libsm64.util;

public class NativeException extends RuntimeException {
  public NativeException() {
    super();
  }

  public NativeException(String message) {
    super(message);
  }

  public NativeException(String message, Throwable cause) {
    super(message, cause);
  }

  public NativeException(Throwable cause) {
    super(cause);
  }
}
