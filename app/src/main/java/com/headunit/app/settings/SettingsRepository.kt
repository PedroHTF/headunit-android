package com.headunit.app.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Guarda as configurações no aparelho (SharedPreferences). */
class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    fun update(transform: (Settings) -> Settings) {
        val new = transform(_settings.value)
        save(new)
        _settings.value = new
    }

    private fun load() = Settings(
        videoWhileDriving = prefs.getBoolean(VIDEO, false),
        androidAutoTarget = decodeTarget(prefs.getString(AA_TARGET, null)),
        carPlayTarget = decodeTarget(prefs.getString(CP_TARGET, null)),
        autoOpenOnBluetooth = prefs.getBoolean(AUTO_OPEN, true),
        onlyLastDevice = prefs.getBoolean(ONLY_LAST, true),
        lastDeviceAddress = prefs.getString(LAST_DEVICE, null),
        devices = prefs.getStringSet(DEVICES, emptySet()).orEmpty()
            .mapNotNull(::decodeDevice)
            .associateBy { it.address },
        logoKind = runCatching { LogoKind.valueOf(prefs.getString(LOGO_KIND, null) ?: "") }.getOrDefault(LogoKind.DEFAULT),
        logoText = prefs.getString(LOGO_TEXT, null) ?: "HR-V",
        logoSeconds = prefs.getInt(LOGO_SECONDS, 3),
        logoOnIgnition = prefs.getBoolean(LOGO_ON_IGNITION, true),
    )

    private fun save(s: Settings) {
        prefs.edit()
            .putBoolean(VIDEO, s.videoWhileDriving)
            .putString(AA_TARGET, encodeTarget(s.androidAutoTarget))
            .putString(CP_TARGET, encodeTarget(s.carPlayTarget))
            .putBoolean(AUTO_OPEN, s.autoOpenOnBluetooth)
            .putBoolean(ONLY_LAST, s.onlyLastDevice)
            .putString(LAST_DEVICE, s.lastDeviceAddress)
            .putStringSet(DEVICES, s.devices.values.map(::encodeDevice).toSet())
            .putString(LOGO_KIND, s.logoKind.name)
            .putString(LOGO_TEXT, s.logoText)
            .putInt(LOGO_SECONDS, s.logoSeconds)
            .putBoolean(LOGO_ON_IGNITION, s.logoOnIgnition)
            .apply()
    }

    private companion object {
        const val VIDEO = "video_while_driving"
        const val AA_TARGET = "android_auto_target"
        const val CP_TARGET = "carplay_target"
        const val AUTO_OPEN = "auto_open_bluetooth"
        const val ONLY_LAST = "only_last_device"
        const val LAST_DEVICE = "last_device"
        const val DEVICES = "devices"
        const val LOGO_KIND = "logo_kind"
        const val LOGO_TEXT = "logo_text"
        const val LOGO_SECONDS = "logo_seconds"
        const val LOGO_ON_IGNITION = "logo_on_ignition"
        const val SEP = "\t"

        fun encodeTarget(t: LaunchTarget?) = t?.let { it.packageName + SEP + it.activity.orEmpty() }

        fun decodeTarget(s: String?): LaunchTarget? {
            val parts = s?.split(SEP) ?: return null
            return LaunchTarget(parts[0], parts.getOrNull(1)?.takeIf { it.isNotEmpty() })
        }

        fun encodeDevice(d: KnownDevice) = listOf(d.address, d.mode.name, d.name).joinToString(SEP)

        fun decodeDevice(s: String): KnownDevice? {
            val parts = s.split(SEP)
            if (parts.size < 3) return null
            val mode = runCatching { ProjectionMode.valueOf(parts[1]) }.getOrNull() ?: return null
            return KnownDevice(parts[0], parts.drop(2).joinToString(SEP), mode)
        }
    }
}
