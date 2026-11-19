package com.headunit.core.camera

import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.tan

/**
 * Sistema de coordenadas do chão (metros), visto de cima:
 *  - origem: no chão, no centro do para-choque traseiro;
 *  - +Y: para trás do carro (para onde a câmera de ré olha);
 *  - +X: para a direita **da imagem** (câmeras de ré entregam a imagem espelhada,
 *    como um retrovisor, então +X é o lado direito do motorista).
 */
data class Ground(val x: Double, val y: Double)

data class Vec3(val x: Double, val y: Double, val z: Double) {
    operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)
    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
    operator fun times(k: Double) = Vec3(x * k, y * k, z * k)
    infix fun dot(o: Vec3) = x * o.x + y * o.y + z * o.z
}

/** Ponto na imagem, em pixels (origem no canto superior esquerdo). */
data class Pixel(val u: Double, val v: Double)

/** Onde a câmera está e para onde aponta. */
data class CameraPose(
    /** Altura da lente em relação ao chão. */
    val heightM: Double = 0.9,
    /** Inclinação para baixo em relação ao horizonte. */
    val pitchDeg: Double = 30.0,
    /** Rotação para os lados (câmera torta na placa). */
    val yawDeg: Double = 0.0,
    /** Giro em torno do próprio eixo (imagem inclinada). */
    val rollDeg: Double = 0.0,
    /** Deslocamento lateral da câmera em relação ao centro do carro. */
    val offsetXM: Double = 0.0,
)

/**
 * Lente. [fisheye] vai de 0 (lente comum, retas continuam retas) a 1 (olho de peixe
 * equidistante). O modelo é r = tan(c·θ)/c com c = 1 − fisheye: c = 1 é a lente
 * comum (r = tan θ), c → 0 é a equidistante (r = θ).
 */
data class Lens(val horizontalFovDeg: Double = 150.0, val fisheye: Double = 0.8) {
    private val c: Double get() = (1.0 - fisheye).coerceIn(MIN_C, 1.0)

    /** Raio normalizado para um ângulo θ (radianos) a partir do eixo da lente. */
    fun radius(theta: Double): Double {
        val ct = c * theta
        return if (c <= MIN_C) theta else tan(ct.coerceAtMost(MAX_ARG)) / c
    }

    fun theta(radius: Double): Double = if (c <= MIN_C) radius else atan(c * radius) / c

    /** Maior ângulo que a lente consegue representar. */
    val maxTheta: Double get() = if (c <= MIN_C) Math.PI else (MAX_ARG / c).coerceAtMost(Math.PI)

    private companion object {
        const val MIN_C = 1e-3
        const val MAX_ARG = Math.PI / 2 * 0.98
    }
}

/**
 * Projeta pontos do chão na imagem e faz o caminho inverso (pixel → chão).
 * A mesma classe serve para a imagem "crua" da câmera (com fisheye) e para a imagem
 * corrigida (lente comum), mudando só a [lens].
 */
class CameraProjector(
    val pose: CameraPose,
    val lens: Lens,
    val width: Int,
    val height: Int,
) {
    private val center = Vec3(pose.offsetXM, 0.0, pose.heightM)
    private val forward: Vec3
    private val right: Vec3
    private val up: Vec3
    /** Pixels por unidade de raio normalizado. */
    private val focal: Double = (width / 2.0) / lens.radius(Math.toRadians(lens.horizontalFovDeg / 2))

    init {
        val p = Math.toRadians(pose.pitchDeg)
        val y = Math.toRadians(pose.yawDeg)
        val r = Math.toRadians(pose.rollDeg)
        // Olhando para +Y, inclinada para baixo; depois yaw em torno de Z.
        val f0 = Vec3(sin(y) * cos(p), cos(y) * cos(p), -sin(p))
        val r0 = Vec3(cos(y), -sin(y), 0.0)
        val u0 = Vec3(sin(y) * sin(p), cos(y) * sin(p), cos(p))
        // Roll em torno do eixo da lente.
        right = r0 * cos(r) + u0 * sin(r)
        up = u0 * cos(r) - r0 * sin(r)
        forward = f0
    }

    fun project(g: Ground): Pixel? = projectPoint(Vec3(g.x, g.y, 0.0))

    fun projectPoint(world: Vec3): Pixel? = projectDirection(world - center)

    /** Projeta uma direção (no referencial do mundo). */
    fun projectDirection(d: Vec3): Pixel? {
        val xc = d dot right
        val yc = d dot up
        val zc = d dot forward
        val theta = atan2(hypot(xc, yc), zc)
        if (theta >= lens.maxTheta) return null
        val phi = atan2(yc, xc)
        val r = focal * lens.radius(theta)
        return Pixel(width / 2.0 + r * cos(phi), height / 2.0 - r * sin(phi))
    }

    /** Direção (no mundo) que chega no pixel. */
    fun ray(px: Pixel): Vec3? {
        val dx = px.u - width / 2.0
        val dy = height / 2.0 - px.v
        val r = hypot(dx, dy) / focal
        val theta = lens.theta(r)
        if (theta >= lens.maxTheta) return null
        val phi = atan2(dy, dx)
        val s = sin(theta)
        return right * (s * cos(phi)) + up * (s * sin(phi)) + forward * cos(theta)
    }

    /** Ponto do chão visto no pixel, ou null se o pixel mostra o céu. */
    fun toGround(px: Pixel): Ground? {
        val d = ray(px) ?: return null
        if (d.z >= -1e-6) return null
        val t = -center.z / d.z
        val p = center + d * t
        return Ground(p.x, p.y)
    }
}
