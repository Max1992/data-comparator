package ru.rabis.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class EntryKeyComposite implements EntryKey {
  private List<EntryKey> composite = new ArrayList<>();

  public EntryKeyComposite add(final EntryKey entry) {
    composite.add(entry);
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EntryKeyComposite that = (EntryKeyComposite) o;
    return Objects.equals(composite, that.composite);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(composite);
  }

  @Override
  public String toString() {
    return composite.stream()
        .map(Objects::toString)
        .collect(Collectors.joining("|"));
  }
}
