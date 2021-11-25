import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val isCommonProject = findProperty("is.common.project") == "true"

plugins {
    kotlin("jvm")
    java
}

repositories {
    mavenCentral()
    if (!isCommonProject) {
        maven { url = uri("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies") }
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.4.21")
    implementation("org.jetbrains:annotations:20.1.0")
    if (!isCommonProject) {
        implementation("com.jetbrains.fus.reporting:model:60")
    } else {
        implementation(project(":projects:model"))
    }
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.4.21")
    testImplementation("com.google.code.gson:gson:2.8.6")
    testImplementation("junit:junit:4.12")
}


val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}

java {
    targetCompatibility = JavaVersion.VERSION_1_8
    sourceCompatibility = JavaVersion.VERSION_1_8
    withSourcesJar()
    withJavadocJar()
}