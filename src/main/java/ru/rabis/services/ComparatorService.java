package ru.rabis.services;

import static ru.rabis.utils.Utils.getTypeFormat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import ru.rabis.model.CompareEntry;
import ru.rabis.model.EntryKey;
import ru.rabis.model.TypeFormat;

public class ComparatorService {

  private final CompareConfigurationService configuration;
  ObjectMapper mapper = new ObjectMapper();
  private Map<EntryKey, ArrayNode> groupedMap1;

  public ComparatorService(CompareConfigurationService configuration) {
    this.configuration = configuration;
  }

  public List<CompareEntry> compare(
      final String source,
      final String target,
      JsonNode specNode,
      JsonNode schemaNode)
      throws JsonProcessingException {

    final JsonNode json1 = mapper.readTree(source);
    final JsonNode json2 = mapper.readTree(target);
    List<CompareEntry> diffs = compareJson(json1, json2, schemaNode, specNode, "");

    return diffs;
  }

  private List<CompareEntry> compareJson(
      JsonNode json1,
      JsonNode json2,
      JsonNode schemaNode,
      JsonNode specNode,
      String path
  ) {
    List<CompareEntry> diffs = new ArrayList<>();
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

      if (!value1.isMissingNode() && value2.isMissingNode()) {
        diffs.addAll(handleMissing(value1, value2, fieldSchema, specNode, currentPath));
      } else if (value1.isObject() /*&& fieldSchema.path("type").asText("").equals("object")*/) {
        diffs.addAll(handleObject(value1, value2, fieldSchema, specNode, currentPath));
      } else if (value1.isArray() && isArrayOfObjects(fieldSchema)) {
        diffs.addAll(handleArray(value1, value2, fieldSchema, specNode, currentPath));
      } else {
        diffs.addAll(handleValue(value1, value2, fieldSchema, specNode, currentPath));
      }
    }
    return diffs;
  }

  private Collection<CompareEntry> handleMissing(JsonNode value1, JsonNode value2,
      JsonNode fieldSchema, JsonNode specNode, String currentPath) {
    List<CompareEntry> diffs = new ArrayList<>();

    if (value2.isMissingNode()) {
      diffs.add(CompareEntry.warning("Поле '%s': в первом JSON '%s' во втором JSON отсутствует",
          currentPath, value1));
    }

    return diffs;
  }

  private Collection<CompareEntry> compareArrays(
      JsonNode array1,
      JsonNode array2,
      JsonNode itemSchema,
      JsonNode specNode,
      String path
  ) {
    List<CompareEntry> diffs = new ArrayList<>();
    int size1 = array1.size();
    int size2 = array2.size();
    if (size1 != size2) {
      diffs.add(CompareEntry.error("Массив '%s': разное количество элементов (%d и %d)",
          path, size1, size2
      ));
      return diffs;
    }

    Map<EntryKey, List<JsonNode>> groupedMap1 = grouping(array1);
    Map<EntryKey, List<JsonNode>> groupedMap2 = grouping(array2);

    for (EntryKey item1Key : groupedMap1.keySet()) {
      if (!groupedMap2.containsKey(item1Key)) {
        diffs.add(CompareEntry.error("Не найдено значение '%s' '%s'",
                item1Key, path));
        continue;
      }

      List<JsonNode> item1 = groupedMap1.get(item1Key);
      List<JsonNode> item2 = groupedMap2.get(item1Key);

      int size = item1.size();
      if (size != 1) {
        diffs.add(CompareEntry.warning("Найдено более одного значение '%s' (%d) '%s'",
                item1Key, size, path)
        );

        continue;
      }

      String itemPath = path + "[0]";
      if (size1 > 1) {
        itemPath = path + "[" + item1Key + "]";
      }
      diffs.addAll(compareJson(item1.get(0), item2.get(0), itemSchema, specNode, itemPath));
    }
    return diffs;
  }

  private Map<EntryKey, List<JsonNode>> grouping(JsonNode array) {
    Map<EntryKey, List<JsonNode>> groupedMap = new HashMap<>();
    for (JsonNode node : array) {
      var key = configuration.getKeyComposite(node);
      groupedMap.computeIfAbsent(key, k -> new ArrayList<>()).add(node);
    }
    return groupedMap;
  }

  private Collection<CompareEntry> handleObject(JsonNode value1, JsonNode value2, JsonNode fieldSchema, JsonNode specNode, String currentPath) {
    List<CompareEntry> diffs = new ArrayList<>();
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

  private Collection<CompareEntry> handleArray(JsonNode array1, JsonNode array2, JsonNode fieldSchema, JsonNode specNode, String path) {
    List<CompareEntry> diffs = new ArrayList<>();
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

  private Collection<CompareEntry> handleValue(JsonNode value1, JsonNode value2, JsonNode fieldSchema, JsonNode specNode, String path) {
    List<CompareEntry> diffs = new ArrayList<>();
    // Простое поле или массив не объектов

    final TypeFormat format = getTypeFormat(value1, fieldSchema);
    if (!configuration.isFormat(format)) {
      return diffs;
    }

    if (!value1.equals(value2)) {
      diffs.add(CompareEntry.error("Поле '%s': в первом JSON '%s', во втором '%s'",
          path, value1, value2));
    }
    return diffs;
  }

  private boolean isArrayOfObjects(JsonNode fieldSchema) {
    return fieldSchema.path("type").asText("").equals("array");
  }
}
