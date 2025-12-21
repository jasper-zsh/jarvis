package pro.sihao.jarvis.core.data.storage

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import pro.sihao.jarvis.core.domain.model.GlassesIntegrationSettings
import pro.sihao.jarvis.core.domain.model.RokidGlassesDevice

@Singleton
class GlassesPreferences @Inject constructor(
    @ApplicationContext context: Context
) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveDevice(device: RokidGlassesDevice) {
        prefs.edit()
            .putString(KEY_NAME, device.name)
            .putString(KEY_MAC, device.macAddress)
            .putString(KEY_SOCKET_UUID, device.socketUuid)
            .putString(KEY_ACCOUNT, device.rokidAccount)
            .putInt(KEY_TYPE, device.glassesType ?: -1)
            .apply()
    }

    fun loadDevice(): RokidGlassesDevice? {
        val mac = prefs.getString(KEY_MAC, null)
        val name = prefs.getString(KEY_NAME, null) ?: mac ?: return null
        val socketUuid = prefs.getString(KEY_SOCKET_UUID, null)
        val account = prefs.getString(KEY_ACCOUNT, null)
        val typeValue = prefs.getInt(KEY_TYPE, -1)
        val type = if (typeValue == -1) null else typeValue

        return RokidGlassesDevice(
            name = name,
            macAddress = mac,
            socketUuid = socketUuid,
            rokidAccount = account,
            glassesType = type
        )
    }

    fun clearDevice() {
        prefs.edit()
            .remove(KEY_NAME)
            .remove(KEY_MAC)
            .remove(KEY_SOCKET_UUID)
            .remove(KEY_ACCOUNT)
            .remove(KEY_TYPE)
            .apply()
    }

    fun saveSettings(settings: GlassesIntegrationSettings) {
        prefs.edit()
            .putBoolean(KEY_USE_INPUT, settings.useGlassesInput)
            .putBoolean(KEY_USE_OUTPUT, settings.useGlassesOutput)
            .apply()
    }

    fun loadSettings(): GlassesIntegrationSettings {
        return GlassesIntegrationSettings(
            useGlassesInput = prefs.getBoolean(KEY_USE_INPUT, false),
            useGlassesOutput = prefs.getBoolean(KEY_USE_OUTPUT, false)
        )
    }

    companion object {
        private const val PREFS_NAME = "glasses_prefs"
        private const val KEY_NAME = "device_name"
        private const val KEY_MAC = "device_mac"
        private const val KEY_SOCKET_UUID = "device_socket_uuid"
        private const val KEY_ACCOUNT = "device_account"
        private const val KEY_TYPE = "device_type"
        private const val KEY_USE_INPUT = "use_glasses_input"
        private const val KEY_USE_OUTPUT = "use_glasses_output"
    }
}
