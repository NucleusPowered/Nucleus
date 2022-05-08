plugins {
    `java-library`
    eclipse
    id("net.kyori.blossom")
}

repositories {
    mavenCentral()
    maven("https://repo-new.spongepowered.org/repository/maven-public")
}

sourceSets {
    main
}

val powermockJunit4: String by rootProject.properties
val powermockApiMockito: String by rootProject.properties

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    api(project(":nucleus-core"))

    val dep = "org.spongepowered:spongeapi:${rootProject.properties["spongeApiVersion"]}"
    annotationProcessor(dep)
    implementation(dep) {
        exclude("org.spongepowered", "configurate-core")
        exclude("org.spongepowered", "configurate-gson")
        exclude("org.spongepowered", "configurate-hocon")
        exclude("org.spongepowered", "configurate-yaml")
    }


}

tasks {

    blossomSourceReplacementJava {
        dependsOn(rootProject.tasks["gitHash"])
    }

    build {
        dependsOn(":nucleus-core:build")
    }

}

blossom {
    replaceTokenIn("src/main/java/io/github/nucleuspowered/nucleus/bootstrap/NucleusPluginInfo.java")
    replaceToken("@name@", rootProject.name)
    replaceToken("@version@", rootProject.version)

    replaceToken("@description@", rootProject.properties["description"])
    replaceToken("@url@", rootProject.properties["url"])
    replaceToken("@gitHash@", rootProject.extra["gitHash"])

    replaceToken("@spongeversion@", rootProject.properties["declaredApiVersion"]) //declaredApiVersion
    val r = rootProject.properties["validVersions"]
    if (r == null) {
        replaceToken("@validversions@", rootProject.properties["declaredApiVersion"]) //validVersions
    } else {
        replaceToken("@validversions@", r) //validVersions
    }
}
