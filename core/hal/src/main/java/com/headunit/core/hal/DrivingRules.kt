package com.headunit.core.hal

import com.headunit.core.hal.vehicle.Gear
import com.headunit.core.hal.vehicle.VehicleState

/** Regras de segurança que dependem do estado do carro. */
object DrivingRules {
    /** Acima desta velocidade o vídeo é ocultado (o áudio continua). */
    const val VIDEO_SPEED_LIMIT_KMH = 5f

    fun isVideoAllowed(state: VehicleState): Boolean = state.speedKmh < VIDEO_SPEED_LIMIT_KMH

    fun shouldShowReverseCamera(state: VehicleState): Boolean =
        state.ignition.isPowered && state.gear == Gear.REVERSE
}
