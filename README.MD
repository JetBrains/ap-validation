# Library for validating statistics events before sending them to the server


### Installation

```groovy
repositories {
    maven { url = uri("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies") }
}

dependencies {
    implementation("org.jetbrains.intellij.deps:ap-validation:${version}")
}
```

### How to use

```kotlin
fun main() {
  val initialMetadata : EventGroupRemoteDescriptors  = parseMetadata("<bundled metadata>")
  val validationRuleStorage = SimpleValidationRuleStorage(initialMetadata,
                                                          buildParser = { build: String? -> EventLogBuild.fromString(build) })
  val validator = SensitiveDataValidator(validationRuleStorage)

  val event: LogEvent = newLogEvent(session = "80bb576ed123",
                                    build = "203.6682.168",
                                    bucket = "123",
                                    time = System.currentTimeMillis(),
                                    groupId = "groupId",
                                    groupVersion = "42",
                                    recorderVersion = "1",
                                    eventId = "eventId",
                                    isState = true,
                                    eventData = hashMapOf("count" to 42))
  val validatedEvent: LogEvent? = validator.validateEvent(event)

  //…
  validationRuleStorage.update(parseMetadata("<loaded from server metadata>"))
  //...
}

fun parseMetadata(metadataJson: String): EventGroupRemoteDescriptors {
  //use any library to parse json
  return Gson().fromJson(metadataJson, EventGroupRemoteDescriptors::class.java)
}
```

#### Validation rules

There are 3 types of validation rules:

* Enums, e.g `{enum:succeed|failed}` checks that the value is equal to ‘succeed’ or ‘failed’.
* Regexp e.g `{regexp:-?\\d+}` or `{regexp#integer}` checks that the value is integer.
* Custom rule. When there are too many possible values or they are dynamically generated (e.g. action id or file type)
  both previous approaches will be not applicable. In this case, we can validate the data with a java code,
  e.g `{util#class_name}`checks that the value is a class name from platform, JB plugin or a plugin from JB plugin
  repository with a java code. To implement it, create a class which inherits UtilValidationRule. Make sure that your
  UtilRuleProducer is able to create new validation rule.