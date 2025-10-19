package ru.rabis.model;

import java.util.HashSet;
import java.util.Set;

public class CompareConfiguration {

  private Set<String> ignore = new HashSet<>();
  private boolean readonly = false;
  private boolean date = false;
  private boolean uuid = false;

  public boolean getUuid() {
    return uuid;
  }

  public void setUuid(boolean uuid) {
    this.uuid = uuid;
  }

  public boolean getDate() {
    return date;
  }

  public void setDate(boolean date) {
    this.date = date;
  }

  public boolean getReadonly() {
    return readonly;
  }

  public void setReadonly(boolean readonly) {
    this.readonly = readonly;
  }

  public Set<String> getIgnore() {
    return ignore;
  }

  public void setIgnore(Set<String> ignore) {
    this.ignore = ignore;
  }
}
