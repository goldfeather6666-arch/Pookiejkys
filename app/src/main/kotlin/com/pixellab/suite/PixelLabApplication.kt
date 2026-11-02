package com.pixellab.suite

import android.app.Application
import com.pixellab.suite.data.PixelLabRepository

class PixelLabApplication : Application() {
    lateinit var repository: PixelLabRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = PixelLabRepository(applicationContext)
    }
}
