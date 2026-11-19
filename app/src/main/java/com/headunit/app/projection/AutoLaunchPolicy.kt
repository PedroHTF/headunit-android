package com.headunit.app.projection

import com.headunit.app.settings.ProjectionMode
import com.headunit.app.settings.Settings

data class BtDevice(val address: String, val name: String)

/** O que fazer quando um celular conecta no Bluetooth. */
sealed interface ConnectAction {
    /** Abrir a projeção sozinho (primeira conexão da viagem). */
    data class AutoOpen(val mode: ProjectionMode) : ConnectAction
    /** Só sugerir: aparece um aviso e alguém precisa tocar para abrir. */
    data class Suggest(val device: BtDevice, val mode: ProjectionMode) : ConnectAction
    data object Nothing : ConnectAction
}

/**
 * Regras:
 *  - Só a **primeira conexão da viagem** (depois de ligar o carro) abre sozinha.
 *    Com "só o último aparelho", ignora outros celulares nessa hora.
 *  - Depois disso, qualquer conexão (o mesmo celular voltando de uma queda, ou outro
 *    celular) **não abre sozinha**: o app mostra um aviso e quem está no carro decide,
 *    tocando no aviso ou no ícone Android Auto / CarPlay.
 *  - O modo vem do aparelho conhecido (aprendido ao tocar no ícone ou definido nas
 *    Configurações); sem isso, um nome com "iPhone" sugere CarPlay.
 */
class AutoLaunchPolicy {
    private var openedThisTrip = false

    /** Chamar quando a ignição desliga: a próxima conexão é a "primeira da viagem". */
    fun onPowerCycle() {
        openedThisTrip = false
    }

    /** Chamar quando alguém abre a projeção manualmente. */
    fun onManualLaunch() {
        openedThisTrip = true
    }

    fun decide(settings: Settings, device: BtDevice): ConnectAction {
        val mode = settings.devices[device.address]?.mode ?: guessFromName(device.name) ?: return ConnectAction.Nothing
        if (openedThisTrip || !settings.autoOpenOnBluetooth) return ConnectAction.Suggest(device, mode)
        val last = settings.lastDeviceAddress
        if (settings.onlyLastDevice && last != null && !last.equals(device.address, ignoreCase = true)) {
            return ConnectAction.Suggest(device, mode)
        }
        openedThisTrip = true
        return ConnectAction.AutoOpen(mode)
    }

    companion object {
        fun guessFromName(name: String): ProjectionMode? {
            val n = name.lowercase()
            return if ("iphone" in n || "ipad" in n) ProjectionMode.CARPLAY else null
        }
    }
}
