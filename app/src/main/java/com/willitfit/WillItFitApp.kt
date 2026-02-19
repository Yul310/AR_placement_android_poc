package com.willitfit

import android.app.Application
import com.google.ar.core.ArCoreApk

class WillItFitApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Check ARCore availability (optional - for early detection)
        checkArCoreAvailability()
    }

    private fun checkArCoreAvailability() {
        val availability = ArCoreApk.getInstance().checkAvailability(this)

        if (availability.isTransient) {
            // Re-query at a later time
            android.os.Handler(mainLooper).postDelayed({
                checkArCoreAvailability()
            }, 200)
        }

        // Store availability status for later use
        isArCoreSupported = when (availability) {
            ArCoreApk.Availability.SUPPORTED_INSTALLED,
            ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD,
            ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED -> true
            else -> false
        }
    }

    companion object {
        var isArCoreSupported: Boolean = false
            private set
    }
}
