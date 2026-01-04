package com.aakash.astro.util

import android.util.Log
import com.aakash.astro.BuildConfig

object AppLog {
    private const val TAG = "AakashAstro"

    fun d(message: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, message)
    }

    fun i(message: String) {
        Log.i(TAG, message)
    }

    fun w(message: String, tr: Throwable? = null) {
        if (tr != null && BuildConfig.DEBUG) {
            Log.w(TAG, message, tr)
        } else {
            Log.w(TAG, message)
        }
    }

    fun e(message: String, tr: Throwable? = null) {
        if (tr != null && BuildConfig.DEBUG) {
            Log.e(TAG, message, tr)
        } else {
            Log.e(TAG, message)
        }
    }
}
