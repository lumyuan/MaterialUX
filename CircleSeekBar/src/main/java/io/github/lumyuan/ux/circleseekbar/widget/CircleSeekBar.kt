package io.github.lumyuan.ux.circleseekbar.widget

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.Cap
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import io.github.lumyuan.ux.circleseekbar.R
import io.github.lumyuan.ux.core.common.dip2px
import java.text.DecimalFormat
import kotlin.math.abs

class CircleSeekBar: View {

    /* 最小宽度，单位为dp */
    private val MIN_WIDTH = 50f

    /* 最小高度，单位为dp */
    private val MIN_HEIGHT = 50f

    /* 默认模式 */
    var MODE_DEFAULT = 0

    /* 笔画模式 */
    var MODE_STROKE = 0

    /* 填充模式 */
    var MODE_FILL = 1

    /* 笔画&填充模式 */
    var MODE_FILL_AND_STROKE = 2

    /* 进度格式化默认值 */
    private val PROGRESS_FORMAT_DEFAULT = "#.00"

    /* 进度默认最大值 */
    private val MAX_PROGRESS_DEFAULT = 100f

    /* 开始位置角度默认值 */
    private val START_ANGLE_DEFAULT = 270f

    /* 刷新滑动速度默认值 */
    private val VELOCITY_DEFAULT = 3.0f

    /* 文字大小默认值,单位为sp */
    private val TEXT_SIZE_DEFAULT = 10.0f

    /* 默认文字颜色 */
    private val TEXT_COLOR_DEFAULT = -0x40adae

    /* 进度条边框宽度默认值,单位为dp */
    private val PROGRESS_WIDTH_DEFAULT = 5.0f

    /* 默认进度颜色 */
    private val PROGRESS_COLOR_DEFAULT = -0xc27a3a

    /* 进度条底色默认值，单位为dp */
    private val S_PROGRESS_WIDTH_DEFAULT = 2.0f

    /* 默认进度颜色 */
    private val S_PROGRESS_COLOR_DEFAULT = -0x222223

    private var mPaint: Paint? = null
    private var mTextPaint: Paint? = null
    private var mProgressPaint: Paint? = null
    private var mSProgressPaint: Paint? = null

    private var mMode = 0 // 进度模式

    private var mMaxProgress = 0f // 最大进度

    private var mShowText = false // 是否显示文字

    private var mStartAngle = 0f // 起始角度

    private var mVelocity = 0f // 速度

    private var mTextSize = 0f // 字体大小

    private var mTextColor = 0 // 字体颜色

    private var mProgressStrokeWidth = 0f // 进度条宽度

    private var mProgressColor = 0 // 进度颜色

    private var mSProgressStrokeWidth = 0f // 二级进度宽度

    private var mSProgressColor = 0 // 二级进度颜色

    private var mFadeEnable = false // 是否开启淡入淡出效果

    private var mStartAlpha = 0 // 开始透明度,0~255

    private var mEndAlpha = 0 // 结束透明度,0~255

    private var mZoomEnable = false // 二级进度缩放

    private var mCapRound = false // 进度条首尾是否圆角


    private var mProgressRect: RectF? = null
    private var mSProgressRect: RectF? = null
    private var mTextBounds: Rect? = null

    private var mCurrentAngle = 0f // 当前角度

    private var mUseCenter = false // 是否从中心绘制

