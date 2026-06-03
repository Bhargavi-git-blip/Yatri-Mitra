package com.yatrimitra.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.yatrimitra.app.R
import com.yatrimitra.app.model.AppPhase
import com.yatrimitra.app.model.AutoState
import com.yatrimitra.app.model.SimulationState
import com.yatrimitra.app.network.SessionManager
import com.yatrimitra.app.simulation.SimulationEngine
import com.yatrimitra.app.ui.auth.LoginActivity
import com.yatrimitra.app.viewmodel.RouteViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val vm: RouteViewModel by viewModels()

    private lateinit var toolbar         : Toolbar
    private lateinit var tvUserGreeting  : TextView
    private lateinit var canvas          : RouteCanvasView
    private lateinit var btnToggle       : Button
    private lateinit var btnBook         : Button
    private lateinit var btnAddAuto      : Button
    private lateinit var btnReset        : Button
    private lateinit var tvBadge         : TextView
    private lateinit var fromSpinner     : Spinner
    private lateinit var toSpinner       : Spinner
    private lateinit var tvJbFrom        : TextView
    private lateinit var tvJbTo          : TextView
    private lateinit var tvJbDist        : TextView
    private lateinit var tvEta           : TextView
    private lateinit var tvRide          : TextView
    private lateinit var tvRideLabel     : TextView
    private lateinit var waitSection     : View
    private lateinit var progressBar     : ProgressBar
    private lateinit var tvWaitTimer     : TextView
    private lateinit var otpSection      : View
    private lateinit var tvOtpDisplay    : TextView
    private lateinit var tvOtpMsg        : TextView
    private lateinit var etOtpInput      : EditText
    private lateinit var btnOtpConfirm   : Button
    private lateinit var tvOtpError      : TextView
    private lateinit var statusBar       : TextView
    private lateinit var bookHint        : TextView
    private lateinit var tvTripLog       : TextView

    private var spinnerFromUpdating = false
    private var spinnerToUpdating   = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SessionManager.init(this)
        setContentView(R.layout.activity_main)

        bindViews()
        toolbar.title = ""
        setSupportActionBar(toolbar)

        tvUserGreeting.text = "Hello, ${SessionManager.getName()} 👋"

        setupSpinners()
        setupListeners()
        startObserving()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> { confirmLogout(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun confirmLogout() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                vm.resetAll()
                SessionManager.clearSession()
                startActivity(Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun bindViews() {
        toolbar          = findViewById(R.id.toolbar)
        tvUserGreeting   = findViewById(R.id.tvUserGreeting)
        canvas           = findViewById(R.id.routeCanvas)
        btnToggle        = findViewById(R.id.btnToggle)
        btnBook          = findViewById(R.id.btnBook)
        btnAddAuto       = findViewById(R.id.btnAddAuto)
        btnReset         = findViewById(R.id.btnReset)
        tvBadge          = findViewById(R.id.tvBadge)
        fromSpinner      = findViewById(R.id.fromSpinner)
        toSpinner        = findViewById(R.id.toSpinner)
        tvJbFrom         = findViewById(R.id.tvJbFrom)
        tvJbTo           = findViewById(R.id.tvJbTo)
        tvJbDist         = findViewById(R.id.tvJbDist)
        tvEta            = findViewById(R.id.tvEta)
        tvRide           = findViewById(R.id.tvRide)
        tvRideLabel      = findViewById(R.id.tvRideLabel)
        waitSection      = findViewById(R.id.waitSection)
        progressBar      = findViewById(R.id.progressBar)
        tvWaitTimer      = findViewById(R.id.tvWaitTimer)
        otpSection       = findViewById(R.id.otpSection)
        tvOtpDisplay     = findViewById(R.id.tvOtpDisplay)
        tvOtpMsg         = findViewById(R.id.tvOtpMsg)
        etOtpInput       = findViewById(R.id.etOtpInput)
        btnOtpConfirm    = findViewById(R.id.btnOtpConfirm)
        tvOtpError       = findViewById(R.id.tvOtpError)
        statusBar        = findViewById(R.id.statusBar)
        bookHint         = findViewById(R.id.bookHint)
        tvTripLog        = findViewById(R.id.tvTripLog)
    }

    private fun setupSpinners() {
        val names = SimulationEngine.defaultStops.map { it.name }
        val adapterFrom = ArrayAdapter(this, android.R.layout.simple_spinner_item, names).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        val adapterTo = ArrayAdapter(this, android.R.layout.simple_spinner_item, names).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        fromSpinner.adapter = adapterFrom
        toSpinner.adapter   = adapterTo

        fromSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                if (!spinnerFromUpdating) vm.setFromStop(pos)
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
        toSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                if (!spinnerToUpdating) vm.setToStop(pos)
            }
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    private fun setupListeners() {
        btnToggle.setOnClickListener  { vm.toggleSimulation() }
        btnBook.setOnClickListener    { vm.bookRide() }
        btnAddAuto.setOnClickListener { vm.addAuto() }
        btnReset.setOnClickListener   { vm.resetAll() }
        btnOtpConfirm.setOnClickListener {
            val ok = vm.verifyOtp(etOtpInput.text.toString().trim())
            if (!ok) { tvOtpError.text = "✗ Wrong code. Try again."; etOtpInput.setText("") }
            else tvOtpError.text = ""
        }
        canvas.onStopTapped = { idx -> vm.setFromStop(idx) }
    }

    private fun startObserving() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.state.collect { render(it) }
            }
        }
    }

    private fun render(s: SimulationState) {
        canvas.updateState(s)

        spinnerFromUpdating = true; fromSpinner.setSelection(s.fromStopIndex); spinnerFromUpdating = false
        spinnerToUpdating   = true; toSpinner.setSelection(s.toStopIndex);     spinnerToUpdating   = false
        val editable = s.phase == AppPhase.IDLE || s.phase == AppPhase.FREE_ROAMING
        fromSpinner.isEnabled = editable; toSpinner.isEnabled = editable

        val fStop = s.stops[s.fromStopIndex]; val tStop = s.stops[s.toStopIndex]
        tvJbFrom.text = fStop.name; tvJbTo.text = tStop.name
        val gap = if (tStop.position >= fStop.position) tStop.position - fStop.position else 1f - fStop.position + tStop.position
        tvJbDist.text = "%.1f km".format(gap * s.routeLengthKm)

        tvEta.text = s.nearestAutoEtaMinutes?.let { fmtMin(it) } ?: "—"

        when (s.phase) {
            AppPhase.RIDING  -> { tvRideLabel.text = "Ride Duration"; tvRide.text = s.rideDurationMinutes?.let { fmtMin(it) } ?: "—" }
            AppPhase.ARRIVED -> { tvRideLabel.text = "Status";        tvRide.text = "Arrived ✅" }
            else             -> { tvRideLabel.text = "Ride Duration"; tvRide.text = s.rideDurationMinutes?.let { fmtMin(it) } ?: "—" }
        }

        btnToggle.text = if (s.isRunning) "⏸ Pause" else "▶ Start"
        tvBadge.text = when (s.phase) {
            AppPhase.IDLE            -> "◌ Paused"
            AppPhase.FREE_ROAMING    -> if (s.isRunning) "🛺 Running" else "◌ Paused"
            AppPhase.BOOKED          -> "● Booked"
            AppPhase.WAITING_AT_FROM -> "⏳ Waiting"
            AppPhase.OTP_PENDING     -> "🔔 Confirm"
            AppPhase.RIDING          -> "● In Ride"
            AppPhase.ARRIVED         -> "✅ Arrived"
        }

        val showWait = s.phase == AppPhase.WAITING_AT_FROM
        waitSection.visibility = if (showWait) View.VISIBLE else View.GONE
        if (showWait) {
            val remTicks = SimulationEngine.WAIT_TICKS - s.waitTicksElapsed
            tvWaitTimer.text  = "${(remTicks * SimulationEngine.TICK_MS / 1000).coerceAtLeast(0)}"
            progressBar.progress = ((remTicks.toFloat() / SimulationEngine.WAIT_TICKS) * 100).toInt()
        }

        val showOtp = s.phase == AppPhase.OTP_PENDING
        otpSection.visibility = if (showOtp) View.VISIBLE else View.GONE
        if (showOtp && s.currentOtp != null) {
            tvOtpDisplay.text = s.currentOtp
            tvOtpMsg.text = "${s.autos.find { it.id == s.waitingAutoId }?.label ?: "Auto"} at ${fStop.name}. Ask driver for code."
        }

        val msg = s.statusMessage
        statusBar.visibility = if (msg != null) View.VISIBLE else View.GONE
        if (msg != null) statusBar.text = msg

        val showBook = s.phase == AppPhase.FREE_ROAMING
        btnBook.visibility  = if (showBook) View.VISIBLE else View.GONE
        bookHint.visibility = if (showBook) View.VISIBLE else View.GONE

        tvTripLog.text = if (s.tripLogs.isEmpty()) "No trips yet"
        else s.tripLogs.take(5).joinToString("\n") { "Trip #${it.tripNumber}: ${it.fromStop} → ${it.toStop} (${it.autoLabel})" }
    }

    private fun fmtMin(m: Float): String {
        val mn = m.toInt(); val sc = ((m - mn) * 60).toInt()
        return if (mn > 0) "${mn}m ${sc}s" else "${sc}s"
    }
}
