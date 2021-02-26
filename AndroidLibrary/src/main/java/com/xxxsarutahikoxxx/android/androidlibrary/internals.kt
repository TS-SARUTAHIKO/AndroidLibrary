package com.xxxsarutahikoxxx.android.androidlibrary

import android.util.Log
import java.lang.RuntimeException

internal var out : Any?
    get() = throw RuntimeException("")
    set(value) { Log.d("標準出力", "$value") }