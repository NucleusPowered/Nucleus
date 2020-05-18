import io.github.nucleuspowered.gradle.enums.getLevel
import io.github.nucleuspowered.gradle.task.RelNotesTask
import io.github.nucleuspowered.gradle.task.StdOutExec
import io.github.nucleuspowered.gradle.task.UploadToGithubReleases
import io.github.nucleuspowered.gradle.task.UploadToOre
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files

val kotlin_version: String by extra
buildscript {
    var kotlin_version: String by extra
    kotlin_version = "1.3.61"
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath(kotlin("gradle-plugin", kotlin_version))
    }
}

plugins {
    java
    idea
    eclipse
    `maven-publish`
    id("com.github.hierynomus.license") version "0.15.0"
    id("ninja.miserable.blossom") version "1.0.1"
    id("com.github.johnrengelman.shadow") version "5.2.0"
    id("org.spongepowered.gradle.plugin") version "0.11.0-SNAPSHOT"
    kotlin("jvm") version "1.3.61"
}
apply {
    plugin("kotlin")
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

extra["gitHash"] = getGitCommit()

// Get the Level
val nucVersion = project.properties["nucleusVersion"]?.toString()!!
val nucSuffix : String? = project.properties["nucleusVersionSuffix"]?.toString()

var level = getLevel(nucVersion, nucSuffix)
val spongeVersion: String = project.properties["declaredApiVersion"]!!.toString()
val versionString: String = if (nucSuffix == null) {
    nucVersion
} else {
    "$nucVersion-$nucSuffix"
}
val filenameSuffix = "SpongeAPI$spongeVersion"
version = versionString

project(":nucleus-api").version = versionString
project(":nucleus-core").version = versionString

group = "io.github.nucleuspowered"

defaultTasks.add("licenseFormat")
defaultTasks.add("build")

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    jcenter()
    maven("https://repo.spongepowered.org/maven")
    maven("https://repo.drnaylor.co.uk/artifactory/list/minecraft")
    maven("https://repo.drnaylor.co.uk/artifactory/list/quickstart")
}

dependencies {
    implementation(project(":nucleus-ap"))
    implementation(project(":nucleus-api"))
    implementation(project(":nucleus-core"))

    implementation(project.properties["qsmlDep"]?.toString()!!)  {
        exclude("org.spongepowered", "configurate-core")
    }
    implementation(project.properties["neutrinoDep"]?.toString()!!)  {
        exclude("org.spongepowered", "configurate-core")
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

val cleanJars by tasks.registering {
    doLast {
        val output = project.file("output").listFiles()
        if (output != null) {
            for (x in output) {
                if (x.name.endsWith("jar")
                        || x.name.endsWith("md")
                        || x.name.endsWith("json")
                        || x.name.endsWith("yml")) {
                    x.delete()
                }
            }
        }
    }
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
    versionString { -> versionString }
    spongeVersion { -> spongeVersion }
    gitHash { -> gitHash.get().result!! }
    gitCommit { -> gitCommitMessage.get().result!! }
    level { -> level }
}

val writeRelNotes by tasks.registering {
    dependsOn(relNotes)
    doLast {
        Files.write(project.projectDir.toPath().resolve("changelogs").resolve("${nucVersion}.md"),
                relNotes.get().relNotes!!.toByteArray(StandardCharsets.UTF_8))
    }
}

val outputRelNotes by tasks.registering {
    dependsOn(relNotes)
    doLast {
        Files.write(project.projectDir.toPath().resolve("output").resolve("${nucVersion}.md"),
                relNotes.get().relNotes!!.toByteArray(StandardCharsets.UTF_8))
    }
}

val shadowJar: com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar by tasks

val upload by tasks.registering(UploadToOre::class) {
    dependsOn(shadowJar)
    dependsOn(relNotes)
    fileProvider = shadowJar.archiveFile
    notes = { relNotes.get().relNotes!! }
    releaseLevel = { level }
    apiKey = properties["ore_apikey"]?.toString() ?: System.getenv("NUCLEUS_ORE_APIKEY")
    pluginid = "nucleus"
}

val ghReleases by tasks.registering(UploadToGithubReleases::class) {
    dependsOn(shadowJar)
    dependsOn(":nucleus-api:jar")
    dependsOn(relNotes)
    version = { versionString }
    notes = { relNotes.get().relNotes!! }
    releaseLevel = { level }
    tag = properties["gh_tag"]?.toString() ?: System.getenv("NUCLEUS_GH_TAG")
    ownerName = properties["gh_owner"]?.toString() ?: System.getenv("NUCLEUS_GH_OWNER")
    repoName = properties["gh_repo"]?.toString() ?: System.getenv("NUCLEUS_GH_REPO")
    pluginFile = shadowJar.archiveFile
    apiFile = (project(":nucleus-api").tasks["jar"] as org.gradle.jvm.tasks.Jar).archiveFile
    jdFile = (project(":nucleus-api").tasks["javadocJar"] as org.gradle.jvm.tasks.Jar).archiveFile
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}

tasks {
    shadowJar {
        dependsOn(":nucleus-api:build")
        dependsOn(":nucleus-core:build")
        dependsOn(gitHash)
        doFirst {
            manifest {
                attributes["Implementation-Title"] = project.name
                attributes["SpongeAPI-Version"] = project.properties["spongeApiVersion"]
                attributes["Implementation-Version"] = project.version
                attributes["Git-Hash"] = gitHash.get().result
            }
        }

        dependencies {
            include(project(":nucleus-api"))
            include(project(":nucleus-core"))
            include(dependency(project.properties["qsmlDep"]?.toString()!!))
            include(dependency(project.properties["neutrinoDep"]?.toString()!!))
        }

        if (!project.properties.containsKey("norelocate")) {
            relocate("uk.co.drnaylor", "io.github.nucleuspowered.relocate.uk.co.drnaylor")
            relocate("io.github.nucleuspowered.neutrino", "io.github.nucleuspowered.relocate.nucleus.neutrino")
        }

        exclude("io/github/nucleuspowered/nucleus/api/NucleusAPIMod.class")
        val minecraftVersion = project.properties["minecraftversion"]
        archiveFileName.set("Nucleus-${versionString}-MC${minecraftVersion}-$filenameSuffix-plugin.jar")
    }

    build {
        dependsOn(shadowJar)
        dependsOn(copyJars)
        dependsOn(outputRelNotes)
    }

    clean {
        dependsOn(cleanJars)
    }

    compileJava {
        dependsOn(":nucleus-ap:build")
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


license {
    // ext.name = project.name

    exclude("**/*.info")
    exclude("assets/**")
    exclude("*.properties")
    exclude("*.txt")

    header = File("HEADER.txt")

    // sourceSets.addLater(ProviderFactory.provider(() -> project(":nucleus-core").sourceSets))

    ignoreFailures = false
    strictCheck = true

    mapping("java", "SLASHSTAR_STYLE")
}
