plugins {
    java
    `java-library`
    eclipse
}

group = "io.github.nucleuspowered"

repositories {
    mavenCentral()
    maven("https://repo-new.spongepowered.org/repository/maven-public")
}

sourceSets {
    main
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

val spongeApiVersion: String by rootProject
val vavrVersion: String by rootProject

dependencies {
    api(project(":nucleus-core"))

    val dep = "org.spongepowered:spongeapi:$spongeApiVersion"
    annotationProcessor(dep)
    api(dep)

    api("io.vavr:vavr:$vavrVersion")
}

