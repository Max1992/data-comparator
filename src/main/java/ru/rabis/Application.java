package ru.rabis;

import java.io.IOException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

  public static void main(String[] args) throws IOException {

//    ObjectMapper jsonMapper = new ObjectMapper();
//    JsonNode json1 = jsonMapper.readTree(new File("00004.json"));
//    JsonNode json2 = jsonMapper.readTree(new File("00005.json"));
//    CompareData data = new CompareData();
//    data.setSource(json1.toString());
//    data.setTarget(json2.toString());
//    String s = jsonMapper.writeValueAsString(data);

    SpringApplication.run(Application.class, args);
  }
}