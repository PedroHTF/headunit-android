package com.headunit.core.hal.vehicle

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Fonte dos dados do carro.
 *
 * Implementações:
 *  - [SimulatedVehicleHal]: controlada pela tela "Veículo" do app (desenvolvimento no PC).
 *  - [CarApiVehicleHal]: Android Automotive OS (emulador AAOS ou central com AAOS).
 *  - futuramente: MCU da central genérica (porta serial / broadcasts do fabricante).
 */
interface VehicleHal {
    val name: String
    val isSimulated: Boolean
    val state: StateFlow<VehicleState>
    val keys: Flow<SteeringKey>

    fun start()
    fun stop()
}
