package io.github.lumyuan.ux.groundglass.dao

import android.content.Context
import android.graphics.Bitmap

interface BlurImpl {
    fun prepare(context: Context?, buffer: Bitmap?, radius: Float): Boolean
    fun release()
    fun blur(input: Bitmap?, output: Bitmap?)
}