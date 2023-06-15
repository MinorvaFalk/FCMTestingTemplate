package com.example.fcmtestingtemplate.utils

import android.os.SystemClock
import android.view.View

fun View.clickWithDebounce(debounceTime: Long = 1200L, action: (view: View) -> Unit) {
    this.setOnClickListener(object : View.OnClickListener {
        private var lastClickTime: Long = 0
        override fun onClick(v: View) {
            if (SystemClock.elapsedRealtime() - lastClickTime < debounceTime) return
            else action(this@clickWithDebounce)
            lastClickTime = SystemClock.elapsedRealtime()
        }
    })
}