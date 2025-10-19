package ru.rabis.utils;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;

public class Utils {
  public static boolean isValidUuid(final String text) {
    try {
      UUID.fromString(text);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }
}
