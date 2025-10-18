package ru.rabis.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import ru.rabis.model.InputData;

@Service
public class JsonComparatorService {

  private static Set<String> fieldsToSkip = Set.of(
      "stringSequence",
      "stringSequenceSearch",
      "titlePIR"
  );

  public List<String> compare(InputData inputData, JsonNode specNode, JsonNode schemaNode)
      throws JsonProcessingException {

    ObjectMapper jsonMapper = new ObjectMapper();
    final JsonNode json1 = jsonMapper.readTree(inputData.getSource());
    final JsonNode json2 = jsonMapper.readTree(inputData.getTarget());
    List<String> diffs = compareJson(json1, json2, schemaNode, specNode, "");

    return diffs;
  }

    private static List<String> compareJson(
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

      if (isReadonlyOrDate(fieldSchema)) {
        continue;
      }

      if (fieldsToSkip.contains(fieldName)) {
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

  private static List<String> compareArrays(
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

    if (size1 > 1) {
      array1 = sortArrayByField(array1, "_name");
      array2 = sortArrayByField(array2, "_name");
    }

    for (int i = 0; i < size1; i++) {
      JsonNode item1 = array1.get(i);
      JsonNode item2 = array2.get(i);
      String itemPath = path + "[" + i + "]";
      diffs.addAll(compareJson(item1, item2, itemSchema, specNode, itemPath));
    }
    return diffs;
  }

  private static List<String> handleObject(JsonNode value1, JsonNode value2, JsonNode fieldSchema, JsonNode specNode, String currentPath) {
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

  private static List<String> handleArray(JsonNode array1, JsonNode array2, JsonNode fieldSchema, JsonNode specNode, String path) {
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

  private static List<String> handleValue(JsonNode value1, JsonNode value2, JsonNode fieldSchema, JsonNode specNode, String path) {
    List<String> diffs = new ArrayList<>();
    // Простое поле или массив не объектов

    if (isValidUuid(value1) || isValidUuid(value2)) {
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

  private static boolean isArrayOfObjects(JsonNode fieldSchema) {
    return fieldSchema.path("type").asText("").equals("array");
  }

  private static boolean isReadonlyOrDate(JsonNode fieldSchema) {
    boolean isReadonly = fieldSchema.path("readOnly").asBoolean(false);
    boolean isDate = fieldSchema.path("format").asText("").equals("date");
    return isReadonly || isDate;
  }

  public static boolean isValidUuid(JsonNode text) {
    try {
      UUID.fromString(text.asText());
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  public static JsonNode sortArrayByField(JsonNode array, String fieldName) {
    List<JsonNode> nodes = new ArrayList<>();
    array.forEach(nodes::add);

    // Сортируем по значению поля fieldName
    Collections.sort(nodes, (a, b) -> {
      String valueA = a.path(fieldName).asText();
      String valueB = b.path(fieldName).asText();
      return valueA.compareTo(valueB);
    });

    // Создаем новый ArrayNode и заполняем его отсортированными элементами
    ArrayNode sortedArray = new ObjectMapper().createArrayNode();
    nodes.forEach(sortedArray::add);

    return sortedArray;
  }
}