    private var mFormat: DecimalFormat? = null // 格式化数值


    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {
        if (attrs != null) {
            val type = context.obtainStyledAttributes(
                attrs,
                R.styleable.CircleSeekBar
            )
            mMode = type.getInt(R.styleable.CircleSeekBar_mode, MODE_DEFAULT)
            mMaxProgress = type.getFloat(
                R.styleable.CircleSeekBar_maxProgress,
                MAX_PROGRESS_DEFAULT
            )
            mShowText = type.getBoolean(
                R.styleable.CircleSeekBar_showText,
                true
            )
            mStartAngle = type.getFloat(
                R.styleable.CircleSeekBar_startAngle,
                START_ANGLE_DEFAULT
            )
            mVelocity = type.getFloat(
                R.styleable.CircleSeekBar_velocity,
                VELOCITY_DEFAULT
            )
            mTextSize = type.getDimension(
                R.styleable.CircleSeekBar_textSize,
                context.dip2px(TEXT_SIZE_DEFAULT).toFloat()
            )
            mTextColor = type.getColor(
                R.styleable.CircleSeekBar_textColor,
                TEXT_COLOR_DEFAULT
            )
            mProgressStrokeWidth = type.getDimension(
                R.styleable.CircleSeekBar_progressWidth,
                context.dip2px(PROGRESS_WIDTH_DEFAULT).toFloat()
            )
            mProgressColor = type.getColor(
                R.styleable.CircleSeekBar_progressColor,
                PROGRESS_COLOR_DEFAULT
            )
            mSProgressStrokeWidth = type.getDimension(
                R.styleable.CircleSeekBar_sProgressWidth,
                context.dip2px(S_PROGRESS_WIDTH_DEFAULT).toFloat()
            )
            mSProgressColor = type.getColor(
                R.styleable.CircleSeekBar_sProgressColor,
                S_PROGRESS_COLOR_DEFAULT
            )
            mFadeEnable = type.getBoolean(
                R.styleable.CircleSeekBar_fadeEnable,
                false
            )
            mStartAlpha = type
                .getInt(R.styleable.CircleSeekBar_startAlpha, 255)
            mEndAlpha = type.getInt(R.styleable.CircleSeekBar_endAlpha, 255)
            mZoomEnable = type.getBoolean(
                R.styleable.CircleSeekBar_zoomEnable,
                false
            )
            mCapRound = type.getBoolean(
                R.styleable.CircleSeekBar_capRound,
                true
            )
            var progress = type.getFloat(
                R.styleable.CircleSeekBar_progress,
                0f
            )
            progress = if (progress > mMaxProgress || progress < 0f) 0f else progress
            mCurrentAngle = progress / mMaxProgress * 360f
            type.recycle()
        } else {
            mMode = MODE_DEFAULT
            mMaxProgress = MAX_PROGRESS_DEFAULT
            mStartAngle = START_ANGLE_DEFAULT
            mVelocity = VELOCITY_DEFAULT
            mTextSize = TEXT_SIZE_DEFAULT
            mTextColor = TEXT_COLOR_DEFAULT
            mProgressStrokeWidth = PROGRESS_WIDTH_DEFAULT
            mProgressColor = PROGRESS_COLOR_DEFAULT
            mSProgressStrokeWidth = S_PROGRESS_WIDTH_DEFAULT
            mSProgressColor = S_PROGRESS_COLOR_DEFAULT
            mCurrentAngle = 0f
            mStartAlpha = 255
            mEndAlpha = 255
            mZoomEnable = false
            mCapRound = true
        }
        mPaint = Paint()
        mPaint?.isAntiAlias = true
        mTextPaint = Paint(mPaint)
        mTextPaint?.color = mTextColor
        mTextPaint?.textSize = mTextSize
        mProgressPaint = Paint(mPaint)
        mProgressPaint?.color = mProgressColor
        mProgressPaint?.strokeWidth = mProgressStrokeWidth
        mSProgressPaint = Paint(mProgressPaint)
        mSProgressPaint?.color = mSProgressColor
        mSProgressPaint?.strokeWidth = mSProgressStrokeWidth
        if (mCapRound) {
            mProgressPaint?.strokeCap = Cap.ROUND
        }
        mUseCenter = when(mMode){
            MODE_FILL_AND_STROKE -> {
                mProgressPaint?.style = Paint.Style.FILL
                mSProgressPaint?.style = Paint.Style.FILL_AND_STROKE
                true
            }
            MODE_FILL -> {
                mProgressPaint?.style = Paint.Style.FILL
                mSProgressPaint?.style = Paint.Style.FILL
                true
            }
            else  -> {
                mProgressPaint?.style = Paint.Style.STROKE
                mSProgressPaint?.style = Paint.Style.STROKE
                false
            }
        }
        mProgressRect = RectF()
        mTextBounds = Rect()
        mFormat = DecimalFormat(PROGRESS_FORMAT_DEFAULT)
    }

