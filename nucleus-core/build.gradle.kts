plugins {
    java
    eclipse
    id("ninja.miserable.blossom")
    id("de.undercouch.download")
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

//    val dep = "org.spongepowered:spongeapi:" + rootProject.properties["spongeApiVersion"]
//    annotationProcessor(dep)
//    implementation(dep)

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

val downloadCompat by tasks.registering(de.undercouch.gradle.tasks.download.Download::class) {
    src("https://nucleuspowered.org/data/nca.json")
    dest(File(buildDir, "resources/main/assets/nucleus/compat.json"))
    onlyIfModified(true)
}

tasks {

    blossomSourceReplacementJava {
        dependsOn(rootProject.tasks["gitHash"])
    }

}

blossom {
    replaceTokenIn("src/main/java/io/github/nucleuspowered/nucleus/NucleusPluginInfo.java")
    replaceToken("@name@", rootProject.name)
    replaceToken("@version@", version)

    replaceToken("@description@", rootProject.properties["description"])
    replaceToken("@url@", rootProject.properties["url"])
    replaceToken("@gitHash@", rootProject.extra["gitHash"])

    replaceToken("@spongeversion@", rootProject.properties["declaredApiVersion"]) //declaredApiVersion
}

configure<nl.javadude.gradle.plugins.license.LicenseExtension> {
    val name: String = rootProject.name

    exclude("**/*.info")
    exclude("assets/**")
    exclude("*.properties")
    exclude("*.txt")

    header = file("../HEADER.txt")
    sourceSets = project.sourceSets

    ignoreFailures = false
    strictCheck = true

    mapping("java", "SLASHSTAR_STYLE")
}