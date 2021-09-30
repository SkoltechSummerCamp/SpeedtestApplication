package ru.scoltech.openran.speedtest

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import kotlinx.coroutines.*
import java.util.*
import kotlin.math.*
import kotlin.random.Random

class Wave(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
    private var mPaint = Paint()
    private var mCurrentSpeed = 0
    private var redrawJob: Job? = null

    private val backgroundHarmonics = HarmonicSum()
    private val foregroundHarmonics = HarmonicSum()

    init {
        mPaint.strokeWidth = 1f
        mPaint.isAntiAlias = true
        mPaint.style = Paint.Style.FILL
    }

    fun start() {
        stop()
        redrawJob = CoroutineScope(Dispatchers.Default).launch {
            while (true) {
                delay(1000 / FPS)
                this@Wave.postInvalidate()
            }
        }
    }

    fun stop() {
        redrawJob?.let {
            runBlocking {
                it.cancel()
                it.cancelAndJoin()
            }
        }
    }

    private inline fun buildFunctionPath(f: (Float) -> Float): Path {
        val path = Path()
        path.moveTo(width.toFloat(), height.toFloat())
        path.lineTo(0f, height.toFloat())
        (0..width)
            .map { it to f(MAX_X * it.toFloat() / width) }
            .forEach { (x, y) -> path.lineTo(x.toFloat(), y) }
        path.close()
        return path
    }

    public override fun onDraw(canvas: Canvas) {
        mPaint.alpha = BACKGROUND_ALPHA
        backgroundHarmonics.update()
        canvas.drawPath(buildFunctionPath(backgroundHarmonics), mPaint)

        mPaint.alpha = FOREGROUND_ALPHA
        foregroundHarmonics.update()
        canvas.drawPath(buildFunctionPath(foregroundHarmonics), mPaint)
    }

    fun attachSpeed(speed: Int) { // attach current instant speed to wave
        mCurrentSpeed = speed
    }

    fun attachColor(color: Int) {
        mPaint.color = color
    }

    private data class Harmonic(
        val amplitude: Float,
        var frequency: Float,
        var initialPhase: Float,
        var amplitudeCyclicScale: Float = 0f,
    ): (Float) -> Float {
        val amplitudeScale: Float
            get() = abs(amplitudeCyclicScale % MAX_CYCLIC_SCALE - MAX_CYCLIC_SCALE / 2)
                .minus(MAX_CYCLIC_SCALE / 4)

        override fun invoke(p1: Float): Float {
            return amplitudeScale * amplitude * sin(frequency * p1 + initialPhase) + amplitude
        }
    }

    private class HarmonicSum : (Float) -> Float {
        private val harmonics = FREQUENCIES.map { frequency ->
            Harmonic(
                MAX_AMPLITUDE,
                frequency,
                Random.nextFloat() * MAX_STARTING_INITIAL_PHASE,
                Random.nextFloat() * MAX_CYCLIC_SCALE
            )
        }

        override fun invoke(p1: Float): Float {
            return harmonics.sumOf { it(p1).toDouble() }.toFloat()
        }

        fun update() {
            harmonics.forEachIndexed { index, harmonic ->
                harmonic.amplitudeCyclicScale += AMPLITUDE_CYCLIC_SCALE_STEP
                if (abs(harmonic.amplitudeCyclicScale) < MUTATION_AMPLITUDE_SCALE_THRESHOLD) {
                    harmonic.initialPhase += Random.nextFloat()
                        .times(MAX_INITIAL_PHASE_STEP)
                        .times(if (index % 2 == 0) -1 else 1)
                    harmonic.frequency = FREQUENCIES[index]
                        .plus(Random.nextFloat() * FREQUENCY_SEGMENT_LENGTH)
                        .minus(FREQUENCY_SEGMENT_LENGTH / 2)
                }
            }
        }
    }

    companion object {
        private const val FPS = 60L
        private const val MAX_AMPLITUDE = 13f
        private val FREQUENCIES = List(8) { 1 - it.toFloat() / 10 }
        private const val MAX_STARTING_INITIAL_PHASE = 5f
        private const val MAX_CYCLIC_SCALE = 4f
        private const val MUTATION_AMPLITUDE_SCALE_THRESHOLD = 0.05f
        private const val AMPLITUDE_CYCLIC_SCALE_STEP = 0.1f
        private const val MAX_INITIAL_PHASE_STEP = 0.4f
        private const val FREQUENCY_SEGMENT_LENGTH = 0.08f
        private const val MAX_X = 25f
        private const val BACKGROUND_ALPHA = 128
        private const val FOREGROUND_ALPHA = 255
    }
}