package ru.rabis.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
    StringBuilder sb = new StringBuilder();
    composite.forEach(a -> {
      if (sb.length() > 1) {
        sb.append("|");
      }
      sb.append(a.toString());
    });
    return sb.toString();
  }
}
