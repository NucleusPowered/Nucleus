buildscript {

    val properties = java.util.Properties()
    val input = java.io.FileInputStream(file("../gradle.properties"))
    properties.load(input)
    input.close()

    val kotlinVersion: String? = properties.getProperty("kotlinVersion")
    repositories {
        gradlePluginPortal()
    }
    dependencies {
        classpath(kotlin("gradle-plugin", kotlinVersion))
        classpath("org.apache.httpcomponents:httpmime:4.5.3")
        classpath("com.google.code.gson:gson:2.8.6")
    }
}
plugins {
    java
    `kotlin-dsl`
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

apply(plugin = "org.jetbrains.kotlin.jvm")

repositories {
    mavenCentral()
}

dependencies {
    gradleApi()
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.apache.httpcomponents:httpmime:4.5.3")
    implementation("com.google.code.gson:gson:2.8.6")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}