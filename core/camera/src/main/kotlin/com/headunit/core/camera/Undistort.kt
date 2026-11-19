package com.headunit.core.camera

/**
 * Remove o fisheye: para cada pixel da imagem corrigida, diz de qual pixel da imagem
 * crua da câmera ele vem. Calculado uma vez (quando a configuração muda) e aplicado
 * a cada quadro, o que é barato: uma consulta por pixel.
 *
 * @return índice (v * largura + u) na imagem crua, ou −1 se fora do campo de visão.
 */
fun buildUndistortMap(raw: CameraProjector, corrected: CameraProjector): IntArray {
    val map = IntArray(corrected.width * corrected.height)
    for (v in 0 until corrected.height) {
        for (u in 0 until corrected.width) {
            val dir = corrected.ray(Pixel(u + 0.5, v + 0.5))
            val src = dir?.let(raw::projectDirection)
            map[v * corrected.width + u] =
                if (src == null || src.u < 0 || src.v < 0 || src.u >= raw.width || src.v >= raw.height) {
                    -1
                } else {
                    src.v.toInt() * raw.width + src.u.toInt()
                }
        }
    }
    return map
}

/** Aplica o mapa a um quadro (pixels ARGB). */
fun applyMap(map: IntArray, source: IntArray, out: IntArray = IntArray(map.size), fill: Int = 0xFF000000.toInt()): IntArray {
    for (i in map.indices) {
        val s = map[i]
        out[i] = if (s >= 0) source[s] else fill
    }
    return out
}
