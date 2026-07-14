package com.drama.x.drama.series.dramax.dramaseries.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

internal suspend fun AuthRepository.clientToken(
    backendBaseUrl: String,
    language: String
): String = authToken()
    ?: registerDevice(backendBaseUrl, language).getOrThrow().token
    ?: throw IllegalStateException("Device auth did not return a bearer token.")

internal fun getClientJson(
    backendBaseUrl: String,
    path: String,
    language: String,
    token: String
): JSONObject {
    val url = URL("${backendBaseUrl.trimEndSlash()}/$path?language=$language")
    val connection = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        connectTimeout = 7000
        readTimeout = 7000
        setRequestProperty("Accept", "application/json")
        setRequestProperty("Authorization", "Bearer $token")
    }
    return connection.readJson(path)
}

internal fun postClientJson(
    backendBaseUrl: String,
    path: String,
    language: String,
    token: String,
    body: JSONObject = JSONObject()
): JSONObject {
    val url = URL("${backendBaseUrl.trimEndSlash()}/$path?language=$language")
    val connection = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = 7000
        readTimeout = 7000
        doOutput = true
        setRequestProperty("Accept", "application/json")
        setRequestProperty("Content-Type", "application/json")
        setRequestProperty("Authorization", "Bearer $token")
        outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
    }
    return connection.readJson(path)
}

internal fun deleteClientJson(
    backendBaseUrl: String,
    path: String,
    language: String,
    token: String
): JSONObject {
    val url = URL("${backendBaseUrl.trimEndSlash()}/$path?language=$language")
    val connection = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "DELETE"
        connectTimeout = 7000
        readTimeout = 7000
        setRequestProperty("Accept", "application/json")
        setRequestProperty("Authorization", "Bearer $token")
    }
    return connection.readJson(path)
}

private fun HttpURLConnection.readJson(path: String): JSONObject {
    val responseText = if (responseCode in 200..299) {
        inputStream.bufferedReader().use { it.readText() }
    } else {
        val error = errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
        throw IllegalStateException("$path failed: $responseCode $error")
    }
    return JSONObject(responseText)
}

private fun String.trimEndSlash(): String =
    trim().trimEnd('/').ifBlank { "https://dramaverse-backend-lbq5.onrender.com" }
