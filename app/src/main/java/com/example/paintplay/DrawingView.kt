package com.example.paintplay

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class DrawingView(context: Context, atts: AttributeSet) : View(context, atts) {

    private var drawPath: CustomPath? = null
    private var canvasBitmap: Bitmap? = null
    private var drawPaint: Paint? = null
    private var canvasPaint: Paint? = null
    private var brushSize: Float = 0f
    private var color = Color.BLACK
    private var canvas: Canvas? = null


    init {
        setupDrawing()
    }

    private fun setupDrawing() {

        drawPaint = Paint()
        drawPath = CustomPath(color, brushSize)
        drawPaint?.color = color
        drawPaint?.style = Paint.Style.STROKE
        drawPaint?.strokeJoin = Paint.Join.ROUND
        drawPaint?.strokeCap = Paint.Cap.ROUND
        canvasPaint = Paint(Paint.DITHER_FLAG)
        brushSize = 20f
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        canvas = Canvas(canvasBitmap!!)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.drawBitmap(canvasBitmap!!, 0f, 0f, canvasPaint)

        drawPaint!!.strokeWidth = drawPath!!.brushThickness
        drawPaint!!.color = drawPath!!.color
        canvas?.drawPath(drawPath!!, drawPaint!!)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {

        val touchX = event?.x
        val touchY = event?.y

        when (event?.action) {

            MotionEvent.ACTION_DOWN -> {
                drawPath!!.color = color
                drawPath!!.brushThickness = brushSize

                drawPath!!.reset()
                drawPath!!.moveTo(touchX!!, touchY!!)

            }

            MotionEvent.ACTION_MOVE -> {
                drawPath!!.lineTo(touchX!!, touchY!!)
            }

            MotionEvent.ACTION_UP -> {
                drawPath = CustomPath(color, brushSize)
            }

            else -> return false
        }
        invalidate()
        return true
    }

    internal inner class CustomPath(var color: Int, var brushThickness: Float) : Path()


}