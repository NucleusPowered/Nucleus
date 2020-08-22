plugins {
    java
    eclipse
    id("ninja.miserable.blossom")
    id("com.github.hierynomus.license")
}

group = "io.github.nucleuspowered"

repositories {
    jcenter()
    maven("https://repo-new.spongepowered.org/repository/maven-public")
    maven("https://repo.drnaylor.co.uk/artifactory/list/minecraft")
    maven("https://repo.drnaylor.co.uk/artifactory/list/quickstart")
    // maven("https://jitpack.io")
}

sourceSets {
    main {
        java {
            srcDir("src/main/java")
        }
        resources {
            srcDir("src/main/resources")
            exclude("assets/nucleus/suggestions/**")
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    annotationProcessor(project(":nucleus-ap"))
    implementation(project(":nucleus-ap"))
    implementation(project(":nucleus-api"))
    implementation(project(":nucleus-core"))

}