package com.example.ib2_lab

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

class GraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paintGrid = Paint().apply {
        color = Color.LTGRAY
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    private val paintSignal = Paint().apply {
        color = Color.BLUE
        strokeWidth = 4f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val values = mutableListOf<Float>()

    fun setValues(newValues: List<Float>) {
        values.clear()
        values.addAll(newValues)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()

        // Fundo
        canvas.drawColor(Color.WHITE)

        // ================================
        // GRADE HORIZONTAL
        // ================================

        for (i in 0..5) {

            val y = height * i / 5f

            canvas.drawLine(
                0f,
                y,
                width,
                y,
                paintGrid
            )
        }

        // ================================
        // GRADE VERTICAL
        // ================================

        for (i in 0..10) {

            val x = width * i / 10f

            canvas.drawLine(
                x,
                0f,
                x,
                height,
                paintGrid
            )
        }

        // ================================
        // DESENHA O SINAL
        // ================================

        if (values.size < 2) {
            return
        }

        val path = Path()

        for (i in values.indices) {

            // A tensão deve estar entre 0 e 3,3 V
            val voltage = values[i]
                .coerceIn(0f, 3.3f)

            // Posição X
            val x =
                i.toFloat() /
                        (values.size - 1).toFloat() *
                        width

            // Posição Y
            //
            // 0 V fica embaixo
            // 3,3 V fica em cima
            val y =
                height -
                        (voltage / 3.3f * height)

            if (i == 0) {

                path.moveTo(x, y)

            } else {

                path.lineTo(x, y)
            }
        }

        canvas.drawPath(
            path,
            paintSignal
        )
    }
}