package com.safephrase.core.util

import timber.log.Timber

/** Extension helpers for structured, redacted logging. */
object SafeLogger {
    fun d(tag: String, msg: String) = Timber.tag(tag).d(msg)
    fun i(tag: String, msg: String) = Timber.tag(tag).i(msg)
    fun w(tag: String, msg: String) = Timber.tag(tag).w(msg)
    fun e(tag: String, msg: String, t: Throwable? = null) = Timber.tag(tag).e(t, msg)

    /** Redact the middle of a string (e.g. auth tokens). */
    fun redact(value: String): String = when {
        value.length <= 6 -> "***"
        else -> "${value.take(3)}***${value.takeLast(3)}"
    }
}
