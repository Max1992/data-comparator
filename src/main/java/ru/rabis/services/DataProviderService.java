package ru.rabis.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class DataProviderService {
  @Value("${rabis.data.provider.url}")
  private String providerUrl;

  private final RestTemplate restTemplate;

  public DataProviderService(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public String getDataObject(String id, String module, String schemaName) throws Exception {
    ResponseEntity<String> response = restTemplate.exchange(getUrl(module, schemaName, id),
        HttpMethod.GET, new HttpEntity<>(""),
        String.class);
    if (response.getStatusCode() != HttpStatus.OK) {
      throw new Exception("Не удалось получить новую схему данных.");
    }
    return response.getBody();
  }

  private String getUrl(String module, String schemaName, String id) {
    return providerUrl + String.format("/api/v1/%s/%s/%s", module, schemaName, id);
  }
}
