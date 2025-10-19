package ru.rabis.configuration;

import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

  @Value("${rabis.openapi.username:admin}")
  private String systemUsername;
  @Value("${rabis.openapi.password:admin}")
  private String systemPassword;

  @Bean
  public RestTemplate restTemplate() {
    return constructRestTemplate();
  }

  private RestTemplate constructRestTemplate() {
    RestTemplate restTemplate = new RestTemplate();
    var requestFactory = new HttpComponentsClientHttpRequestFactory();
    restTemplate.setRequestFactory(requestFactory);
    restTemplate.setInterceptors(List.of(basicAuthInterceptor()));
    return restTemplate;
  }

  public ClientHttpRequestInterceptor basicAuthInterceptor() {
    return (request, body, execution) -> {
      HttpHeaders headers = request.getHeaders();
      headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
      headers.add("Content-Type", "application/json");
      headers.setBasicAuth(systemUsername, systemPassword);
      return execution.execute(request, body);
    };
  }
}
