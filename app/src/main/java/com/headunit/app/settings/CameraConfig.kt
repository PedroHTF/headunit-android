package com.headunit.app.settings

import android.content.Context
import com.headunit.core.camera.CameraPose
import com.headunit.core.camera.CameraProjector
import com.headunit.core.camera.GuideOptions
import com.headunit.core.camera.Lens
import com.headunit.core.camera.Scene
import com.headunit.core.camera.VehicleDimensions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Tudo que a câmera de ré precisa: carro, posição/lente da câmera, correção e linhas. */
data class CameraConfig(
    val car: VehicleDimensions = VehicleDimensions(),
    val pose: CameraPose = CameraPose(),
    val lens: Lens = Lens(),
    val removeFisheye: Boolean = false,
    /** Campo de visão da imagem depois de remover o fisheye (menor = mais "zoom"). */
    val correctedFovDeg: Double = 110.0,
    val guides: GuideOptions = GuideOptions(),
    /** Imagem simulada enquanto não há câmera real. */
    val scene: Scene = Scene.PARKING,
) {
    fun rawProjector(w: Int, h: Int) = CameraProjector(pose, lens, w, h)

    fun correctedProjector(w: Int, h: Int) = CameraProjector(pose, Lens(correctedFovDeg, 0.0), w, h)

    /** Projetor da imagem que aparece na tela (as linhas usam o mesmo). */
    fun displayProjector(w: Int, h: Int) = if (removeFisheye) correctedProjector(w, h) else rawProjector(w, h)
}

/** Guarda a [CameraConfig] no aparelho. */
class CameraConfigRepository(context: Context) {
    private val prefs = context.getSharedPreferences("camera", Context.MODE_PRIVATE)
    private val _config = MutableStateFlow(load())
    val config: StateFlow<CameraConfig> = _config.asStateFlow()

    fun update(transform: (CameraConfig) -> CameraConfig) {
        val c = transform(_config.value)
        save(c)
        _config.value = c
    }

    fun reset() = update { CameraConfig() }

    private fun d(key: String, default: Double) = prefs.getString(key, null)?.toDoubleOrNull() ?: default
    private fun b(key: String, default: Boolean) = prefs.getBoolean(key, default)
    private fun list(key: String, default: List<Double>) =
        prefs.getString(key, null)?.let { s -> s.split(',').mapNotNull { it.toDoubleOrNull() } } ?: default

    private fun load(): CameraConfig {
        val def = CameraConfig()
        return CameraConfig(
            car = VehicleDimensions(
                widthM = d("car.width", def.car.widthM),
                lengthM = d("car.length", def.car.lengthM),
                wheelbaseM = d("car.wheelbase", def.car.wheelbaseM),
                rearOverhangM = d("car.overhang", def.car.rearOverhangM),
                steeringRatio = d("car.ratio", def.car.steeringRatio),
                maxWheelAngleDeg = d("car.maxWheel", def.car.maxWheelAngleDeg),
            ),
            pose = CameraPose(
                heightM = d("cam.height", def.pose.heightM),
                pitchDeg = d("cam.pitch", def.pose.pitchDeg),
                yawDeg = d("cam.yaw", def.pose.yawDeg),
                rollDeg = d("cam.roll", def.pose.rollDeg),
                offsetXM = d("cam.offsetX", def.pose.offsetXM),
            ),
            lens = Lens(d("lens.fov", def.lens.horizontalFovDeg), d("lens.fisheye", def.lens.fisheye)),
            removeFisheye = b("img.removeFisheye", def.removeFisheye),
            correctedFovDeg = d("img.correctedFov", def.correctedFovDeg),
            guides = GuideOptions(
                showStatic = b("lines.static", def.guides.showStatic),
                showDynamic = b("lines.dynamic", def.guides.showDynamic),
                distanceMarkersM = list("lines.markers", def.guides.distanceMarkersM),
                carLengthMarkers = list("lines.carMarkers", def.guides.carLengthMarkers.map(Int::toDouble)).map(Double::toInt),
                lineOffsetXM = d("lines.offsetX", def.guides.lineOffsetXM),
                lineOffsetYM = d("lines.offsetY", def.guides.lineOffsetYM),
                invertSteering = b("lines.invert", def.guides.invertSteering),
                dynamicLengthM = d("lines.dynLength", def.guides.dynamicLengthM),
            ),
            scene = runCatching { Scene.valueOf(prefs.getString("img.scene", null) ?: "") }.getOrDefault(def.scene),
        )
    }

    private fun save(c: CameraConfig) {
        prefs.edit().apply {
            putString("car.width", c.car.widthM.toString())
            putString("car.length", c.car.lengthM.toString())
            putString("car.wheelbase", c.car.wheelbaseM.toString())
            putString("car.overhang", c.car.rearOverhangM.toString())
            putString("car.ratio", c.car.steeringRatio.toString())
            putString("car.maxWheel", c.car.maxWheelAngleDeg.toString())
            putString("cam.height", c.pose.heightM.toString())
            putString("cam.pitch", c.pose.pitchDeg.toString())
            putString("cam.yaw", c.pose.yawDeg.toString())
            putString("cam.roll", c.pose.rollDeg.toString())
            putString("cam.offsetX", c.pose.offsetXM.toString())
            putString("lens.fov", c.lens.horizontalFovDeg.toString())
            putString("lens.fisheye", c.lens.fisheye.toString())
            putBoolean("img.removeFisheye", c.removeFisheye)
            putString("img.correctedFov", c.correctedFovDeg.toString())
            putBoolean("lines.static", c.guides.showStatic)
            putBoolean("lines.dynamic", c.guides.showDynamic)
            putString("lines.markers", c.guides.distanceMarkersM.joinToString(","))
            putString("lines.carMarkers", c.guides.carLengthMarkers.joinToString(","))
            putString("lines.offsetX", c.guides.lineOffsetXM.toString())
            putString("lines.offsetY", c.guides.lineOffsetYM.toString())
            putBoolean("lines.invert", c.guides.invertSteering)
            putString("lines.dynLength", c.guides.dynamicLengthM.toString())
            putString("img.scene", c.scene.name)
        }.apply()
    }
}
