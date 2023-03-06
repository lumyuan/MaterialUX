package io.github.lumyuan.ux.groundglass.dao

import android.content.Context
import android.graphics.Bitmap

class EmptyBlurImpl: BlurImpl {
    override fun prepare(context: Context?, buffer: Bitmap?, radius: Float): Boolean {
        return false
    }

    override fun release() {}

    override fun blur(input: Bitmap?, output: Bitmap?) {}
}