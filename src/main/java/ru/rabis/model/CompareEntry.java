package ru.rabis.model;

public record CompareEntry(String level, String message) {
  public static CompareEntry error(String message) {
    return new CompareEntry(CompareLevels.ERROR, message);
  }

  public static CompareEntry warning(String message) {
    return new CompareEntry(CompareLevels.WARNING, message);
  }
}
