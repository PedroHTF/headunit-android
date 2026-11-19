package com.headunit.core.hal.vehicle

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SimulatedVehicleHal(initial: VehicleState = VehicleState()) : VehicleHal {
    override val name = "Simulado"
    override val isSimulated = true

    private val _state = MutableStateFlow(initial)
    override val state: StateFlow<VehicleState> = _state.asStateFlow()

    private val _keys = MutableSharedFlow<SteeringKey>(extraBufferCapacity = 16)
    override val keys: Flow<SteeringKey> = _keys.asSharedFlow()

    fun setIgnition(ignition: Ignition) = _state.update { it.copy(ignition = ignition) }

    fun setGear(gear: Gear) = _state.update { it.copy(gear = gear) }

    fun setSpeed(kmh: Float) = _state.update { it.copy(speedKmh = kmh.coerceIn(0f, MAX_SPEED_KMH)) }

    fun setSteering(deg: Float) = _state.update { it.copy(steeringWheelDeg = deg.coerceIn(-MAX_STEERING_DEG, MAX_STEERING_DEG)) }

    fun pressKey(key: SteeringKey) {
        _keys.tryEmit(key)
    }

    override fun start() = Unit
    override fun stop() = Unit

    companion object {
        const val MAX_SPEED_KMH = 200f
        const val MAX_STEERING_DEG = 540f
    }
}
