package com.example.car

import com.google.android.apps.auto.sdk.CarActivity
import com.google.android.apps.auto.sdk.CarActivityService

/**
 * Android Auto Projection Service matching `thekirankumar/carstream-android-auto` (`CarService`).
 * Tells the Android Auto head unit to launch `MainCarActivity` as a full graphical projection
 * window instead of falling back to the gray MediaBrowser audio template.
 */
class CarService : CarActivityService() {
    override fun getCarActivity(): Class<out CarActivity> {
        return MainCarActivity::class.java
    }
}
