package ru.rabis.services;

import static ru.rabis.utils.Utils.isValidUuid;

import com.fasterxml.jackson.databind.JsonNode;
import ru.rabis.model.CompareConfiguration;
import ru.rabis.model.EntryKeyComposite;
import ru.rabis.model.EntryKeyValue;

public class CompareConfigurationService {

  private final CompareConfiguration configuration;

  public CompareConfigurationService(CompareConfiguration configuration) {
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

  public EntryKeyComposite getKeyComposite(JsonNode node) {
    EntryKeyComposite composite = new EntryKeyComposite();
    configuration.getArrayKey().forEach(key -> {
      JsonNode value = node.path(key);
      if (!value.isMissingNode()) {
        composite.add(new EntryKeyValue(value));
      }
    });
    return composite;
  }
}
