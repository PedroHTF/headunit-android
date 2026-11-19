package com.headunit.core.camera

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.tan

/** Medidas do carro (metros). */
data class VehicleDimensions(
    val widthM: Double = 1.75,
    val lengthM: Double = 4.30,
    val wheelbaseM: Double = 2.60,
    /** Do eixo traseiro até o para-choque traseiro. */
    val rearOverhangM: Double = 0.85,
    /** Graus de volante para 1 grau de roda (tipicamente 14–17). */
    val steeringRatio: Double = 15.0,
    /** Maior ângulo das rodas dianteiras. */
    val maxWheelAngleDeg: Double = 35.0,
)

data class GuideOptions(
    val showStatic: Boolean = true,
    val showDynamic: Boolean = true,
    /** Marcas de distância a partir do para-choque. */
    val distanceMarkersM: List<Double> = listOf(0.5, 1.0, 2.0),
    /** Marcas em "carros" de distância (1 = um comprimento do carro). */
    val carLengthMarkers: List<Int> = listOf(1),
    /** Ajuste fino das linhas (calibração). */
    val lineOffsetXM: Double = 0.0,
    val lineOffsetYM: Double = 0.0,
    /** Inverte o lado para onde a trajetória curva (se a câmera não espelha a imagem). */
    val invertSteering: Boolean = false,
    /** Comprimento da trajetória dinâmica, medido no centro do para-choque. */
    val dynamicLengthM: Double = 3.0,
)

enum class GuideColor { RED, YELLOW, GREEN, BLUE, WHITE }

/** Uma linha a desenhar, em coordenadas do chão. [label] aparece na ponta. */
data class GuideLine(val points: List<Ground>, val color: GuideColor, val label: String? = null)

object Guidelines {

    /** Ângulo das rodas dianteiras para um ângulo de volante (graus, + = direita). */
    fun wheelAngleDeg(steeringWheelDeg: Double, car: VehicleDimensions): Double =
        (steeringWheelDeg / car.steeringRatio).coerceIn(-car.maxWheelAngleDeg, car.maxWheelAngleDeg)

    fun build(car: VehicleDimensions, options: GuideOptions, steeringWheelDeg: Double): List<GuideLine> {
        val lines = mutableListOf<GuideLine>()
        val half = car.widthM / 2
        val markers = markers(car, options)
        val maxY = (markers.maxOfOrNull { it.first } ?: 2.0).coerceAtLeast(0.5)

        if (options.showStatic) {
            // Laterais: largura do carro, coloridas por faixa de distância.
            val stops = listOf(0.0) + markers.map { it.first }.filter { it < maxY } + maxY
            stops.zipWithNext().forEach { (a, b) ->
                val color = colorFor(a)
                for (sx in listOf(-half, half)) {
                    lines += GuideLine(listOf(shift(sx, a, options), shift(sx, b, options)), color)
                }
            }
            // Marcas transversais.
            markers.forEach { (y, label) ->
                val tick = car.widthM * 0.22
                val color = colorFor(y - 1e-6)
                lines += GuideLine(listOf(shift(-half, y, options), shift(-half + tick, y, options)), color, label)
                lines += GuideLine(listOf(shift(half - tick, y, options), shift(half, y, options)), color)
            }
        }

        if (options.showDynamic) {
            val sign = if (options.invertSteering) -1.0 else 1.0
            val wheel = wheelAngleDeg(steeringWheelDeg * sign, car)
            for (sx in listOf(-half, half)) {
                lines += GuideLine(trajectory(sx, car, wheel, options.dynamicLengthM).map { shift(it.x, it.y, options) }, GuideColor.BLUE)
            }
        }
        return lines
    }

    /** (distância, rótulo), em ordem. Marcas de "carros" usam o comprimento do carro. */
    fun markers(car: VehicleDimensions, options: GuideOptions): List<Pair<Double, String>> {
        val meters = options.distanceMarkersM.filter { it > 0 }.map { it to formatMeters(it) }
        val cars = options.carLengthMarkers.filter { it > 0 }
            .map { n -> n * car.lengthM to (if (n == 1) "1 carro" else "$n carros") }
        return (meters + cars).sortedBy { it.first }.distinctBy { (it.first * 100).toInt() }
    }

    /**
     * Caminho do canto do para-choque (x = [startX], y = 0) andando de ré com as rodas
     * dianteiras em [wheelAngleDeg]. Modelo de bicicleta: o carro gira em torno de um
     * ponto na linha do eixo traseiro, a uma distância R = entre-eixos / tan(ângulo).
     */
    fun trajectory(startX: Double, car: VehicleDimensions, wheelAngleDeg: Double, lengthM: Double, steps: Int = 24): List<Ground> {
        val delta = Math.toRadians(wheelAngleDeg)
        if (abs(delta) < 1e-4) return listOf(Ground(startX, 0.0), Ground(startX, lengthM))
        val axleY = -car.rearOverhangM
        val radius = car.wheelbaseM / tan(abs(delta))
        val cx = if (delta > 0) radius else -radius // volante para a direita: centro à direita
        // Ângulo total para o centro do para-choque percorrer [lengthM].
        val bumperRadius = hypot(cx, axleY)
        val total = lengthM / bumperRadius
        val dir = if (cx > 0) 1.0 else -1.0
        return (0..steps).map { i ->
            val a = total * i / steps
            // Gira o ponto em torno de (cx, axleY); o sentido faz o carro andar para +Y.
            val rx = startX - cx
            val ry = 0.0 - axleY
            val ang = -dir * a
            Ground(cx + rx * cos(ang) - ry * sin(ang), axleY + rx * sin(ang) + ry * cos(ang))
        }
    }

    private fun shift(x: Double, y: Double, o: GuideOptions) = Ground(x + o.lineOffsetXM, y + o.lineOffsetYM)

    private fun colorFor(y: Double) = when {
        y < 0.5 -> GuideColor.RED
        y < 1.0 -> GuideColor.YELLOW
        else -> GuideColor.GREEN
    }

    private fun formatMeters(m: Double) =
        if (m == m.toInt().toDouble()) "${m.toInt()} m" else "${"%.1f".format(m).replace('.', ',')} m"

    /** Usado nos testes: rumo (graus) do movimento no fim da trajetória. */
    internal fun heading(points: List<Ground>): Double {
        val (a, b) = points.takeLast(2)
        return Math.toDegrees(atan2(b.x - a.x, b.y - a.y))
    }
}
