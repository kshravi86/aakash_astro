package com.aakash.astro.util

import android.util.Log
import com.aakash.astro.BuildConfig

/**
 * Centralised logging wrapper for the app.
 *
 * All output goes to Android logcat under the "AakashAstro" tag so log entries
 * can be filtered in one command: `adb logcat -s AakashAstro`.
 *
 * Stacktraces from [Throwable] arguments are only printed in DEBUG builds to
 * keep release logs concise and avoid leaking internal implementation details.
 */
object AppLog {
    private const val TAG = "AakashAstro"

    /** Emitted only in DEBUG builds — use for verbose trace output. */
    fun d(message: String) {
        if (BuildConfig.DEBUG) Log.d(TAG, message)
    }

    /** Always emitted — use for noteworthy lifecycle or state transitions. */
    fun i(message: String) {
        Log.i(TAG, message)
    }

    /**
     * Always emitted. In DEBUG builds the full stacktrace of [tr] is included;
     * in release builds only the message is logged to avoid log spam.
     */
    fun w(message: String, tr: Throwable? = null) {
        if (tr != null && BuildConfig.DEBUG) {
            Log.w(TAG, message, tr)
        } else {
            Log.w(TAG, message)
        }
    }

    /**
     * Always emitted. In DEBUG builds the full stacktrace of [tr] is included;
     * in release builds only the message is logged.
     */
    fun e(message: String, tr: Throwable? = null) {
        if (tr != null && BuildConfig.DEBUG) {
            Log.e(TAG, message, tr)
        } else {
            Log.e(TAG, message)
        }
    }
}
