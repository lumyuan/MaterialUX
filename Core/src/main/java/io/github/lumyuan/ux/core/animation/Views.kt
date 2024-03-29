package io.github.lumyuan.ux.core.animation

import android.animation.ObjectAnimator
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.animation.BounceInterpolator
import android.view.animation.DecelerateInterpolator

const val duration = 150L
const val onLongTime = 750L
const val onDownScale = 0.9f
const val onUpScale = 1f
private val interpolator = DecelerateInterpolator()

/**
 * @author https://github.com/lumyuan
 * @license Apache-2.0 license
 * @copyright 2023 lumyuan
 * View触感反馈扩展函数
 */

private var isGlobalVibrate = true

fun Context.setVibration(isVibration: Boolean) {
    isGlobalVibrate = isVibration
}

fun View.setOnFeedbackListener(
    clickable: Boolean = false/*是否开启点击波纹*/,
    callOnLongClick: Boolean = false/*是否响应长按事件*/,
    isVibration: Boolean = true,
    onLongClick: (View) -> Unit = {},
    click: (View) -> Unit = {}
) {
    val myHandler = Handler(Looper.getMainLooper())
    var cancel = true
    var isLong = false
    val longTouchRunnable = Runnable {
        isLong = true
        vibrationLong(this, isVibration && isGlobalVibrate)
        onLongClick(this)
        cancel = false
        onUp(this)
    }
    if (clickable) {
        isClickable = true
    }
    setOnTouchListener(object : OnTouchListener {
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_UP -> {
                    if (isLong) {
                        //onLongClick(this@setOnFeedbackListener)
                        myHandler.removeCallbacks(longTouchRunnable)
                        return if (!clickable) {
                            true
                        } else {
                            onTouchEvent(event)
                        }
                    } else {
                        if (cancel) {
                            performClick()
                            click(v)
                        }
                    }
                    vibrationUp(v, isVibration && isGlobalVibrate)
                    onUp(v)
                    myHandler.removeCallbacks(longTouchRunnable)
                    return if (!clickable) {
                        true
                    } else {
                        onTouchEvent(event)
                    }
                }

                MotionEvent.ACTION_MOVE -> {
                    val x = event.x
                    val y = event.y
                    if (x < 0 || y < 0 || x > v.measuredWidth || y > v.measuredHeight) {
                        onUp(v)
                        cancel = false
                        myHandler.removeCallbacks(longTouchRunnable)
                        return if (!clickable) {
                            true
                        } else {
                            onTouchEvent(event)
                        }
                    }
                }

                MotionEvent.ACTION_CANCEL -> {
                    onUp(v)
                    cancel = false
                    isLong = false
                    myHandler.removeCallbacks(longTouchRunnable)
                    return if (!clickable) {
                        true
                    } else {
                        onTouchEvent(event)
                    }
                }

                MotionEvent.ACTION_DOWN -> {
                    cancel = true
                    isLong = false
                    onDown(v)
                    vibrationDown(v, isVibration && isGlobalVibrate)
                    if (callOnLongClick) {
                        myHandler.postDelayed(
                            longTouchRunnable, onLongTime
                        )
                    }
                }
            }
            return if (!clickable) {
                true
            } else {
                onTouchEvent(event)
            }
        }
    })
}

private fun onDown(view: View) {
    val scaleX = ObjectAnimator.ofFloat(view, "scaleX", onDownScale)
    scaleX.duration = duration
    scaleX.interpolator = interpolator
    scaleX.start()

    val scaleY = ObjectAnimator.ofFloat(view, "scaleY", onDownScale)
    scaleY.duration = duration
    scaleY.interpolator = interpolator
    scaleY.start()
}

private fun onUp(view: View) {
    val scaleX = ObjectAnimator.ofFloat(view, "scaleX", onUpScale)
    scaleX.duration = duration
    scaleX.interpolator = interpolator
    scaleX.start()

    val scaleY = ObjectAnimator.ofFloat(view, "scaleY", onUpScale)
    scaleY.duration = duration
    scaleY.interpolator = interpolator
    scaleY.start()
}

private fun vibrationDown(view: View, isVibration: Boolean) {
    if (!isVibration){
        return
    }
    val flag =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.GESTURE_START else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) HapticFeedbackConstants.KEYBOARD_PRESS else HapticFeedbackConstants.VIRTUAL_KEY
    view.performHapticFeedback(flag)
}

