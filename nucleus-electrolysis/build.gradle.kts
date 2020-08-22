plugins {
    java
    `java-library`
}

group = "io.github.nucleuspowered"
version = rootProject.version

repositories {
    mavenCentral()
    maven("https://repo-new.spongepowered.org/repository/maven-public")
}

dependencies {
    testCompile("junit", "junit", "4.12")
    implementation("net.kyori:adventure-api:4.0.0-SNAPSHOT")
}
