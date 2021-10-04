import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.20"
    java
    id("maven-publish")
}

repositories {
    jcenter()
    mavenCentral()
    maven { url = uri("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies") }
}

group = "org.jetbrains.intellij.deps"
version  = "0.0.${findProperty("teamcity")
    ?.takeIf { it is Map<*, *> }
    ?.let { (it as Map<*, *>)["build.number"] }
    ?: "x-SNAPSHOT"}"
val artifactID = "ap-validation"


dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.4.21")
    implementation("org.jetbrains:annotations:20.1.0")
    implementation("com.jetbrains.fus.reporting:model:55")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
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

val publicationName = "MyPublication"
publishing {
    publications.invoke {
        create<MavenPublication>(publicationName) {
            from(components["java"])
            groupId = project.group.toString()
            artifactId = artifactID
            version = project.version.toString()

            pom {
                name.set(rootProject.name)
                description.set("Library for validating statistics events before sending them to the server")
                licenses {
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                }
            }
        }
    }
    repositories {
        maven {
            url = uri("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
            credentials {
                username = findProperty("spaceClientId") as? String
                password = findProperty("spaceSecret") as? String
            }
        }
    }
}
