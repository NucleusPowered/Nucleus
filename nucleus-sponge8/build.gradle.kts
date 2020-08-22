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
    maven("https://repo.spongepowered.org/maven")
    maven("https://repo.drnaylor.co.uk/artifactory/list/minecraft")
    maven("https://repo.drnaylor.co.uk/artifactory/list/quickstart")
    maven {
        name = "Sonatype Snapshots"
        setUrl("https://oss.sonatype.org/content/repositories/snapshots/")
    }
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
    implementation(project(":nucleus-electrolysis"))

    val dep = "org.spongepowered:spongeapi:" + rootProject.properties["spongeApiVersion"]
    annotationProcessor(dep) {
        exclude("org.spongepowered", "configurate-core")
    }
    implementation(dep) {
        exclude("org.spongepowered", "configurate-core")
    }

}