    @SuppressLint("DrawAllocation")
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        /* 计算控件宽度与高度 */
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val width: Int = if (widthMode == MeasureSpec.EXACTLY) {
            widthSize
        } else {
            (paddingLeft
                    + context.dip2px(MIN_WIDTH) + paddingRight)
        }
        val height: Int = if (heightMode == MeasureSpec.EXACTLY) {
            heightSize
        } else {
            (paddingTop
                    + context.dip2px(MIN_HEIGHT) + paddingBottom)
        }
        setMeasuredDimension(width, height)
        /* 计算进度显示的矩形框 */
        var radius = (if (width > height) height shr 1 else width shr 1).toFloat()
        val maxStrokeWidth = mProgressStrokeWidth.coerceAtLeast(mSProgressStrokeWidth)
        radius = radius - getMaxPadding() - maxStrokeWidth
        val centerX = width shr 1
        val centerY = height shr 1
        mProgressRect!![centerX - radius, centerY - radius, centerX + radius] = centerY + radius
        mSProgressRect = RectF(mProgressRect)
    }

    override fun onDraw(canvas: Canvas) {

        val ratio = mCurrentAngle / 360f
        // 设置透明度
        if (mFadeEnable) {
            val alpha = ((mEndAlpha - mStartAlpha) * ratio).toInt()
            mProgressPaint!!.alpha = alpha
        }
        // 设置二级进度缩放效果
        if (mZoomEnable) {
            zoomSProgressRect(ratio)
        }
        // 绘制二级进度条
        canvas.drawArc(mSProgressRect!!, 0f, 360f, false, mSProgressPaint!!)
        // 绘制进度条
        canvas.drawArc(
            mProgressRect!!, mStartAngle, mCurrentAngle, mUseCenter,
            mProgressPaint!!
        )
        // 绘制字体
        if (mShowText) {
            val text = formatProgress(ratio * mMaxProgress)
            mTextPaint!!.getTextBounds(text, 0, text.length, mTextBounds)
            canvas.drawText(
                text, (width - mTextBounds!!.width() shr 1).toFloat(),
                (
                        (height shr 1) + (mTextBounds!!.height() shr 1)).toFloat(),
                mTextPaint!!
            )
        }
    }

    /**
     * 格式化进度
     *
     * @param progress
     * @return
     */
    private fun formatProgress(progress: Float): String {
        return mFormat!!.format(progress.toDouble()) + "%"
    }

    /**
     * 获取内边距最大值
     *
     * @return
     */
    private fun getMaxPadding(): Int {
        var maxPadding = paddingLeft
        val paddingRight = paddingRight
        val paddingTop = paddingTop
        val paddingBottom = paddingBottom
        if (maxPadding < paddingRight) {
            maxPadding = paddingRight
        }
        if (maxPadding < paddingTop) {
            maxPadding = paddingTop
        }
        if (maxPadding < paddingBottom) {
            maxPadding = paddingBottom
        }
        return maxPadding
    }

    /**
     * 缩放二级进度条
     *
     * @param ratio
     */
    private fun zoomSProgressRect(ratio: Float) {
        val width = mProgressRect!!.width()
        val height = mProgressRect!!.height()
        val centerX = mProgressRect!!.centerX()
        val centerY = mProgressRect!!.centerY()
        val offsetX = width * 0.5f * ratio
        val offsetY = height * 0.5f * ratio
        val left = centerX - offsetX
        val right = centerX + offsetX
        val top = centerY - offsetY
        val bottom = centerY + offsetY
        mSProgressRect!![left, top, right] = bottom
    }

    override fun onDisplayHint(hint: Int) {
        if (hint == VISIBLE) {
            mCurrentAngle = 0f
            invalidate()
        }
        super.onDisplayHint(hint)
    }

    /**
     * 设置目标进度
     *
     * @param progress
     */
    fun setProgress(progress: Float) {
        setProgress(progress, true)
    }

    /**
     * 设置目标进度
     *
     * @param progress
     * 进度值
     * @param isAnim
     * 是否有动画
     */
    fun setProgress(progress: Float, isAnim: Boolean) {
        var p = progress
        p = if (p > mMaxProgress || p < 0f) 0f else p
        ValueAnimator.ofFloat(mCurrentAngle, p / mMaxProgress * 360f).apply {
//            duration = if (isAnim){
//                (abs(p - (mCurrentAngle * mMaxProgress / 360f )) * 10).toLong()
//            }else {
//                0
//            }
            duration = 1500
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                val v = it.animatedValue as Float
                mCurrentAngle = v
                postInvalidate()
            }
        }.start()
    }

    /**
     * 设置进度画笔着色方式
     *
     * @param shader
     */
    fun setProgressShader(shader: Shader?) {
        mProgressPaint!!.shader = shader
        invalidate()
    }

    /**
     * 设置二级进度画笔着色方式
     *
     * @param shader
     */
    fun setSProgressShader(shader: Shader?) {
        mSProgressPaint!!.shader = shader
        invalidate()
    }

    fun setMaxProgress(max: Float) {
        mMaxProgress = max
    }

    fun getMaxProgress(): Float {
        return mMaxProgress
    }

    fun getMode(): Int {
        return mMode
    }

    fun setMode(mMode: Int) {
        this.mMode = mMode
    }

    fun getStartAngle(): Float {
        return mStartAngle
    }

    fun setStartAngle(mStartAngle: Float) {
        this.mStartAngle = mStartAngle
    }

    fun getVelocity(): Float {
        return mVelocity
    }

    fun setVelocity(mVelocity: Float) {
        this.mVelocity = mVelocity
    }

    fun getTextSize(): Float {
        return mTextSize
    }

    fun setTextSize(mTextSize: Float) {
        this.mTextSize = mTextSize
    }

    fun getTextColor(): Int {
        return mTextColor
    }

    fun setTextColor(mTextColor: Int) {
        this.mTextColor = mTextColor
        postInvalidate()
    }

    fun getProgressStrokeWidth(): Float {
        return mProgressStrokeWidth
    }

    fun setProgressStrokeWidth(mProgressStrokeWidth: Float) {
        this.mProgressStrokeWidth = mProgressStrokeWidth
        postInvalidate()
    }

    fun getProgressColor(): Int {
        return mProgressColor
    }

    fun setProgressColor(mProgressColor: Int) {
        this.mProgressColor = mProgressColor
        postInvalidate()
    }

    fun getSProgressStrokeWidth(): Float {
        return mSProgressStrokeWidth
    }

    fun setSProgressStrokeWidth(mSProgressStrokeWidth: Float) {
        this.mSProgressStrokeWidth = mSProgressStrokeWidth
    }

    fun getSProgressColor(): Int {
        return mSProgressColor
    }

    fun setSProgressColor(mSProgressColor: Int) {
        this.mSProgressColor = mSProgressColor
    }

    fun isFadeEnable(): Boolean {
        return mFadeEnable
    }

    fun setFadeEnable(mFadeEnable: Boolean) {
        this.mFadeEnable = mFadeEnable
    }

    fun getStartAlpha(): Int {
        return mStartAlpha
    }

    fun setStartAlpha(mStartAlpha: Int) {
        this.mStartAlpha = mStartAlpha
    }

    fun getEndAlpha(): Int {
        return mEndAlpha
    }

    fun setEndAlpha(mEndAlpha: Int) {
        this.mEndAlpha = mEndAlpha
    }

    fun isZoomEnable(): Boolean {
        return mZoomEnable
    }

    fun setZoomEnable(mZoomEnable: Boolean) {
        this.mZoomEnable = mZoomEnable
    }
}