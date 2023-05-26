package io.github.lumyuan.ux.cleverseekbar.widget

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Interpolator
import io.github.lumyuan.ux.cleverseekbar.R
import io.github.lumyuan.ux.cleverseekbar.widget.CleverSeekBars.OnSeekBarChangeListener
import io.github.lumyuan.ux.core.common.dip2px

@SuppressLint("Recycle", "CustomViewStyleable", "ResourceType")
open class CleverSeekBar : View {

    constructor(context: Context?) : this(context, null)
    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initView(attrs)
    }

    var barWidth: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    private var barThumbWidth: Float = 0f
    private var barThumbMaxWidth: Float = 0f
    private var viewHeight: Float = 0f
    private var thumbFillWidth: Float = 0f

    var barColor: Int = Color.TRANSPARENT
        set(value) {
            field = value
            invalidate()
        }
    var barBackgroundColor: Int = Color.TRANSPARENT
        set(value) {
            field = value
            invalidate()
        }

    private var progressRadius: Float = 0f
    var barProgressRadius: Float = 0f
        set(value) {
            field = value
            progressRadius = value
            invalidate()
        }

    private var thumbRadius: Float = 0f
    private var barThumbMaxRadius: Float = 0f
    var barThumbRadius: Float = 0f
        set(value) {
            field = value
            thumbRadius = value
            barThumbMaxRadius = value * 3
            invalidate()
        }

    var barThumbColor: Int = 0
        set(value) {
            field = value
            invalidate()
        }

    var progressMin: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    var progressMax: Float = 0f
        set(value) {
            field = value
            invalidate()
        }

    var changeAnimationDuration: Long = 400
    var changeAnimationInterpolator: Interpolator = AccelerateDecelerateInterpolator()
    var progress: Float = 0f
        set(value) {
            ValueAnimator.ofFloat(field, value).apply {
                duration = changeAnimationDuration
                interpolator = changeAnimationInterpolator
                addUpdateListener {
                    val v = it.animatedValue as Float
                    field = v
                    seekBarChangeListener?.onChanged(this@CleverSeekBar, v)
                    invalidate()
                }
            }.start()
        }

    private var seekBarChangeListener: OnSeekBarChangeListener? = null

    fun setOnSeekBarChangeListener(onSeekBarChangeListener: OnSeekBarChangeListener) {
        this.seekBarChangeListener = onSeekBarChangeListener
    }

    fun getOnSeekBarChangeListener(): OnSeekBarChangeListener? = this.seekBarChangeListener

    private fun initView(attrs: AttributeSet?) {
        val typedArray =
            this.context.obtainStyledAttributes(attrs, R.styleable.material_clever_seekbar)
        barWidth = typedArray.getDimension(
            R.styleable.material_clever_seekbar_barWidth, context.dip2px(
                context.resources.getDimension(
                    io.github.lumyuan.ux.core.R.dimen.extra_small
                )
            ).toFloat()
        )

        barThumbWidth = barWidth * 3f
        barThumbMaxWidth = barThumbWidth * 1.5f
        thumbFillWidth = barWidth

        viewHeight = barThumbMaxWidth + elevationTarget * 2

        barColor = typedArray.getColor(
            R.styleable.material_clever_seekbar_barColor,
            Color.parseColor(context.getString(io.github.lumyuan.ux.core.R.color.seed))
        )

        barBackgroundColor = typedArray.getColor(
            R.styleable.material_clever_seekbar_barBackgroundColor,
            Color.parseColor(context.getString(io.github.lumyuan.ux.core.R.color.progressBackgroundColor))
        )

        val radius = barWidth * .5f
        val backgroundRadius = typedArray.getDimension(
            R.styleable.material_clever_seekbar_barProgressRadius,
            radius
        )

        barProgressRadius = if (backgroundRadius > radius) radius else backgroundRadius

        val thumbRadius = typedArray.getDimension(
            R.styleable.material_clever_seekbar_barThumbRadius,
            radius * 3
        )

        barThumbRadius = if (thumbRadius > radius * 3) radius * 3 else thumbRadius

        barThumbColor = typedArray.getColor(
            R.styleable.material_clever_seekbar_barThumbColor, Color.parseColor(
                context.getString(
                    io.github.lumyuan.ux.core.R.color.white
                )
            )
        )

        progressMin = typedArray.getFloat(R.styleable.material_clever_seekbar_minProgress, 0f)
        progressMax = typedArray.getFloat(R.styleable.material_clever_seekbar_maxProgress, 100f)

        changeAnimationDuration =
            typedArray.getInteger(R.styleable.material_clever_seekbar_duration, 500).toLong()

        val p = typedArray.getFloat(R.styleable.material_clever_seekbar_progress, progressMin)
        if (p > progressMax || p < progressMin) {
            throw IllegalStateException("进度超过范围。（应在最小进度与最大进度之间）")
        }
        progress = p
    }

    //背景画笔
    private val bgPaint by lazy {
        Paint().apply {
            style = Paint.Style.FILL_AND_STROKE
        }
    }

    //进度画笔
    private val progressPaint by lazy {
        Paint().apply {
            style = Paint.Style.FILL_AND_STROKE
        }
    }

    //thumb边框画笔
    private val thumbStrokePaint by lazy {
        Paint().apply {
            style = Paint.Style.FILL_AND_STROKE
        }
    }

    //thumb画笔
    private val thumbFillPaint by lazy {
        Paint().apply {
            style = Paint.Style.FILL_AND_STROKE
        }
    }

    //矩阵
    private val rectF by lazy {
        RectF()
    }

    private var elevation = 0f
    private val elevationTarget = 8f

    /**
     * 获取真实的进度百分比
     */
    private fun getRealProgressPercentage(): Float =
        (progress - progressMin) / (progressMax - progressMin)

    private fun getRealProgress(percentage: Float): Float =
        percentage * (progressMax - progressMin) + progressMin

    private fun initState(canvas: Canvas) {
        bgPaint.color = barBackgroundColor
        progressPaint.color = barColor
        thumbStrokePaint.color = barColor
        thumbFillPaint.color = barThumbColor

        val bgPadding = viewHeight * .5f
        val barRadius = barWidth * .5f

        val thumbFillRadius = thumbFillWidth * .5f

        val thumbWidthRadius = barThumbWidth * .5f

        val progressWidth = (width - viewHeight) * getRealProgressPercentage() + bgPadding

        //绘制背景
        rectF.apply {
            this.left = bgPadding
            this.top = bgPadding - barRadius
            this.right = this.left + (width - viewHeight)
            this.bottom = this.top + barWidth
        }
        canvas.drawRoundRect(rectF, progressRadius, progressRadius, bgPaint)

        //绘制进度条
        rectF.apply {
            this.left = bgPadding
            this.top = bgPadding - barRadius
            this.right = progressWidth
            this.bottom = this.top + barWidth
        }
        canvas.drawRoundRect(rectF, progressRadius, progressRadius, progressPaint)

        //绘制ThumbStroke
        rectF.apply {
            this.left = progressWidth - thumbWidthRadius
            this.top = bgPadding - thumbWidthRadius
            this.right = this.left + barThumbWidth
            this.bottom = this.top + barThumbWidth
        }

        //绘制阴影
        thumbStrokePaint.setShadowLayer(elevation, 0f, 3f, Color.GRAY)
        canvas.drawRoundRect(rectF, thumbRadius, thumbRadius, thumbStrokePaint)

        //绘制Thumb
        rectF.apply {
            this.left = progressWidth - thumbFillRadius
            this.top = bgPadding - thumbFillRadius
            this.right = this.left + thumbFillWidth
            this.bottom = this.top + thumbFillWidth
        }
        canvas.drawRoundRect(rectF, progressRadius, progressRadius, thumbFillPaint)

    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        var width = 0
        var height = 0

        var specMode = MeasureSpec.getMode(widthMeasureSpec)
        var specSize = MeasureSpec.getSize(widthMeasureSpec)
        when (specMode) {
            MeasureSpec.EXACTLY -> width = specSize
            MeasureSpec.AT_MOST -> width = paddingLeft + paddingRight
            MeasureSpec.UNSPECIFIED -> {

            }
        }

        // 设置高度
        specMode = MeasureSpec.getMode(heightMeasureSpec)
        specSize = MeasureSpec.getSize(heightMeasureSpec)
        when (specMode) {
            MeasureSpec.EXACTLY -> height = specSize
            MeasureSpec.AT_MOST -> height = width / 10
            MeasureSpec.UNSPECIFIED -> {
                height = (viewHeight + elevationTarget + .5f).toInt()
            }
        }

        setMeasuredDimension(width, height)
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        initState(canvas)
    }

    private var dt = 0L
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val barHeight = barWidth * 3
        val progressWidth = (width - barHeight)

        val x = event.x
        val rx = if (x < barWidth) {
            barWidth
        } else if (x > progressWidth + barHeight * .5) {
            progressWidth + barHeight * .5f
        } else {
            x
        }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val realProgress = getRealProgress((rx - barWidth) / progressWidth)
                progress = if (realProgress < progressMin) 0f else if (realProgress > progressMax) progressMax else realProgress
                dt = changeAnimationDuration
                parent?.requestDisallowInterceptTouchEvent(true)

                vibrationDown()

                //elevation
                ValueAnimator.ofFloat(elevation, elevationTarget).apply {
                    duration = 400
                    interpolator = changeAnimationInterpolator
                    addUpdateListener {
                        val v = it.animatedValue as Float
                        elevation = v
                    }
                }.start()

                //thumb stroke
                ValueAnimator.ofFloat(barThumbWidth, barThumbMaxWidth).apply {
                    duration = 400
                    interpolator = changeAnimationInterpolator
                    addUpdateListener {
                        val v = it.animatedValue as Float
                        barThumbWidth = v
                    }
                }.start()

                //thumb stroke radius
                ValueAnimator.ofFloat(thumbRadius, barThumbMaxRadius).apply {
                    duration = 400
                    interpolator = changeAnimationInterpolator
                    addUpdateListener {
                        val v = it.animatedValue as Float
                        thumbRadius = v
                    }
                }.start()

                //thumb fill
                ValueAnimator.ofFloat(thumbFillWidth, barWidth * 1.5f).apply {
                    duration = 400
                    interpolator = changeAnimationInterpolator
                    addUpdateListener {
                        val v = it.animatedValue as Float
                        thumbFillWidth = v
                    }
                }.start()

                //progress radius
                ValueAnimator.ofFloat(progressRadius, barProgressRadius * 1.5f).apply {
                    duration = 400
                    interpolator = changeAnimationInterpolator
                    addUpdateListener {
                        val v = it.animatedValue as Float
                        progressRadius = v
                        invalidate()
                    }
                }.start()
            }

            MotionEvent.ACTION_MOVE -> {
                changeAnimationDuration = 0
                val realProgress = getRealProgress((rx - barWidth) / progressWidth)
                progress = if (realProgress < progressMin) 0f else if (realProgress > progressMax) progressMax else realProgress
                invalidate()
            }

            MotionEvent.ACTION_UP -> {
                vibrationUp()

                ValueAnimator.ofFloat(elevation, 0f).apply {
                    duration = 400
                    interpolator = changeAnimationInterpolator
                    addUpdateListener {
                        val v = it.animatedValue as Float
                        elevation = v
                    }
                }.start()
                ValueAnimator.ofFloat(barThumbWidth, barWidth * 3).apply {
                    duration = 400
                    interpolator = changeAnimationInterpolator
                    addUpdateListener {
                        val v = it.animatedValue as Float
                        barThumbWidth = v
                    }
                }.start()
                ValueAnimator.ofFloat(thumbRadius, barThumbRadius).apply {
                    duration = 400
                    interpolator = changeAnimationInterpolator
                    addUpdateListener {
                        val v = it.animatedValue as Float
                        thumbRadius = v
                    }
                }.start()
                ValueAnimator.ofFloat(thumbFillWidth, barWidth).apply {
                    duration = 400
                    interpolator = changeAnimationInterpolator
                    addUpdateListener {
                        val v = it.animatedValue as Float
                        thumbFillWidth = v
                    }
                }.start()
                ValueAnimator.ofFloat(progressRadius, barProgressRadius).apply {
                    duration = 400
                    interpolator = changeAnimationInterpolator
                    addUpdateListener {
                        val v = it.animatedValue as Float
                        progressRadius = v
                        invalidate()
                    }
                }.start()
                changeAnimationDuration = dt
                performClick()
                parent?.requestDisallowInterceptTouchEvent(false)
            }

            MotionEvent.ACTION_CANCEL -> {
                changeAnimationDuration = dt
            }
        }
        return true
    }

    private fun vibrationDown() {
        val flag =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.GESTURE_START else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) HapticFeedbackConstants.KEYBOARD_PRESS else HapticFeedbackConstants.VIRTUAL_KEY
        performHapticFeedback(flag)
    }

    private fun vibrationUp() {
        val flag =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.GESTURE_END else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) HapticFeedbackConstants.KEYBOARD_RELEASE else HapticFeedbackConstants.VIRTUAL_KEY
        performHapticFeedback(flag)
    }
}