import io.github.nucleuspowered.gradle.enums.getLevel
import io.github.nucleuspowered.gradle.task.RelNotesTask
import io.github.nucleuspowered.gradle.task.StdOutExec
import org.spongepowered.gradle.plugin.config.PluginLoaders
import org.spongepowered.plugin.metadata.model.PluginDependency
import java.io.ByteArrayOutputStream
import java.net.URL
import java.nio.charset.StandardCharsets
import java.nio.file.Files

val kotlinVersion: String? by project
var kotlin_version: String by extra
kotlin_version = kotlinVersion!!
buildscript {
    val kotlinVersion: String? by project
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath(kotlin("gradle-plugin", kotlinVersion))
    }
}

plugins {
    java
    idea
    eclipse
    `maven-publish`
    // id("com.github.hierynomus.license") version "0.15.0" apply false
    id("org.cadixdev.licenser") version "0.6.1" apply false
    id("net.kyori.blossom") version "1.2.0"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("org.spongepowered.gradle.plugin") version "2.0.1"
}

// Until I can figure out how to get Blossom to accept task outputs, if at all
// this will have to do.
fun getGitCommit() : String {
    return try {
        val byteOut = ByteArrayOutputStream()
        project.exec {
            commandLine = "git rev-parse --short HEAD".split(" ")
            standardOutput = byteOut
        }
        byteOut.toString("UTF-8").trim()
    } catch (ex: Exception) {
        // ignore
        "unknown"
    }
}

val verbose = rootProject.properties["verbose-compile"] != null
allprojects {

    apply(plugin = "org.cadixdev.licenser")
    apply(plugin = "java-library")

    configure<org.cadixdev.gradle.licenser.LicenseExtension> {
        header(rootProject.file("HEADER.txt"))
        newLine(false)
        exclude("**/*.info")
        exclude("assets/**")
        exclude("*.kts")
        exclude("**/*.json")
        exclude("**/*.properties")
        exclude("*.txt")
    }

    if (verbose) {
        gradle.projectsEvaluated {
            tasks.withType(JavaCompile::class.java) {
                options.compilerArgs.add("-Xlint:unchecked")
                options.isDeprecation = true
            }
        }
    }

    dependencies {
        testImplementation("org.mockito:mockito-all:1.10.19")
        testImplementation("org.powermock:powermock-module-junit4:2.0.9")
        testImplementation("org.powermock:powermock-api-mockito:1.7.4")
        testImplementation("org.hamcrest:hamcrest-junit:2.0.0.0")
        testImplementation("junit", "junit", "4.12")
    }
}

extra["gitHash"] = getGitCommit()

// Get the Level
val nucleusVersion: String by project // = project.properties["nucleusVersion"]?.toString()!!
val nucleusVersionSuffix : String? by project // = project.properties["nucleusVersionSuffix"]?.toString()
val vavrVersion: String by project
val declaredApiVersion: String by project
val spongeApiVersion: String by project

var level = getLevel(nucleusVersion, nucleusVersionSuffix)
val spongeVersion: String = declaredApiVersion
val versionString: String = if (nucleusVersionSuffix == null) {
    nucleusVersion
} else {
    "$nucleusVersion-$nucleusVersionSuffix"
}
val filenameSuffix = "SpongeAPI$spongeVersion"
version = versionString

project(":nucleus-api").version = versionString
project(":nucleus-core").version = versionString

group = "io.github.nucleuspowered"

defaultTasks.add("licenseFormat")
defaultTasks.add("build")

