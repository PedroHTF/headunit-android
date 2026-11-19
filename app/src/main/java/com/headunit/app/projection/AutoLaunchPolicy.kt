package com.headunit.app.projection

import com.headunit.app.settings.ProjectionMode
import com.headunit.app.settings.Settings

data class BtDevice(val address: String, val name: String)

/**
 * Decide se a projeção deve abrir sozinha quando um aparelho conecta no Bluetooth.
 *
 * Regras:
 *  - só uma vez por "partida" (ciclo de ignição): reconexões no meio da viagem não reabrem;
 *  - com "só o último aparelho", ignora outros celulares (ex.: o do passageiro);
 *  - o modo vem do aparelho conhecido (aprendido ao tocar no ícone ou definido nas
 *    Configurações); sem isso, um nome com "iPhone" sugere CarPlay.
 */
class AutoLaunchPolicy {
    private var launchedThisCycle = false

    /** Chamar quando a ignição desliga: a próxima partida pode abrir de novo. */
    fun onPowerCycle() {
        launchedThisCycle = false
    }

    /** Chamar quando o usuário abre a projeção manualmente. */
    fun onManualLaunch() {
        launchedThisCycle = true
    }

    fun decide(settings: Settings, device: BtDevice): ProjectionMode? {
        if (!settings.autoOpenOnBluetooth || launchedThisCycle) return null
        val last = settings.lastDeviceAddress
        if (settings.onlyLastDevice && last != null && !last.equals(device.address, ignoreCase = true)) return null
        val mode = settings.devices[device.address]?.mode ?: guessFromName(device.name) ?: return null
        launchedThisCycle = true
        return mode
    }

    companion object {
        fun guessFromName(name: String): ProjectionMode? {
            val n = name.lowercase()
            return if ("iphone" in n || "ipad" in n) ProjectionMode.CARPLAY else null
        }
    }
}
