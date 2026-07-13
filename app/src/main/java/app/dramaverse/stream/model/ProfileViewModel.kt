package app.dramaverse.stream.model

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.dramaverse.stream.data.AuthRepository
import app.dramaverse.stream.data.LocaleHelper
import app.dramaverse.stream.data.ProfileRepository
import app.dramaverse.stream.data.ProfileSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "ProfileViewModel"
private const val PREFS_NAME = "dramaverse_profile_media"
private const val KEY_BANNER_PATH = "banner_path"
private const val KEY_BANNER_PRESET = "banner_preset"
private const val KEY_AVATAR_PATH = "avatar_path"
private const val KEY_AVATAR_PRESET = "avatar_preset"
private const val BANNER_FILE_NAME = "profile_banner.jpg"
private const val AVATAR_FILE_NAME = "profile_avatar.jpg"

/**
 * Holds the user's chosen banner/avatar. Gallery picks are copied into the
 * app's private files directory (so they survive independent of any
 * transient content:// Uri permission) and the resulting path — along with
 * preset selections — is persisted to SharedPreferences so it survives
 * process death and app restarts.
 */
data class ProfileMediaState(
    val bannerFilePath: String? = null,
    val bannerPresetIndex: Int? = null,
    val avatarFilePath: String? = null,
    val avatarPresetIndex: Int? = null,
    val isSavingBanner: Boolean = false,
    val isSavingAvatar: Boolean = false
)

