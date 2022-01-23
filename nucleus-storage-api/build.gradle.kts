plugins {
    java
    `java-library`
    idea
    eclipse
    `maven-publish`
}

description = "The Ultimate Essentials Plugin Storage API."

group = "io.github.nucleuspowered"

defaultTasks.add("licenseFormat")
defaultTasks.add("build")

val spongeApiVersion: String by rootProject.properties

version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo-new.spongepowered.org/repository/maven-public")
}

dependencies {
    api("org.spongepowered:spongeapi:$spongeApiVersion")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

val filenameSuffix = "SpongeAPI${rootProject.properties["declaredApiVersion"]}"

val sourcesJar by tasks.registering(Jar::class) {
    dependsOn.add(JavaPlugin.CLASSES_TASK_NAME)
    archiveClassifier.set("sources")
    archiveFileName.set("Nucleus-${rootProject.version}-$filenameSuffix-Storage-API-sources.jar")
    from(sourceSets["main"].allSource)
}

val javadocJar by tasks.registering(Jar::class) {
    dependsOn.add(JavaPlugin.JAVADOC_TASK_NAME)
    archiveClassifier.set("javadoc")
    archiveFileName.set("Nucleus-${rootProject.version}-$filenameSuffix-Storage-API-javadocs.jar")
    from(tasks.javadoc)
}

val copyJars by tasks.registering(Copy::class) {
    dependsOn(tasks.jar)
    from(
            project.tasks.jar,
            javadocJar
    )
    into(rootProject.file("output"))
}

tasks {

    jar {
        manifest {
            attributes["API-Title"] = project.name
            attributes["Implementation-Title"] = rootProject.name
            attributes["API-Version"] = project.version
            attributes["Implementation-Version"] = rootProject.version
            attributes["Git-Hash"] = rootProject.extra["gitHash"]

        }

        archiveVersion.set("${rootProject.version}")
        archiveFileName.set("Nucleus-${rootProject.version}-$filenameSuffix-Storage-API.jar")
    }

    build {
        dependsOn(javadocJar)
    }
}

publishing {
    publications {
        create<MavenPublication>("api") {
            from(components["java"])
            setArtifacts(listOf(javadocJar.get(), sourcesJar.get(), tasks.jar.get()))
            version = "${rootProject.version}"
            groupId = rootProject.properties["groupId"]?.toString()!!
            artifactId = project.properties["artifactId"]?.toString()!!
        }
    }

    repositories {
        if (!(rootProject.version as String).contains("SNAPSHOT")) {
            maven {
                name = "GitHubPackages"
                url = uri(project.findProperty("gpr.uri") as String? ?: "${rootProject.properties["ghUri"]?.toString()!!}${System.getenv("REPO")}")
                credentials {
                    username = project.findProperty("gpr.user") as String? ?: System.getenv("USER")
                    password = project.findProperty("gpr.key") as String? ?: System.getenv("KEY")
                }
            }
        }
    }
}

artifacts {
    archives(javadocJar)
    archives(sourcesJar)
    archives(tasks.jar)
}
