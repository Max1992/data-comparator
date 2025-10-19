package ru.rabis.services;

import static ru.rabis.utils.Utils.isValidUuid;

import com.fasterxml.jackson.databind.JsonNode;
import ru.rabis.model.CompareConfiguration;

public class JsonCompareConfiguration {

  private final CompareConfiguration configuration;

  public JsonCompareConfiguration(CompareConfiguration configuration) {
    this.configuration = configuration;
  }

  public boolean isIgnore(String name) {
    return configuration.getIgnore().contains(name);
  }

  public boolean isReadonly(JsonNode fieldSchema) {
    return !configuration.getReadonly() && fieldSchema.path("readOnly").asBoolean(false);
  }

  public boolean isDate(JsonNode fieldSchema) {
    return !configuration.getDate() && fieldSchema.path("format").asText("").equals("date");
  }

  public boolean isUuid(JsonNode value) {
    return !configuration.getUuid() && isValidUuid(value.asText());
  }
}
