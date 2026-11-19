package com.headunit.app.settings

/** Modos de projeção do celular. */
enum class ProjectionMode(val label: String) {
    ANDROID_AUTO("Android Auto"),
    CARPLAY("CarPlay"),
}

/** App (e opcionalmente a tela específica) que um ícone de projeção abre. */
data class LaunchTarget(val packageName: String, val activity: String? = null)

/** Aparelho Bluetooth conhecido e o modo que ele usa. */
data class KnownDevice(val address: String, val name: String, val mode: ProjectionMode)

data class Settings(
    /** Mostrar vídeo com o carro em movimento. */
    val videoWhileDriving: Boolean = false,
    /** Alvo escolhido pelo usuário; null = detectar o Zlink automaticamente. */
    val androidAutoTarget: LaunchTarget? = null,
    val carPlayTarget: LaunchTarget? = null,
    /** Abrir a projeção sozinho quando um celular conhecido conectar no Bluetooth. */
    val autoOpenOnBluetooth: Boolean = true,
    /** Só abrir automaticamente para o último aparelho usado. */
    val onlyLastDevice: Boolean = true,
    val lastDeviceAddress: String? = null,
    /** Aparelhos conhecidos, por endereço Bluetooth. */
    val devices: Map<String, KnownDevice> = emptyMap(),
) {
    fun target(mode: ProjectionMode): LaunchTarget? = when (mode) {
        ProjectionMode.ANDROID_AUTO -> androidAutoTarget
        ProjectionMode.CARPLAY -> carPlayTarget
    }

    fun withTarget(mode: ProjectionMode, target: LaunchTarget?): Settings = when (mode) {
        ProjectionMode.ANDROID_AUTO -> copy(androidAutoTarget = target)
        ProjectionMode.CARPLAY -> copy(carPlayTarget = target)
    }

    val lastDevice: KnownDevice? get() = lastDeviceAddress?.let(devices::get)

    /** Registra que [address] usa [mode] e passa a ser o último aparelho. */
    fun rememberDevice(address: String, name: String, mode: ProjectionMode): Settings =
        copy(devices = devices + (address to KnownDevice(address, name, mode)), lastDeviceAddress = address)

    fun setDeviceMode(address: String, name: String, mode: ProjectionMode?): Settings =
        if (mode == null) {
            copy(devices = devices - address, lastDeviceAddress = lastDeviceAddress.takeIf { it != address })
        } else {
            copy(devices = devices + (address to KnownDevice(address, name, mode)))
        }
}
