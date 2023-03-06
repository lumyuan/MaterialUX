package io.github.lumyuan.ux.groundglass.widget

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import io.github.lumyuan.ux.groundglass.R
import io.github.lumyuan.ux.groundglass.dao.AndroidStockBlurImpl
import io.github.lumyuan.ux.groundglass.dao.AndroidXBlurImpl
import io.github.lumyuan.ux.groundglass.dao.BlurImpl
import io.github.lumyuan.ux.groundglass.dao.EmptyBlurImpl
import io.github.lumyuan.ux.groundglass.dao.SupportLibraryBlurImpl

open class GroundGlassView : View {
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        iniView(attrs)
    }

    private var mDownSampleFactor = 0f // default 4
    private var mOverlayColor = 0 // default #aaffffff
    private var mBlurRadius = 0f // default 10dp (0 < r <= 25)

    private var mBlurImpl: BlurImpl? = null
    private var mDirty = false
    private var mBitmapToBlur: Bitmap? = null
    private var mBlurredBitmap: Bitmap? = null
    private var mBlurringCanvas: Canvas? = null
    private var mIsRendering = false
    private var mPaint: Paint? = null
    private var mRectSrc = Rect()
    private var mRectDst = Rect()

    // mDecorView should be the root view of the activity (even if you are on a different window like a dialog)
    private var mDecorView: View? = null

    // If the view is on different root view (usually means we are on a PopupWindow),
    // we need to manually call invalidate() in onPreDraw(), otherwise we will not be able to see the changes
    private var mDifferentRoot = false
    private var RENDERING_COUNT = 0
    private var BLUR_IMPL = 0

    private fun iniView(attrs: AttributeSet?) {
        mBlurImpl = getBlurImpl() // provide your own by override getBlurImpl()

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.GroundGlassView)
        mBlurRadius = typedArray.getDimension(
            R.styleable.GroundGlassView_blurRadius,
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                10f,
                context.resources.displayMetrics
            )
        )
        mDownSampleFactor = typedArray.getFloat(R.styleable.GroundGlassView_downSampleFactor, 4f)
        mOverlayColor = typedArray.getColor(R.styleable.GroundGlassView_overlayColor, -0x55000001)

        typedArray.recycle()

        mPaint = Paint()
    }

    private fun getBlurImpl(): BlurImpl {
        if (BLUR_IMPL == 0) {
            try {
                val impl = AndroidStockBlurImpl()
                val bmp = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
                impl.prepare(context, bmp, 4f)
                impl.release()
                bmp.recycle()
                BLUR_IMPL = 3
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
        if (BLUR_IMPL == 0) {
            try {
                javaClass.classLoader?.loadClass("androidx.renderscript.RenderScript")
                // initialize RenderScript to load jni impl
                // may throw unsatisfied link error
                val impl = AndroidXBlurImpl()
                val bmp = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
                impl.prepare(context, bmp, 4f)
                impl.release()
                bmp.recycle()
                BLUR_IMPL = 1
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
        if (BLUR_IMPL == 0) {
            try {
                javaClass.classLoader?.loadClass("android.support.v8.renderscript.RenderScript")
                // initialize RenderScript to load jni impl
                // may throw unsatisfied link error
                val impl = SupportLibraryBlurImpl()
                val bmp = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
                impl.prepare(context, bmp, 4f)
                impl.release()
                bmp.recycle()
                BLUR_IMPL = 2
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
        if (BLUR_IMPL == 0) {
            BLUR_IMPL = -1
        }
        return when (BLUR_IMPL) {
            1 -> AndroidXBlurImpl()
            2 -> SupportLibraryBlurImpl()
            3 -> AndroidStockBlurImpl()
            else -> EmptyBlurImpl()
        }
    }

    open fun setBlurRadius(radius: Float) {
        if (mBlurRadius != radius) {
            mBlurRadius = radius
            mDirty = true
            invalidate()
        }
    }

    open fun setDownSampleFactor(factor: Float) {
        require(factor > 0) { "Down sample factor must be greater than 0." }
        if (mDownSampleFactor != factor) {
            mDownSampleFactor = factor
            mDirty = true // may also change blur radius
            releaseBitmap()
            invalidate()
        }
    }

    open fun setOverlayColor(color: Int) {
        if (mOverlayColor != color) {
            mOverlayColor = color
            invalidate()
        }
    }

    private fun releaseBitmap() {
        if (mBitmapToBlur != null) {
            mBitmapToBlur!!.recycle()
            mBitmapToBlur = null
        }
        if (mBlurredBitmap != null) {
            mBlurredBitmap!!.recycle()
            mBlurredBitmap = null
        }
    }

    protected open fun release() {
        releaseBitmap()
        mBlurImpl!!.release()
    }

    protected open fun prepare(): Boolean {
        if (mBlurRadius == 0f) {
            release()
            return false
        }
        var downsampleFactor: Float = mDownSampleFactor
        var radius = mBlurRadius / downsampleFactor
        if (radius > 25) {
            downsampleFactor = downsampleFactor * radius / 25
            radius = 25f
        }
        val width = width
        val height = height
        val scaledWidth = Math.max(1, (width / downsampleFactor).toInt())
        val scaledHeight = Math.max(1, (height / downsampleFactor).toInt())
        var dirty = mDirty
        if (mBlurringCanvas == null || mBlurredBitmap == null || mBlurredBitmap!!.width != scaledWidth || mBlurredBitmap!!.height != scaledHeight) {
            dirty = true
            releaseBitmap()
            var r = false
            try {
                mBitmapToBlur =
                    Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888)
                if (mBitmapToBlur == null) {
                    return false
                }
                mBlurringCanvas = Canvas(mBitmapToBlur!!)
                mBlurredBitmap =
                    Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888)
                if (mBlurredBitmap == null) {
                    return false
                }
                r = true
            } catch (e: OutOfMemoryError) {
                // Bitmap.createBitmap() may cause OOM error
                // Simply ignore and fallback
            } finally {
                if (!r) {
                    release()
                    return false
                }
            }
        }
        if (dirty) {
            mDirty = if (mBlurImpl!!.prepare(context, mBitmapToBlur, radius)) {
                false
            } else {
                return false
            }
        }
        return true
    }

    protected open fun blur(bitmapToBlur: Bitmap?, blurredBitmap: Bitmap?) {
        mBlurImpl!!.blur(bitmapToBlur, blurredBitmap)
    }

    private val preDrawListener = ViewTreeObserver.OnPreDrawListener {
        val locations = IntArray(2)
        var oldBmp = mBlurredBitmap
        val decor = mDecorView
        if (decor != null && isShown && prepare()) {
            val redrawBitmap = mBlurredBitmap != oldBmp
            oldBmp = null
            decor.getLocationOnScreen(locations)
            var x = -locations[0]
            var y = -locations[1]
            getLocationOnScreen(locations)
            x += locations[0]
            y += locations[1]

            // just erase transparent
            mBitmapToBlur!!.eraseColor(mOverlayColor and 0xffffff)
            val rc = mBlurringCanvas!!.save()
            mIsRendering = true
            RENDERING_COUNT++
            try {
                mBlurringCanvas!!.scale(
                    1f * mBitmapToBlur!!.width / width,
                    1f * mBitmapToBlur!!.height / height
                )
                mBlurringCanvas!!.translate(-x.toFloat(), -y.toFloat())
                if (decor.background != null) {
                    decor.background.draw(mBlurringCanvas!!)
                }
                decor.draw(mBlurringCanvas)
            } catch (e: StopException) {
            } finally {
                mIsRendering = false
                RENDERING_COUNT--
                mBlurringCanvas!!.restoreToCount(rc)
            }
            blur(mBitmapToBlur, mBlurredBitmap)
            if (redrawBitmap || mDifferentRoot) {
                invalidate()
            }
        }
        true
    }

    protected open fun getActivityDecorView(): View? {
        var ctx = context
        var i = 0
        while (i < 4 && ctx != null && ctx !is Activity && ctx is ContextWrapper) {
            ctx = ctx.baseContext
            i++
        }
        return if (ctx is Activity) {
            ctx.window.decorView
        } else {
            null
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mDecorView = getActivityDecorView()
        if (mDecorView != null) {
            mDecorView?.viewTreeObserver?.addOnPreDrawListener(preDrawListener)
            mDifferentRoot = mDecorView?.rootView !== rootView
            if (mDifferentRoot) {
                mDecorView?.postInvalidate()
            }
        } else {
            mDifferentRoot = false
        }
    }

    override fun onDetachedFromWindow() {
        if (mDecorView != null) {
            mDecorView?.viewTreeObserver?.removeOnPreDrawListener(preDrawListener)
        }
        release()
        super.onDetachedFromWindow()
    }

    override fun draw(canvas: Canvas?) {
        if (mIsRendering) {
            // Quit here, don't draw views above me
            throw STOP_EXCEPTION
        } else if (RENDERING_COUNT > 0) {
            // Doesn't support blurview overlap on another blurview
        } else {
            super.draw(canvas)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawBlurredBitmap(canvas, mBlurredBitmap, mOverlayColor)
    }

    /**
     * Custom draw the blurred bitmap and color to define your own shape
     *
     * @param canvas
     * @param blurredBitmap
     * @param overlayColor
     */
    protected open fun drawBlurredBitmap(
        canvas: Canvas,
        blurredBitmap: Bitmap?,
        overlayColor: Int
    ) {
        if (blurredBitmap != null) {
            mRectSrc.right = blurredBitmap.width
            mRectSrc.bottom = blurredBitmap.height
            mRectDst.right = width
            mRectDst.bottom = height
            canvas.drawBitmap(blurredBitmap, mRectSrc, mRectDst, null)
        }
        mPaint!!.color = overlayColor
        canvas.drawRect(mRectDst, mPaint!!)
    }

    private class StopException : RuntimeException()

    private val STOP_EXCEPTION = StopException()
}