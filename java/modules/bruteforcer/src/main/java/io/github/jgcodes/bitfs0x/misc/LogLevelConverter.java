package io.github.jgcodes.bitfs0x.misc;

import picocli.CommandLine.ITypeConverter;

import java.util.logging.Level;

public class LogLevelConverter implements ITypeConverter<Level> {
  @Override
  public Level convert(String value) {
    return switch (value) {
      case "trace" -> Level.FINEST;
      case "info" -> Level.INFO;
      case "warning" -> Level.WARNING;
      case "severe" -> Level.SEVERE;
      default -> Level.OFF;
    };
  }
}
