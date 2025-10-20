package ru.rabis.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import ru.rabis.model.EntryKeyComposite;

public class ComparatorService {

  private final CompareConfigurationService configuration;

  public ComparatorService(CompareConfigurationService configuration) {
    this.configuration = configuration;
  }

  public List<String> compare(
      final String source,
      final String target,
      JsonNode specNode,
      JsonNode schemaNode)
      throws JsonProcessingException {

    ObjectMapper jsonMapper = new ObjectMapper();
    final JsonNode json1 = jsonMapper.readTree(source);
    final JsonNode json2 = jsonMapper.readTree(target);
    List<String> diffs = compareJson(json1, json2, schemaNode, specNode, "");

    return diffs;
  }

  private List<String> compareJson(
      JsonNode json1,
      JsonNode json2,
      JsonNode schemaNode,
      JsonNode specNode,
      String path
  ) {
    List<String> diffs = new ArrayList<>();
    JsonNode properties = schemaNode.path("properties");
    Iterator<Entry<String, JsonNode>> fields = properties.fields();
    while (fields.hasNext()) {
      Map.Entry<String, JsonNode> field = fields.next();
      String fieldName = field.getKey();
      JsonNode fieldSchema = field.getValue();

      if (configuration.isReadonly(fieldSchema)) {
        continue;
      }

      if (configuration.isIgnore(fieldName)) {
        continue;
      }

      String currentPath = path.isEmpty() ? fieldName : path + "." + fieldName;

      JsonNode value1 = json1.path(fieldName);
      JsonNode value2 = json2.path(fieldName);

      if (value1.isObject() /*&& fieldSchema.path("type").asText("").equals("object")*/) {
        diffs.addAll(handleObject(value1, value2, fieldSchema, specNode, currentPath));
      } else if (value1.isArray() && isArrayOfObjects(fieldSchema)) {
        diffs.addAll(handleArray(value1, value2, fieldSchema, specNode, currentPath));
      } else {
        diffs.addAll(handleValue(value1, value2, fieldSchema, specNode, currentPath));
      }
    }
    return diffs;
  }

  private List<String> compareArrays(
      JsonNode array1,
      JsonNode array2,
      JsonNode itemSchema,
      JsonNode specNode,
      String path
  ) {
    List<String> diffs = new ArrayList<>();
    int size1 = array1.size();
    int size2 = array2.size();
    if (size1 != size2) {
      diffs.add(String.format(
          "Массив '%s': разное количество элементов (%d и %d)",
          path, size1, size2
      ));
      return diffs;
    }

    for (int i = 0; i < size1; i++) {
      JsonNode item1 = array1.get(i);

      EntryKeyComposite composite = configuration.getKeyComposite(item1);

      List<JsonNode> nodes = new ArrayList<>();
      Iterator<JsonNode> elements = array2.elements();
      while (elements.hasNext()) {
        JsonNode next = elements.next();
        EntryKeyComposite current = configuration.getKeyComposite(next);

        if (composite.equals(current)) {
          nodes.add(next);
        }
      }
      if (nodes.isEmpty()) {
        diffs.add("Не найдено значение " + composite);
        continue;
      }
      if (nodes.size() != 1) {
        diffs.add("Найдено более одного значение " + composite);
        continue;
      }

      String itemPath = path + "[" + i + "]";
      if (size1 > 1) {
        itemPath = path + "[" + composite + "]";
      }
      diffs.addAll(compareJson(item1, nodes.get(0), itemSchema, specNode, itemPath));
    }
    return diffs;
  }

  private List<String> handleObject(JsonNode value1, JsonNode value2, JsonNode fieldSchema, JsonNode specNode, String currentPath) {
    List<String> diffs = new ArrayList<>();
    String refSchemaName = fieldSchema.path("$ref").asText("");
    if (!refSchemaName.isEmpty()) {
      String nestedSchemaName = refSchemaName.split("/")[refSchemaName.split("/").length - 1];
      JsonNode nestedSchema = specNode.path("components").path("schemas").path(nestedSchemaName);
      if (!nestedSchema.isMissingNode()) {
        diffs.addAll(compareJson(value1, value2, nestedSchema, specNode, currentPath));
      }
    } else {
      diffs.addAll(compareJson(value1, value2, fieldSchema, specNode, currentPath));
    }
    return diffs;
  }

  private List<String> handleArray(JsonNode array1, JsonNode array2, JsonNode fieldSchema, JsonNode specNode, String path) {
    List<String> diffs = new ArrayList<>();
    JsonNode itemsSchema = fieldSchema.path("items");
    if (itemsSchema != null && itemsSchema.path("$ref") != null) {
      String refSchemaName = itemsSchema.path("$ref").asText("");
      String itemSchemaName = refSchemaName.split("/")[refSchemaName.split("/").length - 1];
      JsonNode itemSchema = specNode.path("components").path("schemas").path(itemSchemaName);
      if (!itemSchema.isMissingNode()) {
        diffs.addAll(compareArrays(array1, array2, itemSchema, specNode, path));
      }
    }
    return diffs;
  }

  private List<String> handleValue(JsonNode value1, JsonNode value2, JsonNode fieldSchema, JsonNode specNode, String path) {
    List<String> diffs = new ArrayList<>();
    // Простое поле или массив не объектов

    if (configuration.isUuid(value1) || configuration.isUuid(value2)) {
      return diffs;
    }

    if (configuration.isDate(fieldSchema)) {
      return diffs;
    }

    if (!value1.equals(value2)) {
      diffs.add(String.format(
          "Поле '%s': в первом JSON '%s', во втором '%s'",
          path, value1, value2
      ));
    }
    return diffs;
  }

  private boolean isArrayOfObjects(JsonNode fieldSchema) {
    return fieldSchema.path("type").asText("").equals("array");
  }
}
