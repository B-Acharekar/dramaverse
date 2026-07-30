package com.drama.x.drama.series.dramax.dramaseries.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.drama.x.drama.series.dramax.dramaseries.BuildConfig
import com.google.android.ump.ConsentInformation
import com.google.android.ump.FormError
import com.itg.iaumodule.IAdConsentCallBack
import com.itg.iaumodule.ITGAdConsent
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

private const val CONSENT_PREFS = "ad_consent"
private const val CONSENT_CONFIRMED = "confirmed"
private const val CONSENT_TIMEOUT_MS = 20_000L

object ConsentFlow {
    suspend fun ensureConsent(activity: Activity): Boolean {
        val prefs = activity.getSharedPreferences(CONSENT_PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(CONSENT_CONFIRMED, false)) return true
        if (!ITGAdConsent.isAdConsentCountry(activity)) return true

        return withTimeoutOrNull(CONSENT_TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                val callback = object : IAdConsentCallBack {
                    private var handled = false

                    private fun finish(canPersonalize: Boolean, confirmed: Boolean, reason: String) {
                        if (handled) return
                        handled = true
                        if (confirmed) {
                            prefs.edit().putBoolean(CONSENT_CONFIRMED, true).apply()
                        }
                        Log.d(ADS_TAG, "CONSENT_FLOW_FINISHED reason=$reason canPersonalize=$canPersonalize")
                        if (cont.isActive) cont.resume(canPersonalize)
                    }

                    override fun getCurrentActivity(): Activity = activity

                    override fun isDebug(): Boolean = BuildConfig.DEBUG

                    override fun isUnderAgeAd(): Boolean = false

                    override fun onNotUsingAdConsent() {
                        finish(canPersonalize = true, confirmed = true, reason = "not_using_ad_consent")
                    }

                    override fun onConsentSuccess(consentAccepted: Boolean) {
                        finish(
                            canPersonalize = consentAccepted,
                            confirmed = consentAccepted,
                            reason = "success"
                        )
                    }

                    override fun onConsentError(formError: FormError) {
                        Log.w(ADS_TAG, "CONSENT_FLOW_ERROR ${formError.message}")
                        finish(canPersonalize = true, confirmed = false, reason = "error")
                    }

                    override fun onConsentStatus(consentStatus: Int) {
                        if (consentStatus != ConsentInformation.ConsentStatus.REQUIRED) {
                            finish(canPersonalize = true, confirmed = true, reason = "not_required")
                        }
                    }

                    override fun testDeviceID(): String = ""

                    override fun onRequestShowDialog() {
                        Log.d(ADS_TAG, "CONSENT_DIALOG_REQUESTED")
                    }
                }

                Handler(Looper.getMainLooper()).post {
                    ITGAdConsent.loadAndShowConsent(true, callback)
                }
            }
        } ?: true
    }
}
