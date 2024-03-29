package io.github.lumyuan.ux.bottomnavigationview.widget

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
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import io.github.lumyuan.ux.core.LiveData
import io.github.lumyuan.ux.bottomnavigationview.R
import io.github.lumyuan.ux.core.common.*
import kotlin.math.max
import kotlin.properties.Delegates

class BottomNavigationView : FrameLayout {
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
                if (isClick) {
                    items.indices.onEach { index ->
                        val view = items[index].view
                        view.select(it == index)
                    }
                    runIndicatorAnimation(it)
                }else {
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

    private var isClick = true
    private fun initItem() {
        itemLayout.removeAllViews()
        items.indices.onEach {
            itemLayout.addView(
                items[it].view.apply {
                    this.setOnClickListener { v ->
                        isClick = true
                        setCurrentItem(it)
                        viewPager?.currentItem = it
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
                duration = this@BottomNavigationView.duration
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
                duration = this@BottomNavigationView.duration
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
        this.view.titleView.textSize = this@BottomNavigationView.textSize
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

    private var oldOffset = 0f
    private var newPosition = 0
    private var state = 0
    private var next: Int? = null
    fun setupViewpager(viewpager: ViewPager) {
        this.viewPager = viewpager
        viewpager.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {

//                println("position: $position\t\tpositionOffset: $positionOffset\t\tpositionOffsetPixels: $positionOffsetPixels")

                if (!isClick){
                    if (positionOffset == 0f) {

                        val tl = centerOffset + (itemWidth * position)
                        val tr = tl + indicatorWith
                        iLeft = tl
                        iRight = tr

                        items.indices.onEach { index ->
                            val view = items[index].view
                            if (position == index){
                                view.titleAnimation(1f)
                                view.iconAnimation(0f)
                            }else {
                                view.titleAnimation(0f)
                                view.iconAnimation(1f)
                            }
                        }

                        positionData.value = position

                        next = null
                    } else {
                        if (next == null) {
                            next = if (oldOffset < positionOffset){
                                val n = position + 1
                                if (n > items.size - 1){
                                    items.size - 1
                                }else {
                                    n
                                }
                            }else {
                                val n = position - 1
                                if (n < 0){
                                    0
                                }else {
                                    n
                                }
                            }
                        }

                        val oldView = items[position].view
                        val nextView = items[next ?: 0].view

                        if (position < (next?.toFloat() ?: 0f)) {

                            oldView.titleAnimation(1 - positionOffset)
                            oldView.iconAnimation(positionOffset)

                            nextView.titleAnimation(positionOffset)
                            nextView.iconAnimation(1 - positionOffset)

                            val tl = centerOffset + (itemWidth * position) + (itemWidth * positionOffset)
                            val tr = tl + indicatorWith
                            iLeft = tl
                            iRight = tr
                        }else {

                            oldView.titleAnimation(positionOffset)
                            oldView.iconAnimation(1 - positionOffset)

                            nextView.titleAnimation(1 - positionOffset)
                            nextView.iconAnimation(positionOffset)

                            val tl = centerOffset + itemWidth * position
                            val tr = tl + indicatorWith
                            iLeft = tl
                            iRight = tr
                        }
                    }
                    oldOffset = positionOffset
                    invalidate()
                }
            }

            override fun onPageSelected(position: Int) {

            }

            override fun onPageScrollStateChanged(state: Int) {
                this@BottomNavigationView.state = state
                if (state == 1){
                    isClick = false
                }
            }

        })
    }

    fun getCurrentItem() = viewPager?.currentItem ?: positionData.value ?: 0

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
        var offsetAnimationDuration = 5L

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

        fun iconAnimation(offset: Float) {
            iconView.apply {
                translationY = -height * (1 - offset)
                alpha = offset
                val scale = .5f + offset * .5f
                scaleX = scale
                scaleY = scale
            }
        }

        fun iconAnimation(visible: Boolean) {
            ObjectAnimator.ofFloat(
                iconView,
                "translationY",
                iconView.translationY, if (visible) 0f else -iconView.height.toFloat()
            ).apply {
                duration = animationDuration
                interpolator = AccelerateInterpolator()
            }.start()
            ObjectAnimator.ofFloat(iconView, "alpha", iconView.alpha, if (visible) 1f else 0f).apply {
                duration = animationDuration
                interpolator = AccelerateInterpolator()
            }.start()
            ObjectAnimator.ofFloat(iconView, "scaleX", iconView.scaleX, if (visible) 1f else .5f).apply {
                duration = animationDuration
                interpolator = AccelerateInterpolator()
            }.start()
            ObjectAnimator.ofFloat(iconView, "scaleY", iconView.scaleY, if (visible) 1f else .5f).apply {
                duration = animationDuration
                interpolator = AccelerateInterpolator()
            }.start()
        }

        fun titleAnimation(offset: Float) {
            titleView.apply {
                translationY = (1 - offset) * titleView.height
                alpha = offset
                val scale = .5f + (offset) * .5f
                scaleX = scale
                scaleY = scale
            }
        }

        fun titleAnimation(visible: Boolean) {
            ObjectAnimator.ofFloat(
                titleView,
                "translationY",
                titleView.translationY,
                if (visible) 0f else titleView.height.toFloat()
            ).apply {
                duration = animationDuration
                interpolator = AccelerateInterpolator()
            }.start()
            ObjectAnimator.ofFloat(titleView, "alpha", titleView.alpha,  if (visible) 1f else 0f).apply {
                duration = animationDuration
                interpolator = AccelerateInterpolator()
            }.start()
            ObjectAnimator.ofFloat(titleView, "scaleX", titleView.scaleX, if (visible) 1f else .5f).apply {
                duration = animationDuration
                interpolator = AccelerateInterpolator()
            }.start()
            ObjectAnimator.ofFloat(titleView, "scaleY", titleView.scaleY, if (visible) 1f else .5f).apply {
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