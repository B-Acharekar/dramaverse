import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.gradle.api.tasks.Copy

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.drama.x.drama.series.dramax.dramaseries"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.drama.x.drama.series.dramax.dramaseries"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true
        manifestPlaceholders["ad_app_id"] = "ca-app-pub-3940256099942544~3347511713"

        buildConfigField("String", "ERAIN_STUDIO_VERSION", "\"1.8\"")
        buildConfigField("String", "PLAY_SERVICES_ADS_VERSION", "\"25.3.0\"")
        buildConfigField("String", "GDPR_MODULE_VERSION", "\"not_configured\"")
        buildConfigField("boolean", "FORCE_LOCAL_AD_CONFIG", "false")
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
            manifestPlaceholders["ad_app_id"] = "ca-app-pub-7462273888693209~7397581193"
            buildConfigField("boolean", "FORCE_LOCAL_AD_CONFIG", "false")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    bundle {
        language {
            enableSplit = false
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(platform(libs.firebase.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.firebase.config)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.extractor)
    implementation(libs.androidx.media3.ui)
    implementation(libs.onesignal)
    implementation(libs.androidx.appcompat)
    implementation(libs.play.services.ads)
    implementation(libs.androidx.multidex)
    implementation(libs.facebook.shimmer)
    implementation(libs.erain.studio)
    implementation(libs.firebase.analytics)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

val releaseApkDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
val formattedReleaseApkName = "DramaX-Short-Drama-Series_v1.0_${releaseApkDate}-release.apk"
val formattedDebugApkName = "DramaX-Short-Drama-Series_v1.0_${releaseApkDate}-debug.apk"

tasks.register("copyFormattedDebugApk", Copy::class) {
    dependsOn("assembleDebug")
    from(layout.buildDirectory.dir("outputs/apk/debug")) {
        include("*.apk")
    }
    into(layout.buildDirectory.dir("outputs/apk/debug/formatted"))
    rename(".*\\.apk", formattedDebugApkName)
}

tasks.register("copyFormattedReleaseApk", Copy::class) {
    dependsOn("assembleRelease")
    from(layout.buildDirectory.dir("outputs/apk/release")) {
        include("*.apk")
    }
    into(layout.buildDirectory.dir("outputs/apk/release/formatted"))
    rename(".*\\.apk", formattedReleaseApkName)
}
