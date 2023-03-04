package io.github.lumyuan.ux.topbar.widget

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import io.github.lumyuan.ux.core.LiveData
import io.github.lumyuan.ux.core.common.dip2px
import io.github.lumyuan.ux.topbar.R
import kotlin.properties.Delegates

@SuppressLint("ResourceType")
class TopBar : FrameLayout {

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initView(attrs)
    }

    private val topView by lazy {
        View.inflate(context, R.layout.top_bar, null)
    }

    private lateinit var titleView: TextView
    private lateinit var subtitleView: TextView
    private lateinit var firstMenu: ImageView
    private lateinit var secondsMenu: ImageView
    private lateinit var menuLayout: LinearLayout

    private var titleColor by Delegates.notNull<Int>()
    private var subtitleColor by Delegates.notNull<Int>()
    private var menuColor by Delegates.notNull<Int>()

    private val itemsPool by lazy {
        ArrayList<Item>()
    }

    private var oldOffset = 0f
    private val pageChangeListener = object : OnPageChangeListener {
        override fun onPageScrolled(
            position: Int,
            positionOffset: Float,
            positionOffsetPixels: Int
        ) {
            if (positionOffset == 0f){
                titleView.apply {
                    translationX = 0f
                    alpha = 1f
                }

                subtitleView.apply {
                    translationX = 0f
                    alpha = 1f
                }

                firstMenu.apply {
                    translationX = 0f
                    alpha = 1f
                }

                secondsMenu.apply {
                    translationX = 0f
                    alpha = 1f
                }
            }else if (positionOffset < .5f){
                setCurrentItem(position)

                val set = oldOffset * 2f
                val tOffset = translationOffset * set
                val aOffset = 1 - set

                titleView.apply {
                    translationX = tOffset
                    alpha = aOffset
                }

                subtitleView.apply {
                    val t = (translationOffset + context.dip2px(5f)) * set
                    translationX = t
                    alpha = aOffset
                }

                firstMenu.apply {
                    translationX = tOffset
                    alpha = aOffset
                }

                secondsMenu.apply {
                    translationX = tOffset
                    alpha = aOffset
                }

            }else {
                setCurrentItem(position + 1)
                val set = (positionOffset - .5f) * 2f
                val tOffset = (translationOffset * set) - translationOffset

                titleView.apply {
                    translationX = tOffset
                    alpha = set
                }

                subtitleView.apply {
                    val t = (translationOffset + context.dip2px(5f)) * set - (translationOffset + context.dip2px(5f))
                    translationX = t
                    alpha = set
                }

                firstMenu.apply {
                    translationX = tOffset
                    alpha = set
                }

                secondsMenu.apply {
                    translationX = tOffset
                    alpha = set
                }
            }
            oldOffset = positionOffset
        }

        override fun onPageSelected(position: Int) {
        }

        override fun onPageScrollStateChanged(state: Int) {
        }

    }

    private val positionLiveData = LiveData(0)

    @SuppressLint("Recycle", "ResourceType")
    private fun initView(attrs: AttributeSet?) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.TopBar)
        titleColor = typedArray.getColor(
            R.styleable.TopBar_titleColor,
            Color.parseColor(context.getString(R.color.titleColor))
        )
        subtitleColor = typedArray.getColor(
            R.styleable.TopBar_subtitleColor,
            Color.parseColor(context.getString(R.color.subtitleColor))
        )
        menuColor = typedArray.getColor(
            R.styleable.TopBar_menuColorFilter,
            Color.TRANSPARENT
        )

        addView(
            topView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        )

        titleView = topView.findViewById(R.id.titleView)
        subtitleView = topView.findViewById(R.id.subtitleView)
        firstMenu = topView.findViewById(R.id.firstMenu)
        secondsMenu = topView.findViewById(R.id.secondsMenu)
        menuLayout = topView.findViewById(R.id.menuLayout)

        titleView.setTextColor(titleColor)
        subtitleView.setTextColor(subtitleColor)
        firstMenu.setColorFilter(menuColor)
        secondsMenu.setColorFilter(menuColor)

        setBackgroundColor(Color.parseColor(context.getString(R.color.backgroundColor)))

    }

    fun setupViewpager(viewpager: ViewPager) {
        viewpager.addOnPageChangeListener(this.pageChangeListener)
    }

    fun setupData(items: ArrayList<Item>){
        this.itemsPool.clear()
        this.itemsPool.addAll(items)
        setCurrentItem(0)
    }

    fun setCurrentItem(position: Int){
        this.positionLiveData.value = position
        val item = itemsPool[position]
        titleView.visibility =  if (TextUtils.isEmpty(item.titleText)){
            GONE
        }else {
            VISIBLE
        }
        titleView.text = item.titleText

        subtitleView.apply {
            visibility = if (TextUtils.isEmpty(item.subtitleText)){
                GONE
            }else {
                VISIBLE
            }
        }
        subtitleView.text = item.subtitleText

        firstMenu.visibility = if (item.firstMenuIconResource == null){
            GONE
        }else {
            firstMenu.setImageResource(item.firstMenuIconResource)
            VISIBLE
        }

        secondsMenu.visibility = if (item.secondsMenuIconResource == null){
            GONE
        }else {
            secondsMenu.setImageResource(item.secondsMenuIconResource)
            VISIBLE
        }
    }

    private var firstMenuClickListener: (view: View, position: Int) -> Unit = {_, _ ->}
    private var secondsMenuClickListener: (view: View, position: Int) -> Unit = {_, _ ->}
    fun setFirstMenuOnClickListener(firstMenuClickListener: (view: View, position: Int) -> Unit){
        this.firstMenuClickListener = firstMenuClickListener
    }

    fun setSecondsMenuOnClickListener(secondsMenuClickListener: (view: View, position: Int) -> Unit){
        this.secondsMenuClickListener = secondsMenuClickListener
    }

    private val translationOffset by lazy {
        context.dip2px(8f)
    }
    private fun setOffsetAnimation(view: View, offset: Float){
        ObjectAnimator.ofFloat(view, "translationX", offset)
    }

    data class Item(
        var titleText: String?,
        var subtitleText: String?,
        @DrawableRes val firstMenuIconResource: Int?,
        @DrawableRes val secondsMenuIconResource: Int?
    )

}