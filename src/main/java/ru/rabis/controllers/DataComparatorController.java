package ru.rabis.controllers;

import static ru.rabis.utils.Utils.isValidUuid;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import ru.rabis.model.CompareData;
import ru.rabis.model.ResponseResult;
import ru.rabis.openapi.schema.OpenApiSchemaProvider;
import ru.rabis.services.DataProviderService;
import ru.rabis.services.ComparatorService;
import ru.rabis.services.CompareConfigurationService;

@Controller
@RequestMapping("/api/v1")
public class DataComparatorController {
  private final Logger log = LoggerFactory.getLogger(DataComparatorController.class);

  private final OpenApiSchemaProvider openApiSchemaProvider;
  private final DataProviderService dataProviderService;

  public DataComparatorController(OpenApiSchemaProvider openApiSchemaProvider,
      DataProviderService dataProviderService) {
    this.openApiSchemaProvider = openApiSchemaProvider;
    this.dataProviderService = dataProviderService;
  }

  @ApiResponses(value = {
      @ApiResponse(responseCode = "200",
          description = "Ok",
          content = { @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ResponseResult.class)) }),
      @ApiResponse(responseCode = "400",
          description = "Bad Request",
          content = { @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ResponseResult.class)) })
  })
  @RequestMapping(
      method = RequestMethod.POST,
      value = "/{module}/{schemaName}",
      produces = { "application/json;charset=UTF-8" },
      consumes = { "application/json;charset=UTF-8" })
  ResponseEntity<ResponseResult> createDataObject(
      @PathVariable("module") String module,
      @PathVariable("schemaName") String schemaName,
      @RequestBody String objectsToCompare) {
    try {
      var start = Instant.now();
      CompareData compareData = new ObjectMapper(new JsonFactory())
          .readValue(objectsToCompare, CompareData.class);
      JsonNode specNode = openApiSchemaProvider.getSchemaByModuleName(module);
      JsonNode schemaNode = specNode.path("components").path("schemas").path(schemaName);
      if (schemaNode.isMissingNode()) {
        log.info("Схема '{}' не найдена!", schemaName);
        return ResponseEntity.noContent().build();
      }

      String source = getDataObject(compareData.getSource(), module, schemaName);
      String target = getDataObject(compareData.getTarget(), module, schemaName);

      ComparatorService comparatorService = new ComparatorService(
          new CompareConfigurationService(compareData.getConfiguration())
      );
      var diffs = comparatorService.compare(source, target, specNode, schemaNode);
      var finishFormula = Instant.now();
      log.info("|Сравнение данных| {} | {} |Time elapsed| {}", module, schemaName,
          Duration.between(start, finishFormula).toMillis());
      ResponseResult result = new ResponseResult();
      result.setDiffs(diffs);
      result.setMessage(
          diffs.isEmpty() ? "JSON-объекты идентичны" : "Есть различия"
      );
      return ResponseEntity.ok(result);
    } catch (Exception exception) {
      log.error(exception.getMessage());
      ResponseResult result = new ResponseResult();
      result.setMessage(exception.getMessage());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(result);
    }
  }

  private String getDataObject(String input, String module, String schemaName)
      throws Exception {
    if (isValidUuid(input)) {
      return dataProviderService.getDataObject(input, module, schemaName);
    }
    return input;
  }
}
