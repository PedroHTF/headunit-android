package com.headunit.core.hal.phone

import kotlinx.coroutines.flow.StateFlow

enum class CallState { IDLE, INCOMING, DIALING, ACTIVE }

enum class CallKind(val label: String) { IN("Recebida"), OUT("Feita"), MISSED("Perdida") }

data class Contact(val name: String, val number: String)

data class RecentCall(val name: String, val number: String, val kind: CallKind, val timeMillis: Long)

data class PhoneState(
    val connected: Boolean = false,
    val deviceName: String = "",
    val battery: Int = 0, // 0..100
    val signal: Int = 0,  // 0..5
    val callState: CallState = CallState.IDLE,
    val callName: String = "",
    val callNumber: String = "",
    val callSeconds: Int = 0,
    val muted: Boolean = false,
    val contacts: List<Contact> = emptyList(),
    val recents: List<RecentCall> = emptyList(),
) {
    val inCall: Boolean get() = callState != CallState.IDLE
}

/**
 * Telefone via Bluetooth (o celular faz a ligação; a central é o viva-voz).
 *
 * Implementações:
 *  - [SimulatedPhoneHal]: celular de mentira, para o PC/emulador.
 *  - futuramente: o app/serviço Bluetooth do fabricante da central (broadcasts) ou, numa
 *    ROM própria, o HFP client do Android (BluetoothHeadsetClient, API de sistema) + PBAP.
 */
interface PhoneHal {
    val name: String
    val isSimulated: Boolean
    val state: StateFlow<PhoneState>

    fun dial(number: String)
    fun accept()
    fun reject()
    fun hangup()
    fun setMuted(muted: Boolean)
    fun sendDtmf(digit: Char)
}

/** Nome na agenda para um número (compara os últimos 8 dígitos), ou o próprio número. */
fun List<Contact>.nameFor(number: String): String {
    val digits = number.filter(Char::isDigit)
    if (digits.isEmpty()) return number
    return firstOrNull { it.number.filter(Char::isDigit).endsWith(digits.takeLast(8)) }?.name ?: number
}
