# Library for validating statistics events before sending them to the server


### Installation

```groovy
repositories {
    maven { url = uri("https://jetbrains.bintray.com/intellij-third-party-dependencies") }
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

fun parseMetadata(metadataJson: String) : EventGroupRemoteDescriptors {
  //use any library to parse json
  return Gson().fromJson(metadataJson, EventGroupRemoteDescriptors::class.java)
}
```