package io.github.jgcodes.libsm64;

/**
 * A set of inputs, which can be sourced from a Mupen64 movie and written to SM64.
 */
public record Input(short buttons, byte joyX, byte joyY) {
  // TODO: add bitmask constants
  @Override
  public String toString() {
    String btnString = Integer.toBinaryString(buttons & 0xFFFF);

    return String.format("Buttons: 0b%s | Joystick: (%s, %s)",
      "0".repeat(16 - btnString.length()).concat(btnString),
      joyX & 0xFF,
      joyY & 0xFF
    );
  }

  /**
   * Reads the first four bytes of {@code buf}, converting them to their corresponding values.
   * @param buf a byte array containing at least 4 bytes
   *
   * @throws IndexOutOfBoundsException if the byte array is too short
   */
  public Input(byte[] buf) {
    this(
      (short) ((buf[0] << 8) + (buf[1] & 0xFF)),
      buf[2],
      buf[3]
    );
  }
}
