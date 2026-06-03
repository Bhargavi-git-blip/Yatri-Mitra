package com.yatrimitra.app.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.yatrimitra.app.model.AppPhase
import com.yatrimitra.app.model.Auto
import com.yatrimitra.app.model.AutoState
import com.yatrimitra.app.model.SimulationState

class RouteCanvasView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    var onStopTapped: ((Int) -> Unit)? = null
    private var state: SimulationState? = null

    // Paints
    private val routeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeWidth = 16f
        color = Color.parseColor("#FFE0B2")
    }
    private val routeFgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeWidth = 6f
        color = Color.parseColor("#FF8F00")
    }
    private val segPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeWidth = 6f
        color = Color.parseColor("#FF8F00")
    }
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL; color = Color.parseColor("#30000000")
    }
    private val autoLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = 22f; textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val speedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 18f; textAlign = Paint.Align.CENTER
    }
    private val stopLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 20f; textAlign = Paint.Align.CENTER
    }
    private val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 18f; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD
    }

    private val pad  = 64f
    private val rY   get() = height * 0.55f
    private val autoR = 24f
    private val stopR = 10f

    fun updateState(s: SimulationState) { state = s; invalidate() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val s = state ?: return
        val showSeg = s.phase in listOf(
            AppPhase.BOOKED, AppPhase.WAITING_AT_FROM, AppPhase.OTP_PENDING, AppPhase.RIDING
        )
        val fPos = s.stops[s.fromStopIndex].position
        val tPos = s.stops[s.toStopIndex].position

        // Segment highlight band
        if (showSeg) {
            val x1 = pX(minOf(fPos, tPos)); val x2 = pX(maxOf(fPos, tPos))
            val bgPaint = Paint().apply { style = Paint.Style.FILL; color = Color.parseColor("#18FF8F00") }
            canvas.drawRoundRect(x1, rY - 20f, x2, rY + 20f, 10f, 10f, bgPaint)
        }

        // Route background line
        canvas.drawLine(pad, rY, width - pad, rY, routeBgPaint)

        // Active segment
        if (showSeg) {
            segPaint.color = Color.parseColor("#FF8F00")
            canvas.drawLine(pX(fPos), rY, pX(tPos), rY, segPaint)
        }

        // Stops
        s.stops.forEachIndexed { i, stop ->
            val x = pX(stop.position)
            val iF = showSeg && i == s.fromStopIndex
            val iT = showSeg && i == s.toStopIndex

            if (iF || iT) {
                val colBg = Paint().apply {
                    style = Paint.Style.FILL
                    color = if (iF) Color.parseColor("#10 1565C0".replace(" ","")) else Color.parseColor("#10C62828".replace(" ",""))
                }
                canvas.drawRect(x - 32f, 0f, x + 32f, height.toFloat(), colBg)
            }

            // Stop dot shadow
            canvas.drawCircle(x, rY + 4f, if (iF || iT) stopR + 3f else stopR, shadowPaint)

            // Stop dot
            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = when { iF -> Color.parseColor("#1565C0"); iT -> Color.parseColor("#C62828"); else -> Color.parseColor("#FF8F00") }
            }
            canvas.drawCircle(x, rY, if (iF || iT) stopR + 3f else stopR, dotPaint)

            // Ring for selected stops
            if (iF || iT) {
                val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE; strokeWidth = 3f
                    color = if (iF) Color.parseColor("#1565C0") else Color.parseColor("#C62828")
                }
                canvas.drawCircle(x, rY, stopR + 14f, ringPaint)

                tagPaint.color = ringPaint.color
                canvas.drawText(if (iF) "FROM" else "TO", x, rY - stopR - 20f, tagPaint)
            }

            // Stop name below
            stopLabelPaint.typeface = if (iF || iT) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            stopLabelPaint.color = when { iF -> Color.parseColor("#0D47A1"); iT -> Color.parseColor("#B71C1C"); else -> Color.parseColor("#795548") }
            val name = stop.name.let { if (it.length > 9) it.take(8) + "…" else it }
            canvas.drawText(name, x, rY + stopR + 26f, stopLabelPaint)
        }

        // Autos
        s.autos.forEach { a ->
            val x = pX(a.pos); val y = rY - 54f
            val isBoarded = a.id == s.boardedAutoId
            val isStopped = a.state == AutoState.STOPPED
            val col = when { isStopped -> Color.parseColor("#E65100"); isBoarded -> Color.parseColor("#1565C0"); else -> autoHue(a) }
            val litCol = liftColor(col, 0.85f)

            // Shadow
            canvas.drawCircle(x, y + 5f, autoR + 2f, shadowPaint)

            // Body
            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = col }
            canvas.drawCircle(x, y, autoR, bodyPaint)

            // Inner ring
            val innerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE; strokeWidth = 2f; color = litCol
            }
            canvas.drawCircle(x, y, autoR - 5f, innerPaint)

            // Label
            canvas.drawText(a.label, x, y + autoLabelPaint.textSize / 3f, autoLabelPaint)

            // Tag above
            val tag = when { isStopped -> "WAIT"; isBoarded -> "YOU"; else -> "${a.speedKmph.toInt()} km/h" }
            speedPaint.color = col
            speedPaint.typeface = if (isStopped || isBoarded) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            canvas.drawText(tag, x, y - autoR - 10f, speedPaint)

            // Connector dashed line
            val conn = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE; strokeWidth = 2f
                color = Color.argb(70, Color.red(col), Color.green(col), Color.blue(col))
                pathEffect = DashPathEffect(floatArrayOf(5f, 4f), 0f)
            }
            canvas.drawLine(x, y + autoR, x, rY - stopR - 2f, conn)
        }
    }

    private fun pX(pos: Float) = pad + pos * (width - 2 * pad)
    private fun autoHue(a: Auto): Int {
        val hues = listOf(130f, 200f, 270f, 30f, 0f, 55f)
        return Color.HSVToColor(floatArrayOf(hues[a.id % hues.size], 0.65f, 0.38f))
    }
    private fun liftColor(col: Int, amount: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(col, hsv)
        hsv[2] = (hsv[2] + amount).coerceAtMost(1f)
        return Color.HSVToColor(hsv)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val pos = ((event.x - pad) / (width - 2 * pad)).coerceIn(0f, 1f)
            val s   = state ?: return false
            val nearest = s.stops.indices.minByOrNull { i -> Math.abs(s.stops[i].position - pos) } ?: return false
            onStopTapped?.invoke(nearest); performClick(); return true
        }
        return super.onTouchEvent(event)
    }
    override fun performClick(): Boolean { super.performClick(); return true }

    // Need to expose Auto.pos
    private val Auto.pos get() = this.position
}
