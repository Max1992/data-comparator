package ru.rabis.utils;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.UUID;
import ru.rabis.model.TypeFormat;

public class Utils {
  public static boolean isValidUuid(final String text) {
    try {
      UUID.fromString(text);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  public static TypeFormat getTypeFormat(final JsonNode value, JsonNode fieldSchema) {
    String format = fieldSchema.path("format").asText("");
    if (format.equals("date") || format.equals("date-time")) {
      return TypeFormat.DATE;
    }
    if (format.equals("double") || format.equals("int32")) {
      return TypeFormat.NUMBER;
    }
    if (isValidUuid(value.asText())) {
      return TypeFormat.UUID;
    }
    return TypeFormat.STRING;
  }
}
