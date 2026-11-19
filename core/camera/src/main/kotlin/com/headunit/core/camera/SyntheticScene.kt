package com.headunit.core.camera

import kotlin.math.abs
import kotlin.math.floor

/**
 * Imagem falsa da câmera de ré, gerada a partir do modelo da câmera: cada pixel vira
 * um raio, que acerta o chão (ou o céu). Com fisheye ligado, as retas do chão aparecem
 * curvas, igual a uma câmera de verdade; isso permite testar a correção sem hardware.
 */
enum class Scene { PARKING, GRID }

object SyntheticScene {
    private const val SKY = 0xFF2E3A48.toInt()
    private const val ASPHALT = 0xFF3B3E43.toInt()
    private const val ASPHALT_DARK = 0xFF34373B.toInt()
    private const val PAINT = 0xFFE8E8E0.toInt()
    private const val CURB = 0xFFB0A890.toInt()
    private const val GRID_A = 0xFF50555C.toInt()
    private const val GRID_B = 0xFF2A2D31.toInt()
    private const val BUMPER = 0xFF15171A.toInt()

    fun render(camera: CameraProjector, scene: Scene): IntArray {
        val out = IntArray(camera.width * camera.height)
        for (v in 0 until camera.height) {
            for (u in 0 until camera.width) {
                val g = camera.toGround(Pixel(u + 0.5, v + 0.5))
                out[v * camera.width + u] = when {
                    g == null -> SKY
                    g.y < 0 -> BUMPER // o próprio para-choque, no pé da imagem
                    scene == Scene.GRID -> grid(g)
                    else -> parking(g)
                }
            }
        }
        return out
    }

    /** Quadriculado de 1 m: ideal para calibrar distâncias. */
    private fun grid(g: Ground): Int {
        val cx = floor(g.x).toInt()
        val cy = floor(g.y).toInt()
        return if ((cx + cy) and 1 == 0) GRID_A else GRID_B
    }

    /** Vagas de 2,5 m de largura por 5 m, com meio-fio no fundo. */
    private fun parking(g: Ground): Int {
        if (g.y > 5.5) return CURB
        val stallLine = abs(((g.x + 1.25) % 2.5 + 2.5) % 2.5 - 0.0) < 0.06 ||
            abs(((g.x + 1.25) % 2.5 + 2.5) % 2.5 - 2.5) < 0.06
        if (stallLine && g.y > 0.3) return PAINT
        if (abs(g.y - 5.2) < 0.06) return PAINT
        return if ((floor(g.x * 2) + floor(g.y * 2)).toInt() and 1 == 0) ASPHALT else ASPHALT_DARK
    }
}
