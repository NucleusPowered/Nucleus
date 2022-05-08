plugins {
    `maven-publish`
}

publishing {
    repositories {
        val githubToken: String? = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
        if (githubToken != null) {
            maven {
                name = "GitHubPackages"
                url = project.uri(project.findProperty("gpr.uri") as String?
                        ?: "${rootProject.properties["ghUri"]?.toString()!!}${rootProject.properties["ghSlug"]}")
                credentials {
                    username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                    password = githubToken
                }
            }
        }

        val artifactoryToken: String? = project.findProperty("art.key") as String? ?: System.getenv("ARTIFACTORY_TOKEN")
        if (artifactoryToken != null) {
            maven {
                name = "Artifactory"
                url = project.uri(project.findProperty("art.uri") as String? ?: System.getenv("ARTIFACTORY_URL"))
                credentials {
                    username = project.findProperty("art.user") as String? ?: System.getenv("ARTIFACTORY_USER")
                    password = artifactoryToken
                }
            }
        }
    }
}