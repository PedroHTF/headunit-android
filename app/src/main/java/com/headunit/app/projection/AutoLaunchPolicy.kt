package com.headunit.app.projection

import com.headunit.app.settings.ProjectionMode
import com.headunit.app.settings.Settings

data class BtDevice(val address: String, val name: String)

/**
 * Decide se a projeção deve abrir sozinha quando um aparelho conecta no Bluetooth.
 *
 * Regras:
 *  - **Primeira conexão da viagem** (depois de ligar o carro): com "só o último aparelho",
 *    ignora outros celulares (ex.: o do passageiro que pareou uma vez).
 *  - **Durante a viagem** (com "trocar de celular na viagem" ligado): outro celular conhecido
 *    que conectar assume a projeção no modo dele. O mesmo celular reconectando também reabre,
 *    exceto em quedas rápidas (menos de [QUICK_RECONNECT_MS]), para não ficar piscando.
 *  - O modo vem do aparelho conhecido (aprendido ao tocar no ícone ou definido nas
 *    Configurações); sem isso, um nome com "iPhone" sugere CarPlay.
 */
class AutoLaunchPolicy(private val clock: () -> Long = System::currentTimeMillis) {
    private var launchedThisTrip = false
    private var lastAddress: String? = null
    private var lastAt = 0L

    /** Chamar quando a ignição desliga: a próxima conexão é a "primeira da viagem". */
    fun onPowerCycle() {
        launchedThisTrip = false
        lastAddress = null
    }

    /** Chamar quando o usuário abre a projeção manualmente. */
    fun onManualLaunch(device: BtDevice?) = record(device?.address)

    fun decide(settings: Settings, device: BtDevice): ProjectionMode? {
        if (!settings.autoOpenOnBluetooth) return null
        val mode = settings.devices[device.address]?.mode ?: guessFromName(device.name) ?: return null

        if (!launchedThisTrip) {
            val last = settings.lastDeviceAddress
            if (settings.onlyLastDevice && last != null && !last.equals(device.address, ignoreCase = true)) return null
        } else {
            if (!settings.switchPhoneMidTrip) return null
            val quickReconnect = device.address.equals(lastAddress, ignoreCase = true) &&
                clock() - lastAt < QUICK_RECONNECT_MS
            if (quickReconnect) return null
        }
        record(device.address)
        return mode
    }

    private fun record(address: String?) {
        launchedThisTrip = true
        lastAddress = address
        lastAt = clock()
    }

    companion object {
        const val QUICK_RECONNECT_MS = 60_000L

        fun guessFromName(name: String): ProjectionMode? {
            val n = name.lowercase()
            return if ("iphone" in n || "ipad" in n) ProjectionMode.CARPLAY else null
        }
    }
}
