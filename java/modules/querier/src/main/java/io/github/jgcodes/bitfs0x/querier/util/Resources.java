package io.github.jgcodes.bitfs0x.querier.util;

import java.net.URL;
import java.util.Objects;

public class Resources {
  public static URL get(Class<?> clazz, String id) {
    URL resource = clazz.getResource(id);
    return Objects.requireNonNull(resource, "Resource doesn't exist wut");
  }
}
