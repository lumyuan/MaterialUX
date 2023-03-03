package io.github.lumyuan.ux.bottomnavigationview.widget

import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.LinearLayout.HORIZONTAL
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.setMargins
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.MODE_FIXED
import io.github.lumyuan.ux.bottomnavigationview.LiveData
import io.github.lumyuan.ux.bottomnavigationview.R
import io.github.lumyuan.ux.bottomnavigationview.common.dip2px
import io.github.lumyuan.ux.bottomnavigationview.common.sp2px
import kotlin.math.max
import kotlin.properties.Delegates


class NavigationView : FrameLayout {
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initView(attrs)
    }

    private val items by lazy {
        ArrayList<ItemView>()
    }

    private val itemLayout: LinearLayout by lazy {
        LinearLayout(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            orientation = HORIZONTAL
        }
    }

    private var viewPager: ViewPager? = null

    private var textColor by Delegates.notNull<Int>()
    private var textSize by Delegates.notNull<Float>()
    private var iconColorFilter by Delegates.notNull<Int>()

    private val positionData = LiveData(0)

    @SuppressLint("CustomViewStyleable", "Recycle", "ResourceType")
    private fun initView(attrs: AttributeSet?) {
        val typedArray =
            context.obtainStyledAttributes(attrs, R.styleable.material_bottom_navigation)
        val primaryColor = Color.parseColor(context.getString(R.color.seed))
        textColor = typedArray.getColor(
            R.styleable.material_bottom_navigation_titleColor,
            primaryColor
        )
        textSize = typedArray.getDimension(R.styleable.material_bottom_navigation_titleSize, 14f)
        iconColorFilter = typedArray.getColor(
            R.styleable.material_bottom_navigation_iconColorFilter,
            primaryColor
        )

        indicatorHeight = context.dip2px(5f).toFloat()

        setBackgroundColor(Color.parseColor(context.getString(R.color.backgroundColor)))

        addView(itemLayout)

        //索引变化
        positionData.observe {
            it?.let {
                if (viewPager == null) {
                    items.indices.onEach { index ->
                        val view = items[index].view
                        view.select(it == index)
                    }
                    runIndicatorAnimation(it)
                } else {
                    viewPager?.currentItem = it
                }
                onItemSelectListener(it, items[it].view)
            }
        }
    }

    private var onItemSelectListener: (position: Int, view: View) -> Unit = { _, _ -> }

    fun setOnItemSelectListener(onItemSelectListener: (position: Int, view: View) -> Unit) {
        this.onItemSelectListener = onItemSelectListener
    }

    private fun initItem() {
        itemLayout.removeAllViews()
        items.indices.onEach {
            itemLayout.addView(
                items[it].view.apply {
                    this.setOnClickListener { v ->
                        positionData.value = it
                    }
                },
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    weight = 1f
                }
            )
        }
        post {
            measure(
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            )
            itemWidth = (this.width / items.size).toFloat()
            indicatorWith = context.dip2px(28f).toFloat()
            centerOffset = (itemWidth - indicatorWith) * .5f
            iY = measuredHeight - indicatorHeight - context.dip2px(16f).toFloat()
            paintIndicator.color = textColor
            iLeft = centerOffset
            iRight = iLeft + indicatorWith
            invalidate()
            setCurrentItem(0)
        }
    }

    //指示器画笔
    private val paintIndicator = Paint().apply {
        this.style = Paint.Style.FILL
    }
    private val indicatorRectF = RectF()
    //单个item宽度
    private var itemWidth = 0f
    //指示器宽度
    private var indicatorWith = 0f
    //居中偏移
    private var centerOffset = 0f
    //指示器y坐标
    private var iY = 0f
    //指示器高度
    private var indicatorHeight = 0f
    //左顶点
    private var iLeft = 0f
    //右顶点
    private var iRight = 0f
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        indicatorRectF.apply {
            this.left = iLeft
            this.right = iRight
            this.top = iY
            this.bottom = iY + indicatorHeight
        }
        println("绘制了：$indicatorRectF")
        val radius = indicatorHeight * .5f
        canvas.drawRoundRect(indicatorRectF, radius, radius, paintIndicator)
    }

    var duration = 500L
    private var oldPosition = 0
    private val mHandler by lazy {
        Handler(Looper.getMainLooper())
    }
    private var lr: Runnable? = null
    private var rr: Runnable? = null
    private fun runIndicatorAnimation(position: Int) {
        val tl = centerOffset + itemWidth * position
        val tr = tl + indicatorWith

        lr?.let(mHandler::removeCallbacks)
        rr?.let(mHandler::removeCallbacks)

        lr = Runnable {
            ValueAnimator.ofFloat(iLeft, tl).apply {
                interpolator = AccelerateDecelerateInterpolator()
                duration = this@NavigationView.duration
                addUpdateListener {
                    it.animatedValue?.apply {
                        val v = this.toString().toFloat()
                        iLeft = v
                        invalidate()
                    }
                }
            }.start()
        }

        rr = Runnable {
            ValueAnimator.ofFloat(iRight, tr).apply {
                interpolator = AccelerateDecelerateInterpolator()
                duration = this@NavigationView.duration
                addUpdateListener {
                    it.animatedValue?.apply {
                        val v = this.toString().toFloat()
                        iRight = v
                        invalidate()
                    }
                }
            }.start()
        }

        if (position < oldPosition) {
            lr?.let(mHandler::post)
            rr?.let {
                mHandler.postDelayed(
                    it, 150
                )
            }
        } else {
            rr?.let(mHandler::post)
            lr?.let {
                mHandler.postDelayed(
                    it, 150
                )
            }
        }
        oldPosition = position
    }

    fun newItemView() = ItemView(context).apply {
        this.setColorFilter(iconColorFilter)
        this.view.titleView.setTextColor(textColor)
        this.view.titleView.textSize = this@NavigationView.textSize
    }

    fun removeItemView(position: Int) {
        items.removeAt(position)
    }

    fun addItemView(itemView: ItemView) {
        items.add(itemView)
        initItem()
    }

    fun setCurrentItem(position: Int) {
        positionData.value = position
    }

    fun getCurrentItem() = positionData.value ?: 0

    class ItemView(context: Context) {

        var view: Item

        var tag: Any? = null

        init {
            view = Item(context)
        }

        /**
         * 设置标题
         * @param text 标题字符串
         */
        fun setText(text: CharSequence) {
            view.titleView.text = text
        }

        /**
         * 设置标题
         * @param id 字符串资源ID
         */
        fun setText(@StringRes id: Int) {
            view.titleView.setText(id)
        }

        /**
         * 设置图标资源ID
         * @param id 图片资源ID
         */
        fun setImageResource(@DrawableRes id: Int) {
            view.iconView.setImageResource(id)
        }

        /**
         * 设置图标数据
         * @param drawable 绘制数据
         */
        fun setImageDrawable(drawable: Drawable?) {
            drawable?.let {
                view.iconView.setImageDrawable(it)
            }
        }

        /**
         * 给图标着色
         * @param color 颜色
         */
        fun setColorFilter(color: Int) {
            view.iconView.setColorFilter(color)
        }

    }

    class Item : RelativeLayout {

        val titleView: TextView by lazy {
            TextView(context).apply {

            }
        }

        val iconView: ImageView by lazy {
            ImageView(context).apply {
                layoutParams = LayoutParams(context.dip2px(24f), context.dip2px(24f))
            }
        }

        constructor(context: Context) : this(context, null)
        constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
        constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
            context,
            attrs,
            defStyleAttr
        ) {
            initView()
        }

        var animationDuration = 500L
        var offsetAnimationDuration = 500L

        private fun initView() {
            layoutParams =
                LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                    .apply {
                        weight = 1f
                    }

            val margin = max(iconView.height, titleView.height) + context.dip2px(20f)

            addView(
                iconView,
                LayoutParams(context.dip2px(24f), context.dip2px(24f)).apply {
                    this.addRule(CENTER_IN_PARENT)
                    this.topMargin = margin
                    this.bottomMargin = margin
                }
            )
            addView(
                titleView,
                LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    this.addRule(CENTER_IN_PARENT)
                    this.topMargin = margin
                    this.bottomMargin = margin
                }
            )

            initAnimation()
        }

        private fun initAnimation() {
            ObjectAnimator.ofFloat(iconView, "translationY", -iconView.height.toFloat()).apply {
                duration = 0
                interpolator = AccelerateInterpolator()
            }.start()
            ObjectAnimator.ofFloat(iconView, "alpha", 0f).apply {
                duration = 0
                interpolator = AccelerateInterpolator()
            }.start()
            ObjectAnimator.ofFloat(iconView, "scaleX", .5f).apply {
                duration = 0
                interpolator = AccelerateInterpolator()
            }.start()
            ObjectAnimator.ofFloat(iconView, "scaleY", .5f).apply {
                duration = 0
                interpolator = AccelerateInterpolator()
            }.start()

            ObjectAnimator.ofFloat(titleView, "translationY", titleView.height.toFloat()).apply {
                duration = 0
                interpolator = AccelerateInterpolator()
            }.start()
            ObjectAnimator.ofFloat(titleView, "alpha", 0f).apply {
                duration = 0
                interpolator = AccelerateInterpolator()
            }.start()
            ObjectAnimator.ofFloat(titleView, "scaleX", .5f).apply {
                duration = 0
                interpolator = AccelerateInterpolator()
            }.start()
            ObjectAnimator.ofFloat(titleView, "scaleY", .5f).apply {
                duration = 0
                interpolator = AccelerateInterpolator()
            }.start()
        }

        private fun iconAnimation(offset: Float) {
            //位移
            ObjectAnimator.ofFloat(iconView, "translationY", iconView.height * (offset - 1)).apply {
                duration = offsetAnimationDuration
            }.start()
            //渐变
            ObjectAnimator.ofFloat(iconView, "alpha", offset).apply {
                duration = offsetAnimationDuration
            }.start()
            //缩放
            ObjectAnimator.ofFloat(iconView, "scaleX", .5f + offset * .5f).apply {
                duration = offsetAnimationDuration
            }.start()
            ObjectAnimator.ofFloat(iconView, "scaleY", .5f + offset * .5f).apply {
                duration = offsetAnimationDuration
            }.start()
        }

        private fun iconAnimation(visible: Boolean) {
            ObjectAnimator.ofFloat(
                iconView,
                "translationY",
                if (visible) 0f else -iconView.height.toFloat()
            ).apply {
                duration = animationDuration
                interpolator = AccelerateInterpolator()
            }.start()
            ObjectAnimator.ofFloat(iconView, "alpha", if (visible) 1f else 0f).apply {
                duration = animationDuration
                interpolator = AccelerateInterpolator()
            }.start()
            ObjectAnimator.ofFloat(iconView, "scaleX", if (visible) 1f else .5f).apply {
                duration = animationDuration
                interpolator = AccelerateInterpolator()
            }.start()
            ObjectAnimator.ofFloat(iconView, "scaleY", if (visible) 1f else .5f).apply {
                duration = animationDuration
                interpolator = AccelerateInterpolator()
            }.start()
        }

        private fun titleAnimation(offset: Float) {
            ObjectAnimator.ofFloat(titleView, "translationY", offset * titleView.height).apply {
                duration = offsetAnimationDuration
            }.start()
            ObjectAnimator.ofFloat(titleView, "alpha", offset).apply {
                duration = offsetAnimationDuration
            }.start()
            ObjectAnimator.ofFloat(titleView, "scaleX", .5f + offset * .5f).apply {
                duration = offsetAnimationDuration
            }.start()
            ObjectAnimator.ofFloat(titleView, "scaleY", .5f + offset * .5f).apply {
                duration = offsetAnimationDuration
            }.start()
        }

        private fun titleAnimation(visible: Boolean) {
            ObjectAnimator.ofFloat(
                titleView,
                "translationY",
                if (visible) 0f else titleView.height.toFloat()
            ).apply {
                duration = animationDuration
                interpolator = AccelerateInterpolator()
            }.start()
            ObjectAnimator.ofFloat(titleView, "alpha", if (visible) 1f else 0f).apply {
                duration = animationDuration
                interpolator = AccelerateInterpolator()
            }.start()
            ObjectAnimator.ofFloat(titleView, "scaleX", if (visible) 1f else .5f).apply {
                duration = animationDuration
                interpolator = AccelerateInterpolator()
            }.start()
            ObjectAnimator.ofFloat(titleView, "scaleY", if (visible) 1f else .5f).apply {
                duration = animationDuration
                interpolator = AccelerateInterpolator()
            }.start()
        }

        fun selectOffset(offset: Float) {
            iconAnimation(1 - offset)
            titleAnimation(offset)
        }

        fun select(visible: Boolean) {
            iconAnimation(!visible)
            titleAnimation(visible)
        }
    }

}