private fun vibrationUp(view: View, isVibration: Boolean) {
    if (!isVibration){
        return
    }
    val flag =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.GESTURE_END else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) HapticFeedbackConstants.KEYBOARD_RELEASE else HapticFeedbackConstants.VIRTUAL_KEY
    view.performHapticFeedback(flag)
}

private fun vibrationLong(view: View, isVibration: Boolean) {
    if (!isVibration){
        return
    }
    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
}

private var animatorX: ObjectAnimator? = null
private var animatorY: ObjectAnimator? = null

/**
 * 3D触摸动画
 * @param maxAngle 最大偏移角度
 */
fun View.setOnTouchAnimationToRotation(
    maxAngle: Float = 20F,
    isVibration: Boolean = isGlobalVibrate
) {
    var mRotationX: Float
    var mRotationY: Float

    this.setOnTouchListener { v, event ->
        var x = event.x
        var y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mRotationX = getSpecificValueX(v, y) * maxAngle
                mRotationY = getSpecificValueY(v, x) * maxAngle
                startRotationX(v, mRotationX, 200)
                startRotationY(v, mRotationY, 200)
            }

            MotionEvent.ACTION_MOVE -> {
                if (x < 0){
                    x = 0f
                }else if (x > v.width){
                    x = v.width.toFloat()
                }
                if (y < 0){
                    y = 0f
                } else if (y > v.height){
                    y = v.height.toFloat()
                }
                mRotationX = getSpecificValueX(v, y) * maxAngle
                mRotationY = getSpecificValueY(v, x) * maxAngle
                startRotationX(v, mRotationX, 200)
                startRotationY(v, mRotationY, 200)
            }

            MotionEvent.ACTION_UP -> {
                resetState(v, isVibration)
                mRotationX = 0f
                mRotationY = 0f
                performClick()
            }

            MotionEvent.ACTION_CANCEL -> {
                resetState(v, isVibration)
                mRotationX = 0f
                mRotationY = 0f
            }
        }
        true
    }
}

private fun getSpecificValueY(targetView: View, x: Float): Float {
    val halfHeight = targetView.width / 2f
    return -if (x <= halfHeight) {
        (1 - (x / halfHeight))
    } else {
        -((x - halfHeight) / halfHeight)
    }
}

private fun getSpecificValueX(targetView: View, x: Float): Float {
    val halfHeight = targetView.height / 2f
    return -if (x <= halfHeight) {
        -(1 - (x / halfHeight))
    } else {
        ((x - halfHeight) / halfHeight)
    }
}

private fun startRotationX(view: View, `value`: Float, duration: Long = 50) {
    animatorX?.end()
    animatorX = ObjectAnimator.ofFloat(view, "rotationX", view.rotationX, value).apply {
        this.duration = duration
    }
    animatorX?.start()
}

private fun startRotationY(view: View, `value`: Float, duration: Long = 50) {
    animatorY?.end()
    animatorY = ObjectAnimator.ofFloat(view, "rotationY", view.rotationY, value).apply {
        this.duration = duration
    }
    animatorY?.start()
}

private fun resetState(view: View, isVibration: Boolean) {
    var vX = -1
    var lastVX = -1
    var lastX = view.rotationX

    var vY = -1
    var lastVY = -1
    var lastY = view.rotationY
    animatorX?.end()
    animatorY?.end()
    animatorX = ObjectAnimator.ofFloat(view, "rotationX", 0f).apply {
        duration = 750
        interpolator = BounceInterpolator()
        addUpdateListener {
            val f = it.animatedValue as Float
            if (lastX > f){
                vX = 0
            }
            if (lastX < f){
                vX = 1
            }
            if (lastVX != vX){
                simulatedOverBounce(view, isVibration)
            }
            lastVX = vX
            lastX = f
        }
    }
    animatorX?.start()

    animatorY = ObjectAnimator.ofFloat(view, "rotationY", 0f).apply {
        duration = 750
        interpolator = BounceInterpolator()
        addUpdateListener {
            val f = it.animatedValue as Float
            if (lastY > f){
                vY = 0
            }
            if (lastY < f){
                vY = 1
            }
            if (lastVY != vY){
                simulatedOverBounce(view, isVibration)
            }
            lastVY = vY
            lastY = f
        }
    }
    animatorY?.start()
}

private fun simulatedOverBounce(view: View, isVibration: Boolean) {
    if (isVibration) {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }
}