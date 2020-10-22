package io.github.nucleuspowered.gradle.task

import io.github.nucleuspowered.gradle.enums.ReleaseLevel
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import javax.inject.Inject

open class RelNotesTask @Inject constructor() : DefaultTask() {
    var relNotes: String? = null
    private var versionStringProvider: () -> String = { -> "" }
    private var gitHashProvider: () -> String = { -> "" }
    private var gitCommitProvider: () -> String = { -> "" }
    private var releaseLevelProvider: () -> ReleaseLevel = { -> ReleaseLevel.SNAPSHOT }

    fun versionString(provider: () -> String) {
        this.versionStringProvider = provider
    }

    fun gitHash(provider: () -> String) {
        this.gitHashProvider = provider
    }

    fun gitCommit(provider: () -> String) {
        this.gitCommitProvider = provider
    }

    fun level(provider: () -> ReleaseLevel) {
        this.releaseLevelProvider = provider
    }

    @TaskAction
    fun doTask() {
        val versionString = this.versionStringProvider.invoke()
        val level = this.releaseLevelProvider.invoke()
        val templatePath = project.projectDir.toPath()
                .resolve("changelogs")
                .resolve("templates")
                .resolve("${level.template}.md")

        // val template = File("changelogs/templates/" + level.template + ".md").getText("UTF-8")
        val notesDir = project.projectDir.toPath()
                .resolve("changelogs")
                .resolve("templates")

        val notesFull = notesDir.resolve("$versionString.md")
        val notes = if (Files.exists(notesFull)) {
            notesFull
        } else {
            notesDir.resolve("${versionString.substringBefore("-")}.md")
        }

        val templateText: String = if (Files.exists(templatePath)) {
            String(Files.readAllBytes(templatePath), StandardCharsets.UTF_8)
        } else {
            "There are no templated release notes available."
        }

        val notesText: String = if (Files.exists(notes)) {
            String(Files.readAllBytes(notes), StandardCharsets.UTF_8)
        } else {
            "There are no release notes available."
        }

        relNotes = templateText
                .replace("{{hash}}", this.gitHashProvider.invoke()) //gitHash.get().result!!
                .replace("{{info}}", notesText)
                .replace("{{version}}", project.properties["nucleusVersion"]?.toString()!!)
                .replace("{{message}}", this.gitCommitProvider.invoke()) // gitCommitMessage.get().result!!
                .replace("{{sponge}}", project.properties["declaredApiVersion"]?.toString()!!)
    }
}