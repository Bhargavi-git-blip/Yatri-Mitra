package com.yatrimitra.app.simulation

import com.yatrimitra.app.model.*
import kotlin.math.abs
import kotlin.random.Random

/**
 * ─────────────────────────────────────────────────────────────────────────────
 * SimulationEngine — Pure stateless functions for route simulation logic.
 *
 * SIMULATION LOGIC OVERVIEW:
 *  1. Positions are normalised [0.0, 1.0] across the route.
 *  2. Speed: km/h → position-units/second = speed / (routeLen × 3600)
 *  3. ETA formula: ETA (min) = distance (km) / speed (km/h) × 60
 *  4. Vehicles loop back to 0.0 when they reach 1.0 (circular service).
 * ─────────────────────────────────────────────────────────────────────────────
 */
object SimulationEngine {

    const val WAIT_SECS = 10
    const val TICK_MS   = 50L
    const val ARRIVE_THRESHOLD = 0.018f  // ~1.5% of route = "at stop"

    val WAIT_TICKS = (WAIT_SECS * 1000L / TICK_MS).toInt()

    val defaultStops = listOf(
        RouteStop(0, "Bus Stand",        0.00f),
        RouteStop(1, "Mandi Chowk",      0.20f),
        RouteStop(2, "Govt. School",     0.42f),
        RouteStop(3, "Health Centre",    0.62f),
        RouteStop(4, "Panchayat Office", 0.80f),
        RouteStop(5, "Grain Market",     1.00f)
    )

    fun defaultAutos() = listOf(
        Auto(id = 0, label = "A1", position = 0.05f, speedKmph = 28f),
        Auto(id = 1, label = "A2", position = 0.55f, speedKmph = 30f)
    )

    fun makeAuto(id: Int): Auto {
        val pos   = Random.nextFloat() * 0.85f + 0.05f
        val speed = (22 + Random.nextInt(14)).toFloat()
        return Auto(id = id, label = "A${id + 1}", position = pos, speedKmph = speed)
    }

    fun generateOtp(): String = (1000 + Random.nextInt(9000)).toString()

    // ── Tick ─────────────────────────────────────────────────────────────────
    /**
     * SIMULATION LOGIC: advance all FREE/BOARDED autos by one tick.
     * deltaPosition = speed(km/h) / (routeLen(km) × 3600 s/h) × dt(s)
     */
    fun moveAutos(autos: List<Auto>, routeLenKm: Float, deltaTimeSec: Float): List<Auto> =
        autos.map { a ->
            if (a.state == AutoState.STOPPED) return@map a
            val delta = a.speedKmph / (routeLenKm * 3600f) * deltaTimeSec
            a.copy(position = (a.position + delta) % 1.0f)
        }

    // ── ETA ──────────────────────────────────────────────────────────────────
    /**
     * SIMULATION LOGIC: ETA (min) = gap(km) / speed(km/h) × 60
     * If auto is ahead of stop, it wraps around the full route.
     */
    fun etaMinutes(auto: Auto, stopPos: Float, routeLenKm: Float): Float {
        val gap = if (auto.position <= stopPos) stopPos - auto.position
                  else (1.0f - auto.position) + stopPos
        return (gap * routeLenKm / auto.speedKmph) * 60f
    }

    fun rideDurationMinutes(fromPos: Float, toPos: Float, speedKmph: Float, routeLenKm: Float): Float {
        val gap = if (toPos >= fromPos) toPos - fromPos else 1.0f - fromPos + toPos
        return (gap * routeLenKm / speedKmph) * 60f
    }

    fun nearestFreeEta(autos: List<Auto>, stopPos: Float, routeLenKm: Float): Float? =
        autos.filter { it.state == AutoState.FREE }
             .minOfOrNull { etaMinutes(it, stopPos, routeLenKm) }

    fun isAtStop(auto: Auto, stopPos: Float) = abs(auto.position - stopPos) < ARRIVE_THRESHOLD
}
