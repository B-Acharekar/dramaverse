package app.dramaverse.stream.data

import android.content.Context
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.UUID

private const val PREFS_NAME = "dramaverse_auth"
private const val KEY_DEVICE_ID = "device_id"
private const val KEY_AUTH_TOKEN = "auth_token"
private const val DEFAULT_BACKEND_URL = "https://dramaverse-backend-lbq5.onrender.com"

data class DeviceAuthResult(
    val deviceId: String,
    val token: String?
)

class AuthRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun deviceId(): String {
        prefs.getString(KEY_DEVICE_ID, null)?.let { return it }

        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ).orEmpty()
        val generated = if (androidId.isNotBlank()) {
            "android-$androidId"
        } else {
            "android-${UUID.randomUUID()}"
        }

        prefs.edit().putString(KEY_DEVICE_ID, generated).apply()
        return generated
    }

    suspend fun registerDevice(
        backendBaseUrl: String,
        language: String?
    ): Result<DeviceAuthResult> = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL("${backendBaseUrl.trimEndSlash()}/client/auth/device")
            val body = JSONObject()
                .put("device_id", deviceId())
                .put("language", language.toBackendLanguage())
                .toString()

            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 12000
                readTimeout = 12000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
            }

            connection.outputStream.use { stream ->
                stream.write(body.toByteArray(Charsets.UTF_8))
            }

            val responseText = if (connection.responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                val error = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw IllegalStateException("Device auth failed: ${connection.responseCode} $error")
            }

            val json = JSONObject(responseText)
            val token = json.optString("token").takeIf { it.isNotBlank() }
            if (token != null) {
                prefs.edit().putString(KEY_AUTH_TOKEN, token).apply()
            }

            DeviceAuthResult(
                deviceId = json.optString("device_id", deviceId()),
                token = token
            )
        }
    }

    fun authToken(): String? = prefs.getString(KEY_AUTH_TOKEN, null)
}

private fun String.trimEndSlash(): String = trim().trimEnd('/').ifBlank { DEFAULT_BACKEND_URL }

private fun String?.toBackendLanguage(): String {
    val normalized = this?.trim().orEmpty()
    return when (normalized.lowercase(Locale.US)) {
        "english" -> "en"
        "spanish" -> "es"
        "deutsch" -> "de"
        "portuguese" -> "pt"
        "turkish" -> "tr"
        "arabic" -> "ar"
        "hindi" -> "hi"
        "japanese" -> "ja"
        "korean" -> "ko"
        "chinese" -> "zh"
        else -> normalized.ifBlank { "en" }.take(8)
    }
}
