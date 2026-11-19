package com.headunit.core.hal.vehicle

import android.car.Car
import android.car.VehicleGear
import android.car.VehicleIgnitionState
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.update

/**
 * Lê marcha, ignição e velocidade pela API oficial do Android Automotive (Vehicle HAL).
 * No emulador AAOS esses valores são alterados em Extended controls > Car data.
 *
 * Só instancie se o aparelho tiver PackageManager.FEATURE_AUTOMOTIVE.
 * No AAOS os botões do volante chegam como KeyEvent na Activity, por isso [keys] é vazio.
 */
class CarApiVehicleHal(private val context: Context) : VehicleHal {
    override val name = "Android Automotive"
    override val isSimulated = false

    private val _state = MutableStateFlow(VehicleState(ignition = Ignition.UNKNOWN, gear = Gear.UNKNOWN))
    override val state: StateFlow<VehicleState> = _state.asStateFlow()
    override val keys: Flow<SteeringKey> = emptyFlow()

    private var car: Car? = null
    private var propertyManager: CarPropertyManager? = null

    private val callback = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) = handle(value)
        override fun onErrorEvent(propertyId: Int, zone: Int) {
            Log.w(TAG, "Erro na propriedade $propertyId (zona $zone)")
        }
    }

    override fun start() {
        if (car != null) return
        val connected = runCatching { Car.createCar(context) }.getOrNull() ?: run {
            Log.w(TAG, "Car service indisponível")
            return
        }
        car = connected
        val manager = connected.getCarManager(Car.PROPERTY_SERVICE) as? CarPropertyManager ?: return
        propertyManager = manager

        register(manager, VehiclePropertyIds.GEAR_SELECTION, CarPropertyManager.SENSOR_RATE_ONCHANGE)
        register(manager, VehiclePropertyIds.IGNITION_STATE, CarPropertyManager.SENSOR_RATE_ONCHANGE)
        register(manager, VehiclePropertyIds.PERF_VEHICLE_SPEED, CarPropertyManager.SENSOR_RATE_NORMAL)
    }

    private fun register(manager: CarPropertyManager, propertyId: Int, rate: Float) {
        // SecurityException se a permissão (ex.: CAR_SPEED) não foi concedida.
        runCatching { manager.registerCallback(callback, propertyId, rate) }
            .onFailure { Log.w(TAG, "Não foi possível ouvir a propriedade $propertyId", it) }
    }

    private fun handle(value: CarPropertyValue<*>) {
        val raw = value.value ?: return
        when (value.propertyId) {
            VehiclePropertyIds.GEAR_SELECTION -> _state.update { it.copy(gear = mapGear(raw as Int)) }
            VehiclePropertyIds.IGNITION_STATE -> _state.update { it.copy(ignition = mapIgnition(raw as Int)) }
            VehiclePropertyIds.PERF_VEHICLE_SPEED -> _state.update { it.copy(speedKmh = (raw as Float) * 3.6f) }
        }
    }

    override fun stop() {
        propertyManager?.let { runCatching { it.unregisterCallback(callback) } }
        propertyManager = null
        car?.disconnect()
        car = null
    }

    companion object {
        private const val TAG = "CarApiVehicleHal"

        fun mapGear(value: Int): Gear = when (value) {
            VehicleGear.GEAR_PARK -> Gear.PARK
            VehicleGear.GEAR_REVERSE -> Gear.REVERSE
            VehicleGear.GEAR_NEUTRAL -> Gear.NEUTRAL
            VehicleGear.GEAR_UNKNOWN -> Gear.UNKNOWN
            else -> Gear.DRIVE // DRIVE, 1ª, 2ª... contam como "andando para frente"
        }

        fun mapIgnition(value: Int): Ignition = when (value) {
            VehicleIgnitionState.LOCK, VehicleIgnitionState.OFF -> Ignition.OFF
            VehicleIgnitionState.ACC -> Ignition.ACC
            VehicleIgnitionState.ON -> Ignition.ON
            VehicleIgnitionState.START -> Ignition.START
            else -> Ignition.UNKNOWN
        }
    }
}