data class ProfileUiState(
    val isLoading: Boolean = true,
    val summary: ProfileSummary? = null,
    val errorMessage: String? = null
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val profileRepository = ProfileRepository(appContext, AuthRepository(appContext))

    private val _mediaState = MutableStateFlow(loadPersistedState())
    val mediaState: StateFlow<ProfileMediaState> = _mediaState.asStateFlow()
    private val _uiState = MutableStateFlow(ProfileUiState(summary = fallbackSummary()))
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun loadProfile(backendBaseUrl: String) {
        val language = LocaleHelper.persistedLanguageCode(appContext)
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            profileRepository.loadProfile(backendBaseUrl, language)
                .onSuccess { summary ->
                    _uiState.update { it.copy(isLoading = false, summary = summary, errorMessage = null) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            summary = it.summary ?: fallbackSummary(),
                            errorMessage = error.message ?: "Unable to sync profile."
                        )
                    }
                }
        }
    }

    private fun fallbackSummary(): ProfileSummary {
        val deviceId = AuthRepository(appContext).deviceId()
        return ProfileSummary(
            userName = "Guest ${deviceId.takeLast(6)}",
            guestId = deviceId,
            deviceId = deviceId,
            avatarUrl = null,
            coins = 0,
            hoursWatched = 0,
            minutesWatched = 0,
            episodesWatched = 0
        )
    }

    private fun loadPersistedState(): ProfileMediaState {
        val bannerPath = prefs.getString(KEY_BANNER_PATH, null)?.takeIf { File(it).exists() }
        val avatarPath = prefs.getString(KEY_AVATAR_PATH, null)?.takeIf { File(it).exists() }
        val bannerPreset = prefs.getInt(KEY_BANNER_PRESET, -1).takeIf { it >= 0 }
        val avatarPreset = prefs.getInt(KEY_AVATAR_PRESET, -1).takeIf { it >= 0 }
        return ProfileMediaState(
            bannerFilePath = bannerPath,
            bannerPresetIndex = bannerPreset,
            avatarFilePath = avatarPath,
            avatarPresetIndex = avatarPreset
        )
    }

    /** Persist a gallery-picked banner image. Copies the bytes so it survives beyond the picker's Uri grant. */
    fun setBannerFromUri(uri: Uri) {
        _mediaState.update { it.copy(isSavingBanner = true) }
        viewModelScope.launch {
            val savedPath = withContext(Dispatchers.IO) { copyUriToInternalFile(uri, uniqueMediaFileName("profile_banner")) }
            if (savedPath != null) {
                prefs.edit()
                    .putString(KEY_BANNER_PATH, savedPath)
                    .remove(KEY_BANNER_PRESET)
                    .apply()
                _mediaState.update {
                    it.copy(bannerFilePath = savedPath, bannerPresetIndex = null, isSavingBanner = false)
                }
            } else {
                Log.w(TAG, "Failed to save banner image from $uri")
                _mediaState.update { it.copy(isSavingBanner = false) }
            }
        }
    }

    /** Persist a gallery-picked avatar image. Copies the bytes so it survives beyond the picker's Uri grant. */
    fun setAvatarFromUri(uri: Uri) {
        _mediaState.update { it.copy(isSavingAvatar = true) }
        viewModelScope.launch {
            val savedPath = withContext(Dispatchers.IO) { copyUriToInternalFile(uri, uniqueMediaFileName("profile_avatar")) }
            if (savedPath != null) {
                prefs.edit()
                    .putString(KEY_AVATAR_PATH, savedPath)
                    .remove(KEY_AVATAR_PRESET)
                    .apply()
                _mediaState.update {
                    it.copy(avatarFilePath = savedPath, avatarPresetIndex = null, isSavingAvatar = false)
                }
            } else {
                Log.w(TAG, "Failed to save avatar image from $uri")
                _mediaState.update { it.copy(isSavingAvatar = false) }
            }
        }
    }

    fun setBannerPreset(index: Int) {
        prefs.edit()
            .putInt(KEY_BANNER_PRESET, index)
            .remove(KEY_BANNER_PATH)
            .apply()
        deleteFileQuietly(BANNER_FILE_NAME)
        _mediaState.update { it.copy(bannerPresetIndex = index, bannerFilePath = null) }
    }

    fun setAvatarPreset(index: Int) {
        prefs.edit()
            .putInt(KEY_AVATAR_PRESET, index)
            .remove(KEY_AVATAR_PATH)
            .apply()
        deleteFileQuietly(AVATAR_FILE_NAME)
        _mediaState.update { it.copy(avatarPresetIndex = index, avatarFilePath = null) }
    }

    private fun copyUriToInternalFile(uri: Uri, fileName: String): String? {
        return runCatching {
            val outFile = File(appContext.filesDir, fileName)
            val input = appContext.contentResolver.openInputStream(uri) ?: return null
            input.use { stream ->
                outFile.outputStream().use { output -> stream.copyTo(output) }
            }
            outFile.absolutePath
        }.onFailure {
            Log.w(TAG, "copyUriToInternalFile failed", it)
        }.getOrNull()
    }

    private fun deleteFileQuietly(fileName: String) {
        runCatching { File(appContext.filesDir, fileName).delete() }
    }

    private fun uniqueMediaFileName(prefix: String): String = "$prefix-${System.currentTimeMillis()}.jpg"
    fun openEditProfile() {
        Log.d(TAG, "openEditProfile: not yet implemented")
    }

    fun openWatchHistory() {
        Log.d(TAG, "openWatchHistory: not yet implemented")
    }

    fun openWatchlist() {
        Log.d(TAG, "openWatchlist: not yet implemented")
    }

    fun openLanguageSettings() {
        Log.d(TAG, "openLanguageSettings: not yet implemented")
    }

    fun openSettings() {
        Log.d(TAG, "openSettings: not yet implemented")
    }

    fun openHelpCenter() {
        Log.d(TAG, "openHelpCenter: not yet implemented")
    }

    fun openRateUs() {
        Log.d(TAG, "openRateUs: not yet implemented")
    }

    fun openPrivacyPolicy() {
        Log.d(TAG, "openPrivacyPolicy: not yet implemented")
    }

    fun openSubscription() {
        Log.d(TAG, "openSubscription: not yet implemented")
    }

    fun openWallet() {
        Log.d(TAG, "openWallet: not yet implemented")
    }

    fun openDownloads() {
        Log.d(TAG, "openDownloads: not yet implemented")
    }
}
