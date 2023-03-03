package io.github.lumyuan.ux.bottomnavigationview.common

import android.content.Context
import android.graphics.Point
import android.view.WindowManager

fun Context.width(): Int {
    val windowManager = this.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val display = windowManager.defaultDisplay
    val outPoint = Point()
    display.getRealSize(outPoint)
    return outPoint.x
}

fun Context.height(): Int {
    val windowManager = this.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val display = windowManager.defaultDisplay
    val outPoint = Point()
    display.getRealSize(outPoint)
    return outPoint.y
}

fun Context.px2dip(pxValue: Float): Int {
    val scale = this.resources.displayMetrics.density
    return (pxValue / scale + 0.5f).toInt()
}

fun Context.dip2px(dipValue: Float): Int {
    val scale = this.resources.displayMetrics.density
    return (dipValue * scale + 0.5f).toInt()
}

fun Context.px2sp(pxValue: Float): Int {
    val fontScale = this.resources.displayMetrics.scaledDensity
    return (pxValue / fontScale + 0.5f).toInt()
}

fun Context.sp2px(spValue: Float): Int {
    val fontScale = this.resources.displayMetrics.scaledDensity
    return (spValue * fontScale + 0.5f).toInt()
}