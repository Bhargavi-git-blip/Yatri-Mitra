package com.yatrimitra.app.model

data class RouteStop(val id: Int, val name: String, val position: Float)

enum class AutoState { FREE, STOPPED, BOARDED }

data class Auto(
    val id: Int,
    val label: String,
    val position: Float,
    val speedKmph: Float,
    val state: AutoState = AutoState.FREE
)

enum class AppPhase {
    IDLE, FREE_ROAMING, BOOKED, WAITING_AT_FROM, OTP_PENDING, RIDING, ARRIVED
}

data class TripLog(val tripNumber: Int, val fromStop: String, val toStop: String, val autoLabel: String)

data class SimulationState(
    val autos: List<Auto> = emptyList(),
    val stops: List<RouteStop> = emptyList(),
    val fromStopIndex: Int = 2,
    val toStopIndex: Int = 4,
    val routeLengthKm: Float = 8f,
    val phase: AppPhase = AppPhase.IDLE,
    val isRunning: Boolean = false,
    val waitingAutoId: Int? = null,
    val boardedAutoId: Int? = null,
    val currentOtp: String? = null,
    val waitTicksElapsed: Int = 0,
    val nearestAutoEtaMinutes: Float? = null,
    val rideDurationMinutes: Float? = null,
    val tripLogs: List<TripLog> = emptyList(),
    val tripCount: Int = 0,
    val statusMessage: String? = null
)
