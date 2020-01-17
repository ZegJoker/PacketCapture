package com.stanley.packet_capture.utils

import android.content.Context
import android.widget.Toast

val Any.TAG: String
    get() = javaClass.simpleName

internal fun Context.toast(msg: String, duration: Int = Toast.LENGTH_SHORT) = Toast.makeText(this, msg, duration).show()