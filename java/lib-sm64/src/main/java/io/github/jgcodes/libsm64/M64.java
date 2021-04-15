package io.github.jgcodes.libsm64;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

public class M64 implements Iterable<Input> {
  PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:*.m64");

  private List<Input> inputs;

  /**
   * Reads a .m64 file, converting it into an internal input list.
   *
   * @param is an input stream containing the M64
   * @throws FileNotFoundException if the file does not exist or isn't an .m64
   * @throws IOException if some other I/O error occurs or the .m64 does not conform to the format
   */
  public M64(InputStream is) throws IOException {
    inputs = new ArrayList<>();

    try (BufferedInputStream in = new BufferedInputStream(is)) {
      byte[] bytes = new byte[4];
      List<Input> tmp = new ArrayList<>();
      //thing
      in.skipNBytes(0x400L);
      while (in.read(bytes) > 0) {
        tmp.add(new Input(bytes));
      }
      inputs = Collections.unmodifiableList(tmp);
    }
    catch (IndexOutOfBoundsException e) {
      throw new IOException(".m64 file is not formatted correctly");
    }
  }

  @Override
  public Iterator<Input> iterator() {
    return inputs.iterator();
  }

  @Override
  public void forEach(Consumer<? super Input> action) {
    for (Input input: inputs) {
      action.accept(input);
    }
  }

  public List<Input> getInputs() {
    return inputs;
  }
}
