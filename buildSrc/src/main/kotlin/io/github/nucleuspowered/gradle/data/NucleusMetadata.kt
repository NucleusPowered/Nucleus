package io.github.nucleuspowered.gradle.data

data class SpongePlugin(val id: String, val displayName: String, val entrypoint: String, val description: String, val version: String, val links: Links, val contributors: List<Contributor>, val dependencies: List<Dependency>)

data class Links(val homepage: String, val source: String, val issues: String)

data class Contributor(val name: String, val description: String)

data class Dependency(val id: String, val loadOrder: String, val version: String, val optional: Boolean) {
    companion object Dependencies {
        fun sponge(apiVersion: String): Dependency {
            return Dependency("spongeapi", "after", apiVersion, false)
        }
    }
}
