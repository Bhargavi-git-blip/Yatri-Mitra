package com.yatrimitra.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yatrimitra.app.model.*
import com.yatrimitra.app.simulation.SimulationEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * RouteViewModel — survives screen rotation, owns all simulation state via StateFlow.
 *
 * Screen rotation safety:
 *  • `by viewModels()` returns the SAME instance after rotation.
 *  • StateFlow always replays latest value to new collectors.
 *  • viewModelScope coroutine is NOT cancelled on rotation.
 */
class RouteViewModel : ViewModel() {

    private val _state = MutableStateFlow(
        SimulationState(
            autos         = SimulationEngine.defaultAutos(),
            stops         = SimulationEngine.defaultStops,
            fromStopIndex = 2,
            toStopIndex   = 4,
            phase         = AppPhase.IDLE
        )
    )
    val state: StateFlow<SimulationState> = _state.asStateFlow()

    private var tickerJob: Job? = null
    private var nextAutoId = 2

    // ── Controls ──────────────────────────────────────────────────────────────

    fun startSimulation() {
        if (tickerJob?.isActive == true) return
        val current = _state.value
        val newPhase = if (current.phase == AppPhase.IDLE) AppPhase.FREE_ROAMING else current.phase
        _state.value = current.copy(isRunning = true, phase = newPhase)
        tickerJob = viewModelScope.launch {
            while (isActive) {
                tick()
                delay(SimulationEngine.TICK_MS)
            }
        }
    }

    fun pauseSimulation() {
        tickerJob?.cancel(); tickerJob = null
        _state.value = _state.value.copy(isRunning = false)
    }

    fun toggleSimulation() {
        if (_state.value.isRunning) pauseSimulation() else startSimulation()
    }

    fun addAuto() {
        val s = _state.value
        if (s.autos.size >= 7) return
        val newAuto = SimulationEngine.makeAuto(nextAutoId++)
        _state.value = s.copy(autos = s.autos + newAuto)
    }

    fun setFromStop(index: Int) {
        val s = _state.value
        if (!isSelectionEditable(s)) return
        val toIdx = if (index == s.toStopIndex) (index + 1) % s.stops.size else s.toStopIndex
        _state.value = s.copy(fromStopIndex = index, toStopIndex = toIdx)
    }

    fun setToStop(index: Int) {
        val s = _state.value
        if (!isSelectionEditable(s)) return
        val fromIdx = if (index == s.fromStopIndex) (index - 1 + s.stops.size) % s.stops.size else s.fromStopIndex
        _state.value = s.copy(toStopIndex = index, fromStopIndex = fromIdx)
    }

    fun bookRide() {
        val s = _state.value
        if (s.phase != AppPhase.FREE_ROAMING) return
        _state.value = s.copy(
            phase = AppPhase.BOOKED,
            statusMessage = "Finding nearest auto for ${s.stops[s.fromStopIndex].name}…"
        )
    }

    fun verifyOtp(entered: String): Boolean {
        val s = _state.value
        if (entered != s.currentOtp) return false
        // Correct OTP — start ride
        val boardedAuto = s.waitingAutoId
        _state.value = s.copy(
            phase         = AppPhase.RIDING,
            boardedAutoId = boardedAuto,
            waitingAutoId = null,
            autos = s.autos.map { a ->
                if (a.id == boardedAuto) a.copy(state = AutoState.BOARDED) else a
            },
            statusMessage = "Riding to ${s.stops[s.toStopIndex].name}…"
        )
        return true
    }

    fun resetAll() {
        pauseSimulation()
        nextAutoId = 2
        _state.value = SimulationState(
            autos         = SimulationEngine.defaultAutos(),
            stops         = SimulationEngine.defaultStops,
            fromStopIndex = 2,
            toStopIndex   = 4,
            phase         = AppPhase.IDLE
        )
    }

    // ── Tick ─────────────────────────────────────────────────────────────────

