buildscript {
    var kotlin_version: String by extra
    kotlin_version = "1.3.61"
    repositories {
        jcenter()
        gradlePluginPortal()
    }
    dependencies {
        classpath(kotlin("gradle-plugin", kotlin_version))
        classpath("org.apache.httpcomponents:httpmime:4.5.3")
        classpath("com.google.code.gson:gson:2.8.6")
    }
}
plugins {
    java
    `kotlin-dsl`
}

apply(plugin = "org.jetbrains.kotlin.jvm")

repositories {
    jcenter()
}

dependencies {
    gradleApi()
    implementation(kotlin("stdlib-jdk8", "1.3.61"))
    implementation("org.apache.httpcomponents:httpmime:4.5.3")
    implementation("com.google.code.gson:gson:2.8.6")
}

val compileKotlin: org.jetbrains.kotlin.gradle.tasks.KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: org.jetbrains.kotlin.gradle.tasks.KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}