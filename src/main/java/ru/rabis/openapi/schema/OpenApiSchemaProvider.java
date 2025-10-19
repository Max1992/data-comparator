package ru.rabis.openapi.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class OpenApiSchemaProvider {
  private final Logger log = LoggerFactory.getLogger(OpenApiSchemaProvider.class);
  private final Map<String, String> moduleDigestByModuleName = new ConcurrentHashMap<>();
  private final Map<String, JsonNode> schemaByModuleName = new ConcurrentHashMap<>();

  @Value("${rabis.openapi.schema.url}")
  private String openApiSchemaUrl;
  @Value("${rabis.data.provider.url}")
  private String providerUrl;
  @Value("${rabis.openapi.path.schema:/api/v1/dataProvider/generateOpenAPISpec/}")
  private String schemaPath;
  @Value("${rabis.openapi.path.modules:/api/v1/dataProvider/getModules}")
  private String modulesPath;
  @Value("${rabis.openapi.path.digest:/api/v1/dataProvider/getModuleDigest/}")
  private String digestPath;

  private final RestTemplate restTemplate;

  public OpenApiSchemaProvider(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public void updateSchemas() {
    log.debug("Updating modules list");

    ResponseEntity<List> response = restTemplate.exchange(getModulesUrl(),
        HttpMethod.GET, new HttpEntity<>(""),
        List.class
    );

    log.debug("Got modules: {}", response.getBody());
    var newNames = new HashSet<String>(response.getBody());
    for (var name : newNames) {
      moduleDigestByModuleName.put(name, "unknown-digest");
    }
    moduleDigestByModuleName.keySet().retainAll(newNames);
  }

  private String getModulesUrl() {
    return openApiSchemaUrl + modulesPath;
  }

  private String getUrl(String path, String moduleName) {
    return openApiSchemaUrl + path + moduleName + "?dataProviderUrl=" + providerUrl;
  }

  public void checkModifications() {
    for (var module : moduleDigestByModuleName.keySet()) {
      checkModifications(module);
    }
  }

  private void checkModifications(String moduleName) {
    try {
      log.debug("Checks for digest update. Module: {}", moduleName);
      ResponseEntity<String> response = restTemplate.exchange(getUrl(digestPath, moduleName),
          HttpMethod.GET, new HttpEntity<>(""),
          String.class);
      if (response.getStatusCode() != HttpStatus.OK) {
        throw new Exception("Не удалось получить новую схему данных.");
      }
      var prevModuleDigest = moduleDigestByModuleName.get(moduleName);
      if (!Objects.equals(response.getBody(), prevModuleDigest)) {
        updateOpenApiSchema(moduleName);
        moduleDigestByModuleName.put(moduleName, response.getBody());
        log.info("Digest updated for module: {}", moduleName);
      } else {
        log.debug("Module {} is up to date", moduleName);
      }
    } catch (Exception e) {
      moduleDigestByModuleName.put(moduleName, "unknown-digest");
      log.error("{}", e.getMessage());
    }
  }

  private void updateOpenApiSchema(String moduleName)
      throws Exception {
    var mapper = new ObjectMapper(new YAMLFactory());
    ResponseEntity<Resource> response = restTemplate.exchange(
        getUrl(schemaPath, moduleName),
        HttpMethod.GET,
        new HttpEntity<>(""),
        Resource.class
    );
    if (response.getStatusCode() != HttpStatus.OK) {
      throw new Exception("Не удалось получить новую схему данных.");
    }
    try (var fileInputStream = response.getBody().getInputStream()) {
      var rawSchema = new String(fileInputStream.readAllBytes(), StandardCharsets.UTF_8);
      schemaByModuleName.put(moduleName, mapper.readTree(rawSchema));
    }
  }

  public JsonNode getSchemaByModuleName(String moduleName) {
    checkModuleName(moduleName);
    return schemaByModuleName.get(moduleName);
  }

  private void checkModuleName(String moduleName) {
    if (!moduleDigestByModuleName.containsKey(moduleName)) {
      throw new RuntimeException("Unsupported module: " + moduleName);
    }
  }
}