    private fun tick() {
        val s = _state.value
        val dt = SimulationEngine.TICK_MS / 1000f

        when (s.phase) {
            AppPhase.WAITING_AT_FROM -> handleWaitingTick(s)
            AppPhase.OTP_PENDING     -> { /* waiting for user input — no movement */ }
            AppPhase.IDLE            -> { /* not started */ }
            else -> handleMovementTick(s, dt)
        }
    }

    private fun handleWaitingTick(s: SimulationState) {
        val newTicks = s.waitTicksElapsed + 1
        if (newTicks >= SimulationEngine.WAIT_TICKS) {
            // Time's up → show OTP
            _state.value = s.copy(
                phase             = AppPhase.OTP_PENDING,
                waitTicksElapsed  = newTicks,
                currentOtp        = SimulationEngine.generateOtp()
            )
        } else {
            _state.value = s.copy(waitTicksElapsed = newTicks)
        }
    }

    private fun handleMovementTick(s: SimulationState, dt: Float) {
        val movedAutos = SimulationEngine.moveAutos(s.autos, s.routeLengthKm, dt)
        val fromPos = s.stops[s.fromStopIndex].position
        val toPos   = s.stops[s.toStopIndex].position

        var updated = s.copy(autos = movedAutos)

        when (s.phase) {
            AppPhase.BOOKED -> {
                // Check if any free auto has arrived at FROM stop
                val arrived = movedAutos.firstOrNull { a ->
                    a.state == AutoState.FREE && SimulationEngine.isAtStop(a, fromPos)
                }
                if (arrived != null) {
                    updated = updated.copy(
                        phase         = AppPhase.WAITING_AT_FROM,
                        waitingAutoId = arrived.id,
                        waitTicksElapsed = 0,
                        autos = movedAutos.map { a ->
                            if (a.id == arrived.id) a.copy(state = AutoState.STOPPED, position = fromPos) else a
                        }
                    )
                } else {
                    // Update ETA display while waiting for auto
                    val eta = SimulationEngine.nearestFreeEta(movedAutos, fromPos, s.routeLengthKm)
                    updated = updated.copy(nearestAutoEtaMinutes = eta)
                }
            }

            AppPhase.RIDING -> {
                val boarded = movedAutos.find { it.id == s.boardedAutoId }
                if (boarded != null && SimulationEngine.isAtStop(boarded, toPos)) {
                    // Drop off passenger — log trip, go FREE_ROAMING
                    val newCount = s.tripCount + 1
                    val log = TripLog(
                        tripNumber = newCount,
                        fromStop   = s.stops[s.fromStopIndex].name,
                        toStop     = s.stops[s.toStopIndex].name,
                        autoLabel  = boarded.label
                    )
                    updated = updated.copy(
                        phase         = AppPhase.FREE_ROAMING,
                        boardedAutoId = null,
                        tripCount     = newCount,
                        tripLogs      = listOf(log) + s.tripLogs,
                        autos = movedAutos.map { a ->
                            if (a.id == boarded.id) a.copy(state = AutoState.FREE) else a
                        },
                        statusMessage = "✅ Dropped at ${s.stops[s.toStopIndex].name}! Auto back on route.",
                        nearestAutoEtaMinutes = null,
                        rideDurationMinutes   = null
                    )
                }
            }

            AppPhase.FREE_ROAMING -> {
                val eta  = SimulationEngine.nearestFreeEta(movedAutos, fromPos, s.routeLengthKm)
                val ride = eta?.let {
                    val fastAuto = movedAutos.filter { a -> a.state == AutoState.FREE }
                        .minByOrNull { a -> SimulationEngine.etaMinutes(a, fromPos, s.routeLengthKm) }
                    fastAuto?.let { a ->
                        SimulationEngine.rideDurationMinutes(fromPos, toPos, a.speedKmph, s.routeLengthKm)
                    }
                }
                updated = updated.copy(
                    nearestAutoEtaMinutes = eta,
                    rideDurationMinutes   = ride
                )
            }

            else -> {}
        }

        _state.value = updated
    }

    private fun isSelectionEditable(s: SimulationState) =
        s.phase == AppPhase.IDLE || s.phase == AppPhase.FREE_ROAMING

    override fun onCleared() { super.onCleared(); tickerJob?.cancel() }
}
