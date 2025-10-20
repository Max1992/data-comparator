package ru.rabis.model;

public class CompareData {
  private String source;
  private String target;
  private CompareConfiguration configuration = new CompareConfiguration();

  public CompareConfiguration getConfiguration() {
    return configuration;
  }

  public void setConfiguration(CompareConfiguration configuration) {
    this.configuration = configuration;
  }

  public String getTarget() {
    return target;
  }

  public void setTarget(String target) {
    this.target = target;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }
}
