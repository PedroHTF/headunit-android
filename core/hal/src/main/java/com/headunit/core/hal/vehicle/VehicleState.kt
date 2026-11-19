package com.headunit.core.hal.vehicle

/** Marcha selecionada. */
enum class Gear(val label: String) {
    PARK("P"),
    REVERSE("R"),
    NEUTRAL("N"),
    DRIVE("D"),
    UNKNOWN("-"),
}

/** Estado da chave / ACC, como o MCU da central informa. */
enum class Ignition(val label: String) {
    OFF("Desligado"),
    ACC("ACC"),
    ON("Ligado"),
    START("Partida"),
    UNKNOWN("Desconhecido");

    /** A central deve ficar ligada? (sem informação, assume que sim) */
    val isPowered: Boolean get() = this != OFF
}

/** Botões do volante (ou teclas de mídia equivalentes). */
enum class SteeringKey(val label: String) {
    PREVIOUS("Anterior"),
    PLAY_PAUSE("Play/Pause"),
    NEXT("Próxima"),
    VOLUME_DOWN("Vol -"),
    VOLUME_UP("Vol +"),
    MUTE("Mudo"),
    MODE("Fonte"),
}

data class VehicleState(
    val ignition: Ignition = Ignition.ON,
    val gear: Gear = Gear.PARK,
    val speedKmh: Float = 0f,
)
