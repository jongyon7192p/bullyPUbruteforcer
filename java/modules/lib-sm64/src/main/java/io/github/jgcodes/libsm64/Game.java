package io.github.jgcodes.libsm64;

import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import io.github.jgcodes.libsm64.util.MemoryRegion;
import io.github.jgcodes.libsm64.util.Pointers;

import java.io.FileNotFoundException;
import java.nio.file.Path;

public class Game {
  public enum Version {
    /**
     * Represents the US version of SM64.<br>
     * This version had some glitches patched (e.g. timestop, spawning displacement),
     * but for the most part is basically as glitchy.
     */
    US(
      new MemoryRegion(1302528, 2836576), //data
      new MemoryRegion(14045184, 4897408) //bss
    ),
    /**
     * Represents the original Japanese version of SM64.<br>
     * This version has a few extra glitches that later versions lack.
     */
    JP(
      new MemoryRegion(1294336, 2406112), //data
      new MemoryRegion(13594624, 4897632) //bss
    );

    private final MemoryRegion[] segments;

    Version(MemoryRegion... segments) {
      this.segments = segments;
    }

    public MemoryRegion[] segments() {
      return segments;
    }
  }
  // Proxy interfaces
  private final SuperMario64 sm64;
  private final NativeLibrary sm64Lib;
  // Private properties
  final Version version;

  public Game(Version version, Path dllPath) throws FileNotFoundException {
    if (!dllPath.getFileName().toString().endsWith(".dll")) throw new FileNotFoundException("File is not a DLL");
    this.version = version;
    // Load sm64_jp.dll or sm64_us.dll
    System.setProperty("jna.library.path", dllPath.getParent().toAbsolutePath().toString());
    final String dllName = dllPath.getFileName().toString();
    sm64 = Native.load(dllName, SuperMario64.class);
    sm64.sm64_init();
    sm64Lib = NativeLibrary.getInstance(dllName);
  }

  public void advance(Input input) {
    setInput(input);
    advance();
  }

  public void advance() {
    sm64.sm64_update();
  }

  public Pointer locate(String name) {
    return sm64Lib.getGlobalVariableAddress(name);
  }

  public Pointer handle() {
    return new Pointer(Pointers.getHandle(sm64Lib));
  }

  public Pointer objectSlot(int slot) {
    Pointer p = locate("gObjectPool");
    return Pointers.incr(p, 1392 * slot);
  }

  public void setInput(Input input) {
    Pointer controlPtr = locate("gControllerPads");
    controlPtr.setShort(0, input.buttons());
    controlPtr.setByte(2, input.joyX());
    controlPtr.setByte(3, input.joyY());
  }
}
