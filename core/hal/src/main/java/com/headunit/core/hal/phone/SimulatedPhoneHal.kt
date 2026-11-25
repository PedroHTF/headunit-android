package com.headunit.core.hal.phone

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Celular de mentira: agenda fictícia, chamadas que "atendem" sozinhas e ligações simuladas. */
class SimulatedPhoneHal(
    private val scope: CoroutineScope,
    private val answerDelayMs: Long = 2_500,
    private val clock: () -> Long = System::currentTimeMillis,
) : PhoneHal {
    override val name = "Telefone simulado"
    override val isSimulated = true

    private val _state = MutableStateFlow(
        PhoneState(
            connected = true,
            deviceName = "Celular (simulado)",
            battery = 78,
            signal = 4,
            contacts = CONTACTS,
            recents = listOf(
                RecentCall("Ana Souza", "+55 11 91234-5678", CallKind.IN, clock() - 3_600_000),
                RecentCall("Oficina", "+55 11 4002-8922", CallKind.OUT, clock() - 3 * 3_600_000),
                RecentCall("+55 11 95555-0000", "+55 11 95555-0000", CallKind.MISSED, clock() - 86_400_000),
            ),
        ),
    )
    override val state: StateFlow<PhoneState> = _state.asStateFlow()

    private var outgoing = false
    private var callJob: Job? = null

    override fun dial(number: String) {
        val s = _state.value
        if (!s.connected || s.inCall || number.isBlank()) return
        outgoing = true
        start(number.trim(), CallState.DIALING)
        callJob = scope.launch {
            delay(answerDelayMs)
            becomeActive()
        }
    }

    /** Simulador: chega uma ligação. */
    fun simulateIncoming(number: String) {
        val s = _state.value
        if (!s.connected || s.inCall) return
        outgoing = false
        start(number, CallState.INCOMING)
    }

    override fun accept() {
        if (_state.value.callState != CallState.INCOMING) return
        callJob = scope.launch { becomeActive() }
    }

    override fun reject() {
        if (_state.value.callState == CallState.INCOMING) end(CallKind.MISSED)
    }

    override fun hangup() {
        when (_state.value.callState) {
            CallState.IDLE -> Unit
            CallState.INCOMING -> end(CallKind.MISSED)
            else -> end(if (outgoing) CallKind.OUT else CallKind.IN)
        }
    }

    /** Simulador: o outro lado desliga. */
    fun simulateRemoteHangup() = hangup()

    override fun setMuted(muted: Boolean) {
        if (_state.value.inCall) _state.update { it.copy(muted = muted) }
    }

    override fun sendDtmf(digit: Char) = Unit

    /** Simulador: celular conecta ou desconecta. */
    fun setConnected(connected: Boolean) {
        if (!connected && _state.value.inCall) hangup()
        _state.update { it.copy(connected = connected) }
    }

    private fun start(number: String, state: CallState) {
        _state.update {
            it.copy(callState = state, callNumber = number, callName = it.contacts.nameFor(number), callSeconds = 0, muted = false)
        }
    }

    private suspend fun becomeActive() {
        _state.update { it.copy(callState = CallState.ACTIVE) }
        while (scope.isActive && _state.value.callState == CallState.ACTIVE) {
            delay(1_000)
            _state.update { if (it.callState == CallState.ACTIVE) it.copy(callSeconds = it.callSeconds + 1) else it }
        }
    }

    private fun end(kind: CallKind) {
        callJob?.cancel()
        callJob = null
        _state.update {
            it.copy(
                callState = CallState.IDLE, callName = "", callNumber = "", callSeconds = 0, muted = false,
                recents = (listOf(RecentCall(it.callName, it.callNumber, kind, clock())) + it.recents).take(30),
            )
        }
    }

    companion object {
        val CONTACTS = listOf(
            Contact("Ana Souza", "+55 11 91234-5678"), Contact("Bruno Lima", "+55 11 98765-4321"),
            Contact("Carla Mendes", "+55 21 99876-1234"), Contact("Casa", "+55 11 3456-7890"),
            Contact("Diego Rocha", "+55 31 99123-4567"), Contact("Escritório", "+55 11 2345-6789"),
            Contact("Fernanda Alves", "+55 11 97654-3210"), Contact("Oficina", "+55 11 4002-8922"),
        )
    }
}
