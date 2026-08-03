package com.drama.x.drama.series.dramax.dramaseries.data

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object NetworkUtil {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    fun getJsonSync(
        url: String,
        token: String? = null,
        timeoutMillis: Int = 10000
    ): JSONObject {
        val request = Request.Builder()
            .url(url)
            .apply {
                if (token != null) {
                    header("Authorization", "Bearer $token")
                }
            }
            .header("Accept", "application/json")
            .build()

        val response = httpClient.newCall(request).execute()
        return try {
            if (response.isSuccessful) {
                JSONObject(response.body?.string() ?: "{}")
            } else {
                throw IllegalStateException("HTTP ${response.code}: ${response.body?.string().orEmpty()}")
            }
        } finally {
            response.close()
        }
    }

    fun postJsonSync(
        url: String,
        body: String? = null,
        token: String? = null
    ): JSONObject {
        val request = Request.Builder()
            .url(url)
            .post((body ?: "{}").toRequestBody("application/json".toMediaType()))
            .apply {
                if (token != null) {
                    header("Authorization", "Bearer $token")
                }
            }
            .header("Accept", "application/json")
            .build()

        val response = httpClient.newCall(request).execute()
        return try {
            if (response.isSuccessful) {
                JSONObject(response.body?.string() ?: "{}")
            } else {
                throw IllegalStateException("HTTP ${response.code}: ${response.body?.string().orEmpty()}")
            }
        } finally {
            response.close()
        }
    }
}
