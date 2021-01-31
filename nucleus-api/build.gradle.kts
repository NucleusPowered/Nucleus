plugins {
    java
    idea
    eclipse
    maven
    `maven-publish`
    id("com.github.hierynomus.license")
    id("ninja.miserable.blossom")
}

description = "The Ultimate Essentials Plugin API."

group = "io.github.nucleuspowered"

defaultTasks.add("licenseFormat")
defaultTasks.add("build")

repositories {
    jcenter()
    maven("https://repo.spongepowered.org/maven")
}

dependencies {
    implementation("org.spongepowered:spongeapi:" + rootProject.properties["spongeApiVersion"])
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

val filenameSuffix = "SpongeAPI${rootProject.properties["declaredApiVersion"]}"

val sourcesJar by tasks.registering(Jar::class) {
    dependsOn.add(JavaPlugin.CLASSES_TASK_NAME)
    archiveClassifier.set("sources")
    archiveFileName.set("Nucleus-${rootProject.version}-$filenameSuffix-API-sources.jar")
    from(sourceSets["main"].allSource)
}

val javadocJar by tasks.registering(Jar::class) {
    dependsOn.add(JavaPlugin.JAVADOC_TASK_NAME)
    archiveClassifier.set("javadoc")
    archiveFileName.set("Nucleus-${rootProject.version}-$filenameSuffix-API-javadocs.jar")
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

    blossomSourceReplacementJava {
        dependsOn(rootProject.tasks["gitHash"])
    }

    jar {
        manifest {
            attributes["API-Title"] = project.name
            attributes["Implementation-Title"] = rootProject.name
            attributes["API-Version"] = project.version
            attributes["Implementation-Version"] = rootProject.version
            attributes["Git-Hash"] = project.name

        }

        archiveVersion.set("${rootProject.version}")
        archiveFileName.set("Nucleus-${rootProject.version}-$filenameSuffix-API.jar")
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

blossom {
    replaceTokenIn("src/main/java/io/github/nucleuspowered/nucleus/api/NucleusAPITokens.java")
    replaceToken("@version@", version)
    replaceToken("@description@", description)
    replaceToken("@gitHash@", rootProject.extra["gitHash"])
}

artifacts {
    archives(javadocJar)
    archives(sourcesJar)
    archives(tasks.jar)
}
