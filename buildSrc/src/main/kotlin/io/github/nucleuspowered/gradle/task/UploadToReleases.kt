package io.github.nucleuspowered.gradle.task

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import io.github.nucleuspowered.gradle.enums.ReleaseLevel
import org.apache.http.entity.ContentType
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskAction
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.stream.Collectors

val GITHUB_API_URL = "https://api.github.com"

open class UploadToGithubReleases : DefaultTask() {

    var releaseToken: String? = null
    var mcVersion: () -> String = { "1.12.2" }
    var version: () -> String = { "unknown" }
    var apiFile: Provider<RegularFile>? = null
    var jdFile: Provider<RegularFile>? = null
    var pluginFile: Provider<RegularFile>? = null
    var notes: () -> String = { "" }
    var releaseLevel: () -> ReleaseLevel = { ReleaseLevel.SNAPSHOT }
    var token: String? = null
    var tag: String? = null
    var ownerName: String? = null
    var repoName: String? = null
    var force = false

    @TaskAction
    fun createRelease() {
        if (token != null && tag != null) {
            val tagToUse: String = tag!! // Kotlin is probably thinking about threads.
            val level = releaseLevel.invoke()
            if (force || !level.isSnapshot) {
                // Start by creating the release, asset is next.
                val creation = CreateRelease(
                        tagToUse,
                        "Version ${version.invoke()} (for Minecraft ${mcVersion.invoke()})",
                        notes.invoke(),
                        !level.isRelease
                )

                // POST /repos/:owner/:repo/releases
                val apiEndpoint = URL("https://api.github.com/repos/$ownerName/$repoName/releases")
                val con: HttpURLConnection = apiEndpoint.openConnection() as HttpURLConnection
                val gson = Gson()
                val uploadUrl = try {
                    con.requestMethod = "POST"
                    con.setRequestProperty("Authorization", "token $token")
                    con.setRequestProperty("Content-Type", ContentType.APPLICATION_JSON.mimeType)
                    con.setRequestProperty("User-Agent", "Nucleus/Gradle")

                    val status = con.responseCode
                    if (status != 201) {
                        throw Exception("Error creating release: $status")
                    }

                    val response = returnStringFromInputStream(con.inputStream)
                    val json = gson.fromJson(response, JsonObject::class.java)
                    json["upload_url"].asString
                } finally {
                    con.disconnect()
                }

                // POST :server/repos/:owner/:repo/releases/:release_id/assets{?name,label}
                val uploadStripped = uploadUrl.replace("{?name,label}", "")
                apiFile?.also { uploadJarFile(uploadStripped, token!!, it.get()) }
                jdFile?.also { uploadJarFile(uploadStripped, token!!, it.get()) }
                pluginFile?.also { uploadJarFile(uploadStripped, token!!, it.get()) }
            }
        }
    }

    private fun uploadJarFile(uploadStripped: String, token: String, file: RegularFile) {
        val fileName = file.asFile.nameWithoutExtension
        val toUpload = URL("$uploadStripped?name=$fileName")
        val con: HttpURLConnection = toUpload.openConnection() as HttpURLConnection
        try {
            con.requestMethod = "POST"
            con.setRequestProperty("Authorization", "token $token")
            con.setRequestProperty("Content-Type", "application/java-archive")
            con.setRequestProperty("User-Agent", "Nucleus/Gradle")
            con.doOutput = true
            con.outputStream.use {
                it.write(file.asFile.readBytes())
            }

            val status = con.responseCode
            if (status != 201) {
                throw Exception("Error creating release: $status")
            }
        } finally {
            con.disconnect()
        }
    }

    private fun returnStringFromInputStream(inputStream: InputStream): String {
        return BufferedReader(InputStreamReader(inputStream)).use {
            it.lines().collect(Collectors.joining(System.lineSeparator()))
        }
    }

}

data class CreateRelease(
        @field:SerializedName("tag_name") val tag_name: String,
        @field:SerializedName("name") val name: String,
        @field:SerializedName("body") val body: String,
        @field:SerializedName("prerelease") val prerelease: Boolean
)