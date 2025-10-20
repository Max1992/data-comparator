package ru.rabis.model;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Objects;

public class EntryKeyDefault implements EntryKey {
  private final JsonNode value;

  public EntryKeyDefault(final JsonNode value) {
    this.value = value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EntryKeyDefault that = (EntryKeyDefault) o;
    return Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(value);
  }

  @Override
  public String toString() {
    return value.asText();
  }
}
