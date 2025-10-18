package ru.rabis.openapi;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ru.rabis.openapi.schema.OpenApiSchemaProvider;

@Component
public class ApplicationRunnerImpl implements CommandLineRunner {
  private final OpenApiSchemaProvider openApiSchemaProvider;

  public ApplicationRunnerImpl(OpenApiSchemaProvider openApiSchemaProvider) {
    this.openApiSchemaProvider = openApiSchemaProvider;
  }

  @Override
  public void run(String... args) throws Exception {
    openApiSchemaProvider.updateSchemas();
    openApiSchemaProvider.checkModifications();
  }
}
