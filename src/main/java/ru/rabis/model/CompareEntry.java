package ru.rabis.model;

public record CompareEntry(String level, String message) {
  public static CompareEntry error(String format, Object... args) {
    return new CompareEntry(CompareLevels.ERROR, String.format(format, args));
  }

  public static CompareEntry warning(String format, Object... args) {
    return new CompareEntry(CompareLevels.WARNING, String.format(format, args));
  }
}
