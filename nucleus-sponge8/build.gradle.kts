plugins {
    java
    eclipse
    id("ninja.miserable.blossom")
    id("com.github.hierynomus.license")
}

group = "io.github.nucleuspowered"

repositories {
    jcenter()
    maven("https://repo.spongepowered.org/maven")
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

    val dep = "org.spongepowered:spongeapi:" + rootProject.properties["spongeApiVersion"]
    annotationProcessor(dep)
    implementation(dep)

    implementation(rootProject.properties["qsmlDep"]?.toString()!!)
    implementation(rootProject.properties["neutrinoDep"]?.toString()!!) {
        exclude("org.spongepowered", "configurate-core")
    }

    testCompile("org.mockito:mockito-all:1.10.19")
    testCompile("org.powermock:powermock-module-junit4:1.6.4")
    testCompile("org.powermock:powermock-api-mockito:1.6.4")
    testCompile("org.hamcrest:hamcrest-junit:2.0.0.0")
    testCompile("junit", "junit", "4.12")
}