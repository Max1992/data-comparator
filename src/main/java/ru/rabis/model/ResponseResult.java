package ru.rabis.model;

import java.util.ArrayList;
import java.util.List;

public class ResponseResult {

  private String message;
  private List<CompareEntry> diffs = new ArrayList<>();

  public int getCount() {
    return diffs.size();
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public List<String> getErrors() {
    return diffs.stream()
        .filter(x -> x.level().equals(CompareLevels.ERROR))
        .map(CompareEntry::message)
        .sorted()
        .toList();
  }

  public List<String> getWarnings() {
    return diffs.stream()
        .filter(x -> x.level().equals(CompareLevels.WARNING))
        .map(CompareEntry::message)
        .sorted()
        .toList();
  }

  public void setDiffs(List<CompareEntry> diffs) {
    this.diffs = diffs;
  }
}
