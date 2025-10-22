# smc-data-loader
Проект платформы SmartCalculator реализующий сравнение расчетов приложения СмартКалькулятор:

Используют openapi для сравнения.

```json
{
  "source" : "695b4051-7e48-4dfd-b5a8-1317444392ed",
  "target" : "aadd8704-e8aa-4b95-b14c-6df422cf14f9",
  "configuration": {}
}
```
 name          | description            | example                                                                   
---------------|------------------------|---------------------------------------------------------------------------|
 source        | Источник для сравнения | uuid или json  |
 target        | Цель сравнения         | uuid или json  |
 configuration | Конфигурация           | Пример ниже                                                               |

# Пример использования



Раздел конфигурации
```json
"configuration": {
    "ignore": ["stringSequenceSearch","stringSequence","titlePIR","sequence", "typePir"],
    "readonly": false,
    "date": false,
    "uuid": false,
    "arrayKey": ["_name", "objectUuid"]
}
```

name | description                                        | example                 
--- |----------------------------------------------------|-------------------------|
ignore | Название поля которые будут проигнорирован         | ["stringSequenceSearch","stringSequence","titlePIR","sequence", "typePir"]                     |
readonly | Сравнивать readonly поля (по спецификации swagger) | false                   |
date | Сравнивать date поля (имеющие формат "date")       | false                   |
uuid | Сравнивать uuid поля (проверка по тексту)          | false                   |
arrayKey | Ключи для нахождение элемента в массиве            | ["_name", "objectUuid"] |

# Пример 

```json
curl --location 'http://localhost:8096/api/v1/Calculator/parentBpmnCalculator' \
--header 'Content-Type: application/json' \
--data '{
  "source" : "695b4051-7e48-4dfd-b5a8-1317444392ed",
  "target" : "aadd8704-e8aa-4b95-b14c-6df422cf14f9",
  "configuration":{
    "ignore": ["stringSequenceSearch","stringSequence","titlePIR","sequence", "typePir"],
    "readonly": false,
    "date": false,
    "uuid": false,
    "arrayKey": ["_name", "objectUuid"]
  }
}'
```

В адресе http://localhost:8096/api/v1/{module}/{schema} указывается модуль и схема для сравнения.

# Ответ

```json
{
    "message": "Есть различия",
    "count": 1,
    "warnings": [
        "Найдено более одного значение 'Инженерно-геофизические изыскания' (2) 'kii[0].kiiIgfi'"
    ],
    "errors": []
}
```

# Либо можно указать два файла с настройками

```json
curl --location 'http://localhost:8096/api/v1/Calculator/parentBpmnCalculator/file' \
--form 'configuration="{
  \"ignore\": [\"stringSequenceSearch\",\"stringSequence\",\"titlePIR\",\"sequence\", \"typePir\"],
  \"readonly\": false,
  \"date\": false,
  \"uuid\": false,
  \"arrayKey\": [\"_name\", \"objectUuid\"]
}";type=application/json' \
--form 'source=@"000011.json"' \
--form 'target=@"000014.json"'
```

## Используемые технологии
* Spring Boot https://spring.io/projects/spring-boot
* Spring Security https://spring.io/projects/spring-security


## Инструментарий
* **Java** JDK 17
* **IDE** IntelliJ IDEA
    * Файл с Google Сode Style для IDEA в корне проекта _intellij-java-google-style.xml_
* **Checkstyle**
    * Checkstyle запускается автоматически при сборке Maven-ом
    * В папке _tools/checkstyle_ находятся файл правил checkstyle _google_checks.xml_ соответствующий Google Java Style Guide (https://google.github.io/styleguide/javaguide.html)
    * Plugin checkstyle для IDEA: https://plugins.jetbrains.com/plugin/1065-checkstyle-idea (выбрать Checkstyle version: 8.29, использовать конфигурацию Google Checks)
* **Maven**
    * Проект содержит maven wrapper (_/.mvn/wrapper_)

## Сборка и установка
Для сборки проекта небходима только JDK 8. Проект содержит maven wrapper (/.mvn/wrapper).
В файле application.properties указать значения настроек для доступа к Camunda и Nuxeo.

* **build only**

###
    ./mvnw clean install
###

* **build and run**
###
    ./mvnw spring-boot:run
###