sponge {
    apiVersion(spongeApiVersion)
    license("MIT")
    loader {
        name(PluginLoaders.JAVA_PLAIN)
        version("1.0")
    }
    plugin("nucleus") {
        displayName("Nucleus")
        entrypoint("io.github.nucleuspowered.nucleus.bootstrap.NucleusBootstrapper")
        description("Nucleus")
        version(versionString)
        links {
            homepage("https://nucleuspowered.org")
            source("https://github.com/NucleusPowered/Nucleus")
            issues("https://github.com/NucleusPowered/Nucleus/issues")
        }
        contributor("dualspiral") {
            description("Lead Developer")
        }
        dependency("spongeapi") {
            loadOrder(PluginDependency.LoadOrder.AFTER)
            optional(false)
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    mavenCentral()
    maven("https://repo.spongepowered.org/maven")
}

dependencies {
    implementation(project(":nucleus-api"))
    implementation(project(":nucleus-core"))
    implementation(project(":nucleus-bootstrap"))
    testImplementation("junit:junit:4.13.2")
}

/**
 * For use with Github Actions, as you probably suspected by the name of the task
 */
val setGithubActionsVersion by tasks.registering(Exec::class) {
    commandLine("echo", "::set-env name=NUCLEUS_VERSION::$version")
    if (!level.isSnapshot) {
        commandLine("echo", "::set-env name=NUCLEUS_NOT_SNAPSHOT::true")
    }
}

val gitHash by tasks.registering(StdOutExec::class)  {
    commandLine("git", "rev-parse", "--short", "HEAD")
    doLast {
        project.extra["gitHash"] = result
    }
}

val gitCommitMessage by tasks.registering(StdOutExec::class) {
    commandLine("git", "log", "-1", "--format=%B")
}

val cleanOutput by tasks.registering(Delete::class) {
    delete(fileTree("output").matching {
        include("*.jar")
        include("*.md")
        include("*.json")
        include("*.yml")
    })
}

val copyJars by tasks.registering(Copy::class) {
    dependsOn(":nucleus-api:copyJars")
    dependsOn(":nucleus-core:build")
    dependsOn(tasks.shadowJar)
    from(tasks.shadowJar)
    into(project.file("output"))
}

val relNotes by tasks.registering(RelNotesTask::class) {
    dependsOn(gitHash)
    dependsOn(gitCommitMessage)
    versionString { versionString }
    gitHash { gitHash.get().result!! }
    gitCommit { gitCommitMessage.get().result!! }
    level { level }
}

val writeRelNotes by tasks.registering {
    dependsOn(relNotes)
    doLast {
        Files.write(project.projectDir.toPath().resolve("changelogs").resolve("${nucleusVersion}.md"),
                relNotes.get().relNotes!!.toByteArray(StandardCharsets.UTF_8))
    }
}

val outputRelNotes by tasks.registering {
    dependsOn(relNotes)
    doLast {
        Files.write(project.projectDir.toPath().resolve("output").resolve("${nucleusVersion}.md"),
                relNotes.get().relNotes!!.toByteArray(StandardCharsets.UTF_8))
    }
}

val shadowJar: com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar by tasks

val upload by tasks.registering(io.github.nucleuspowered.gradle.task.UploadToOre::class) {
    dependsOn(shadowJar)
    dependsOn(relNotes)
    fileProvider = shadowJar.archiveFile
    notes = { relNotes.get().relNotes!! }
    releaseLevel = { level }
    apiKey = properties["ore_apikey"]?.toString() ?: System.getenv("NUCLEUS_ORE_APIKEY")
    pluginid = "nucleus"
}
/*
// TODO: Re-enable
val setupDocGen by tasks.registering(io.github.nucleuspowered.gradle.task.SetupServer::class) {
    dependsOn(shadowJar)
    spongeVanillaDownload = URL("https://repo.spongepowered.org/maven/org/spongepowered/spongevanilla/1.12.2-7.2.3-RC372/spongevanilla-1.12.2-7.2.3-RC372.jar")
    spongeVanillaSHA1Hash = "e72ec4bc0368cd2fc604f412eed5fa8941d7580c"
    acceptEula = true
    fileProvider = shadowJar.archiveFile
}

val runDocGen by tasks.registering(JavaExec::class) {
    dependsOn(setupDocGen)
    workingDir = File("run")
    classpath = files(File("run/sv.jar"))
    systemProperty("nucleus.docgen", "docs")
}

val copyDocGen by tasks.registering(Copy::class) {
    dependsOn(runDocGen)
    from("run/docs")
    into(project.file("output"))
}

val docGen by tasks.registering {
    dependsOn(copyDocGen)
    dependsOn(runDocGen)
    dependsOn(setupDocGen)
}

val deleteDocGenServer by tasks.registering(Delete::class) {
    delete("run")
}
 */

tasks {
    shadowJar {
        dependsOn(":nucleus-api:build")
        dependsOn(":nucleus-core:build")
        dependsOn(":nucleus-bootstrap:build")
        dependsOn(gitHash)
        doFirst {
            manifest {
                attributes["Implementation-Title"] = project.name
                attributes["SpongeAPI-Version"] = spongeApiVersion
                attributes["Implementation-Version"] = project.version
                attributes["Git-Hash"] = gitHash.get().result
            }
        }

        dependencies {
            include(project(":nucleus-api"))
            include(project(":nucleus-core"))
            include(project(":nucleus-modules"))
            include(project(":nucleus-bootstrap"))
            include(dependency("io.vavr:vavr:$vavrVersion"))
        }

        if (!project.properties.containsKey("norelocate")) {
            relocate("io.vavr", "io.github.nucleuspowered.relocate.io.vavr")
        }

        minimize {
            exclude(project(":nucleus-api"))
            exclude(project(":nucleus-core"))
            exclude(project(":nucleus-modules"))
            exclude(project(":nucleus-bootstrap"))
        }

        exclude("io/github/nucleuspowered/nucleus/api/NucleusAPIMod.class")
        val minecraftversion: String by project
        archiveFileName.set("Nucleus-${versionString}-MC${minecraftversion}-$filenameSuffix-plugin.jar")
    }

    build {
        dependsOn(shadowJar)
        dependsOn(copyJars)
        dependsOn(outputRelNotes)
    }

    clean {
        dependsOn(cleanOutput)
    }

    blossomSourceReplacementJava {
        dependsOn(gitHash)
    }

}

publishing {
    publications {
        create<MavenPublication>("core") {
            shadow.component(this)
            setArtifacts(listOf(shadowJar))
            version = versionString
            groupId = project.properties["groupId"]?.toString()!!
            artifactId = project.properties["artifactId"]?.toString()!!
        }
    }

    repositories {
        if (!versionString.contains("SNAPSHOT")) {
            maven {
                name = "GitHubPackages"
                url = uri(project.findProperty("gpr.uri") as String? ?: "${project.properties["ghUri"]?.toString()!!}${System.getenv("REPO")}")
                credentials {
                    username = project.findProperty("gpr.user") as String? ?: System.getenv("USER")
                    password = project.findProperty("gpr.key") as String? ?: System.getenv("KEY")
                }
            }
        }
    }
}

