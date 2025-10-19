package ru.rabis.model;

import java.util.List;

public class ResponseResult {

  private int count;
  private String message;
  private List<String> diffs;
  private CompareConfiguration configuration;

  public CompareConfiguration getConfiguration() {
    return configuration;
  }

  public void setConfiguration(CompareConfiguration configuration) {
    this.configuration = configuration;
  }

  public int getCount() {
    return count;
  }

  public void setCount(int count) {
    this.count = count;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public List<String> getDiffs() {
    return diffs;
  }

  public void setDiffs(List<String> diffs) {
    this.diffs = diffs;
  }
}
