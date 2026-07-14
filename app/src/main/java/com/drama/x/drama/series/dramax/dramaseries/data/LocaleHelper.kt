package com.drama.x.drama.series.dramax.dramaseries.data

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import java.util.Locale

private const val PREFS_NAME = "dramaverse_locale"
private const val KEY_LANGUAGE_CODE = "language_code"
private const val KEY_LANGUAGE_NAME = "language_name"
private const val KEY_PENDING_STEP = "pending_step_after_recreate"

object LocaleHelper {

    // These labels are persisted from the language screen; keep them stable across releases.
    private val languageCodes = mapOf(
        "English" to "en",
        "Spanish" to "es",
        "Deutsch" to "de",
        "Portuguese" to "pt",
        "Turkish" to "tr",
        "Arabic" to "ar",
        "Hindi" to "hi",
        "Japanese" to "ja",
        "Korean" to "ko",
        "Chinese" to "zh"
    )

    fun codeFor(languageName: String): String = languageCodes[languageName] ?: "en"

    fun persistedLanguageName(context: Context): String? =
        prefs(context).getString(KEY_LANGUAGE_NAME, null)

    fun persistedLanguageCode(context: Context): String =
        prefs(context).getString(KEY_LANGUAGE_CODE, null) ?: "en"

    fun persistLanguage(context: Context, languageName: String) {
        prefs(context).edit()
            .putString(KEY_LANGUAGE_NAME, languageName)
            .putString(KEY_LANGUAGE_CODE, codeFor(languageName))
            .apply()
    }

    fun persistPendingStep(context: Context, stepName: String) {
        prefs(context).edit().putString(KEY_PENDING_STEP, stepName).apply()
    }

    fun consumePendingStep(context: Context): String? {
        val step = prefs(context).getString(KEY_PENDING_STEP, null)
        if (step != null) {
            prefs(context).edit().remove(KEY_PENDING_STEP).apply()
        }
        return step
    }

    fun wrap(context: Context): Context {
        val code = persistedLanguageCode(context)
        return applyLocale(context, code)
    }

    private fun applyLocale(context: Context, languageCode: String): Context {
        // Wrapping the base context makes Compose resource lookups use the persisted locale.
        val locale = Locale.forLanguageTag(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)

        return ContextWrapper(context.createConfigurationContext(config))
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
