package com.drama.x.drama.series.dramax.dramaseries.data

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object NetworkUtil {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)  // Optimized for better performance
        .readTimeout(12, TimeUnit.SECONDS)     // Slightly longer for video content
        .writeTimeout(10, TimeUnit.SECONDS)    // Standard write timeout
        .build()

    fun getJsonSync(
        url: String,
        token: String? = null,
        timeoutMillis: Int = 10000,  // Optimized to 10s for better performance
        maxRetries: Int = 4          // Increased retries to compensate for shorter timeout
    ): JSONObject {
        var lastException: Exception? = null
        
        repeat(maxRetries + 1) { attempt ->
            try {
                val request = Request.Builder()
                    .url(url)
                    .apply {
                        if (token != null) {
                            header("Authorization", "Bearer $token")
                        }
                    }
                    .header("Accept", "application/json")
                    .header("User-Agent", "DramaVerse-Android/1.0")
                    .build()

                val response = httpClient.newCall(request).execute()
                return try {
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string() ?: "{}"
                        if (responseBody.isBlank()) {
                            throw IllegalStateException("Empty response body")
                        }
                        JSONObject(responseBody)
                    } else {
                        val errorBody = response.body?.string().orEmpty()
                        throw IllegalStateException("HTTP ${response.code}: $errorBody")
                    }
                } finally {
                    response.close()
                }
            } catch (e: Exception) {
                lastException = e
                android.util.Log.w("NetworkUtil", "Request attempt ${attempt + 1} failed for $url: ${e.message}")
                
                // Don't retry on the last attempt
                if (attempt < maxRetries) {
                    try {
                        // Progressive backoff: 1s, 2s, 3s
                        Thread.sleep(1000L * (attempt + 1))
                    } catch (ie: InterruptedException) {
                        Thread.currentThread().interrupt()
                        throw lastException ?: Exception("Request interrupted")
                    }
                }
            }
        }
        
        android.util.Log.e("NetworkUtil", "All ${maxRetries + 1} attempts failed for $url", lastException)
        throw lastException ?: Exception("Request failed after ${maxRetries + 1} attempts")
    }

    fun postJsonSync(
        url: String,
        body: String? = null,
        token: String? = null,
        maxRetries: Int = 2
    ): JSONObject {
        var lastException: Exception? = null
        
        repeat(maxRetries + 1) { attempt ->
            try {
                val request = Request.Builder()
                    .url(url)
                    .post((body ?: "{}").toRequestBody("application/json".toMediaType()))
                    .apply {
                        if (token != null) {
                            header("Authorization", "Bearer $token")
                        }
                    }
                    .header("Accept", "application/json")
                    .header("User-Agent", "DramaVerse-Android/1.0")
                    .build()

                val response = httpClient.newCall(request).execute()
                return try {
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string() ?: "{}"
                        if (responseBody.isBlank()) {
                            JSONObject()
                        } else {
                            JSONObject(responseBody)
                        }
                    } else {
                        val errorBody = response.body?.string().orEmpty()
                        throw IllegalStateException("HTTP ${response.code}: $errorBody")
                    }
                } finally {
                    response.close()
                }
            } catch (e: Exception) {
                lastException = e
                android.util.Log.w("NetworkUtil", "POST attempt ${attempt + 1} failed for $url: ${e.message}")
                
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(1000L * (attempt + 1))
                    } catch (ie: InterruptedException) {
                        Thread.currentThread().interrupt()
                        throw lastException ?: Exception("POST request interrupted")
                    }
                }
            }
        }
        
        android.util.Log.e("NetworkUtil", "All ${maxRetries + 1} POST attempts failed for $url", lastException)
        throw lastException ?: Exception("POST request failed after ${maxRetries + 1} attempts")
    }
}