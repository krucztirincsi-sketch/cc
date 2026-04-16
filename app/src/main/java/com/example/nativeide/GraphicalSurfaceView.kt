package com.example.nativeide

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView

/**
 * A simplified representation of a Native Graphical Surface.
 * In a real X11 implementation, this would be an XServer rendering to a Surface.
 */
class GraphicalSurfaceView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback, Runnable {

    private var isRunning = false
    private var thread: Thread? = null
    private val paint = Paint()

    init {
        holder.addCallback(this)
        paint.color = Color.WHITE
        paint.textSize = 50f
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        isRunning = true
        thread = Thread(this)
        thread?.start()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        isRunning = false
        thread?.join()
    }

    override fun run() {
        while (isRunning) {
            val canvas = holder.lockCanvas()
            if (canvas != null) {
                drawContent(canvas)
                holder.unlockCanvasAndPost(canvas)
            }
            Thread.sleep(16) // ~60fps
        }
    }

    private fun drawContent(canvas: Canvas) {
        canvas.drawColor(Color.BLACK)
        canvas.drawText("X11 Graphical Output (Simulated)", 100f, 200f, paint)
        canvas.drawCircle(300f, 500f, 100f, paint)
    }
}
