rootProject.name = "ap-validation"

pluginManagement {
    val isCommonProject:String? by settings

    if (isCommonProject != "true") {
        plugins {
            kotlin("jvm") version "1.4.21"
        }
    }
}
