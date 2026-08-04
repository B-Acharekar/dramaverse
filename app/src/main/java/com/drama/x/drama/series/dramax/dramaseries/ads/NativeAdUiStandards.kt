package com.drama.x.drama.series.dramax.dramaseries.ads

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.ads.module.ads.ERainAd
import com.drama.x.drama.series.dramax.dramaseries.R

private const val DEFAULT_CTA_HEIGHT_DP = 40
private const val MIN_CTA_HEIGHT_DP = 36
private const val MAX_CTA_HEIGHT_DP = 52
private const val MAX_CTA_HEIGHT_WITHOUT_HIGH_GATE_DP = 46

// App color scheme
private const val APP_DARK_BACKGROUND = "#16121A"  // Dark background matching app
private const val APP_CTA_COLOR = "#FF3F59"        // Pink CTA button color
private const val APP_TEXT_COLOR = "#FFFFFF"       // White text
private const val APP_SECONDARY_TEXT = "#B0B0B0"   // Gray secondary text

object NativeAdUiStandards {
    fun apply(root: View, context: Context) {
        // Apply CTA button styling
        val cta = root.findViewById<Button>(R.id.ad_call_to_action)
        if (cta != null) {
            val requestedHeight = RemoteConfigUtils.ctaHeightDp(context)
            val allowHighCta = ERainAd.getInstance().getShouldDisplayHighCTA(true)
            val maxHeight = if (allowHighCta) {
                MAX_CTA_HEIGHT_DP
            } else {
                MAX_CTA_HEIGHT_WITHOUT_HIGH_GATE_DP
            }
            val clampedHeight = requestedHeight.coerceIn(MIN_CTA_HEIGHT_DP, maxHeight)
            val heightPx = (clampedHeight * context.resources.displayMetrics.density).toInt()
            cta.layoutParams = cta.layoutParams.apply {
                height = heightPx
            }
            cta.minHeight = 0
            cta.minimumHeight = 0
            
            // Apply app CTA color and styling
            val ctaDrawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 50f * context.resources.displayMetrics.density
                setColor(Color.parseColor(APP_CTA_COLOR))
            }
            cta.background = ctaDrawable
            cta.setTextColor(Color.parseColor(APP_TEXT_COLOR))
            cta.isAllCaps = false
            
            cta.requestLayout()
            Log.d(
                ADS_TAG,
                "CTA_HEIGHT_APPLIED requestedDp=$requestedHeight clampedDp=$clampedHeight allowHighCta=$allowHighCta color=$APP_CTA_COLOR"
            )
        }
        
        // Apply dark background theme to ad container
        applyDarkTheme(root, context)
    }
    
    /**
     * Apply dark theme styling to match app UI.
     * - Dark background (#16121A)
     * - White text for titles
     * - Gray text for descriptions
     */
    private fun applyDarkTheme(root: View, context: Context) {
        try {
            // Apply dark background to ad container
            val adContainer = root.findViewById<View>(R.id.ad_container)
            if (adContainer != null) {
                val bgDrawable = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 12f * context.resources.displayMetrics.density
                    setColor(Color.parseColor(APP_DARK_BACKGROUND))
                    setStroke(
                        (1 * context.resources.displayMetrics.density).toInt(),
                        Color.parseColor("#1AFFFFFF")
                    )
                }
                adContainer.background = bgDrawable
            }
            
            // Style text elements
            applyTextStyling(root)
            
            // Style "Ad" badge with dark theme
            val adIcon = root.findViewById<View>(R.id.ad_icon_text)
            adIcon?.setBackgroundResource(R.drawable.bg_ad_icon_dark)
            
            Log.d(ADS_TAG, "DARK_THEME_APPLIED background=$APP_DARK_BACKGROUND")
        } catch (e: Exception) {
            Log.w(ADS_TAG, "DARK_THEME_ERROR: ${e.message}")
        }
    }
    
    /**
     * Apply text color styling recursively.
     */
    private fun applyTextStyling(view: View) {
        when (view) {
            is TextView -> {
                // Headline/Title uses white
                if (view.id == R.id.ad_headline) {
                    view.setTextColor(Color.parseColor(APP_TEXT_COLOR))
                }
                // Body/Description uses gray
                else if (view.id == R.id.ad_body) {
                    view.setTextColor(Color.parseColor(APP_SECONDARY_TEXT))
                }
                // Advertiser name uses gray
                else if (view.id == R.id.ad_advertiser) {
                    view.setTextColor(Color.parseColor(APP_SECONDARY_TEXT))
                }
            }
            is ViewGroup -> {
                for (i in 0 until view.childCount) {
                    applyTextStyling(view.getChildAt(i))
                }
            }
        }
    }

    fun defaultCtaHeightDp(): Int = DEFAULT_CTA_HEIGHT_DP
}
