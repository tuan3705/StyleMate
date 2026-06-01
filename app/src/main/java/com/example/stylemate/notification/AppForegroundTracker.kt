package com.example.stylemate.notification

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

object AppForegroundTracker : DefaultLifecycleObserver {

    @Volatile
    var isForeground: Boolean = false
        private set

    override fun onStart(owner: LifecycleOwner) {
        isForeground = true
    }

    override fun onStop(owner: LifecycleOwner) {
        isForeground = false
    }
}

