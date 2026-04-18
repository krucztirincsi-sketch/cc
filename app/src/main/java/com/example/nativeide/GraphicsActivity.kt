package com.example.nativeide

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class GraphicsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(GraphicalSurfaceView(this))
    }
}
