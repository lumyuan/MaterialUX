package io.github.lumyuan.ux.overscroll

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.FocusFinder
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.Scroller
import io.github.lumyuan.ux.core.common.height
import kotlin.math.abs

/**
 * 弹性阻尼 ScrollView
 */
class OverScrollView : FrameLayout, OnTouchListener {

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ){
        initView()
    }

    private val ANIMATED_SCROLL_GAP = 250

    private val MAX_SCROLL_FACTOR = .5f
    private val OVERSHOOT_TENSION = 0.75f

    private var mLastScroll: Long = 0

    private val mTempRect = Rect()
    private val mScroller by lazy {
        Scroller(context, OvershootInterpolator(OVERSHOOT_TENSION))
    }

    private var prevScrollY = 0
    private var isInFlingMode = false

    private var metrics: DisplayMetrics? = null
    private var child: View? = null

    private val overScrollerSpringBackTask by lazy {
        object : Runnable {
            override fun run() {
                mScroller.computeScrollOffset()
                scrollTo(0, mScroller.currY)
                if (mScroller.isFinished) {
                    post(this)
                }
            }
        }
    }

    private var mScrollViewMovedFocus = false

    private var mLastMotionY = 0f

    private var mIsLayoutDirty = true

    private var mChildToScrollTo: View? = null

    private var mIsBeingDragged = false

    private var mVelocityTracker: VelocityTracker? = null

    private var mFillViewport = false

    private var mSmoothScrollingEnabled = true

    private var mTouchSlop = 0
    private var mMinimumVelocity = 0
    private var mMaximumVelocity = 0

    private var mActivePointerId = -1

    private val INVALID_POINTER = -1

    private val duration = 1250L
    private fun initView() {
        initScrollView()
        setFillViewport(true)
        initBounce()
    }

    private fun initBounce() {
        metrics = context.resources.displayMetrics
        prevScrollY = paddingTop
    }
    private fun initChildPointer() {
        child = getChildAt(0)
        child?.setPadding(0, context.height(), 0, context.height())
        post { scrollTo(0, context.height()) }
    }

    override fun getTopFadingEdgeStrength(): Float {
        if (childCount == 0) {
            return 0.0f
        }
        val length = verticalFadingEdgeLength
        return if (scrollY < length) {
            scrollY / length.toFloat()
        } else 1.0f
    }

    override fun getBottomFadingEdgeStrength(): Float {
        if (childCount == 0) {
            return 0.0f
        }
        val length = verticalFadingEdgeLength
        val bottomEdge = height - paddingBottom
        val span = getChildAt(0).bottom - scrollY - bottomEdge
        return if (span < length) {
            span / length.toFloat()
        } else 1.0f
    }

    private fun getMaxScrollAmount(): Int {
        return (MAX_SCROLL_FACTOR * (bottom - top)).toInt()
    }

    private fun initScrollView() {
        isFocusable = true
        descendantFocusability = FOCUS_AFTER_DESCENDANTS
        setWillNotDraw(false)
        val configuration = ViewConfiguration.get(context)
        mTouchSlop = configuration.scaledTouchSlop
        mMinimumVelocity = configuration.scaledMinimumFlingVelocity
        mMaximumVelocity = configuration.scaledMaximumFlingVelocity
        setOnTouchListener(this)
        post { scrollTo(0, context.height()) }
    }

    override fun addView(child: View?) {
        check(childCount <= 0) { "ScrollView can host only one direct child" }
        super.addView(child)
        initChildPointer()
    }

    override fun addView(child: View?, index: Int) {
        check(childCount <= 0) { "ScrollView can host only one direct child" }
        super.addView(child, index)
        initChildPointer()
    }

    override fun addView(child: View?, params: ViewGroup.LayoutParams?) {
        check(childCount <= 0) { "ScrollView can host only one direct child" }
        super.addView(child, params)
        initChildPointer()
    }

    override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
        check(childCount <= 0) { "ScrollView can host only one direct child" }
        super.addView(child, index, params)
    }

    private fun canScroll(): Boolean {
        val child = getChildAt(0)
        if (child != null) {
            val childHeight = child.height
            return height < childHeight + paddingTop + paddingBottom
        }
        return false
    }

    fun isFillViewport(): Boolean {
        return mFillViewport
    }

    fun setFillViewport(fillViewport: Boolean) {
        if (fillViewport != mFillViewport) {
            mFillViewport = fillViewport
            requestLayout()
        }
    }

    fun isSmoothScrollingEnabled(): Boolean {
        return mSmoothScrollingEnabled
    }

    fun setSmoothScrollingEnabled(smoothScrollingEnabled: Boolean) {
        mSmoothScrollingEnabled = smoothScrollingEnabled
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (!mFillViewport) {
            return
        }
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        if (heightMode == MeasureSpec.UNSPECIFIED) {
            return
        }
        if (childCount > 0) {
            val child = getChildAt(0)
            var height = measuredHeight
            if (child.measuredHeight < height) {
                val lp = child.layoutParams as LayoutParams
                val childWidthMeasureSpec =
                    getChildMeasureSpec(widthMeasureSpec, paddingLeft + paddingRight, lp.width)
                height -= paddingTop
                height -= paddingBottom
                val childHeightMeasureSpec =
                    MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
                child.measure(childWidthMeasureSpec, childHeightMeasureSpec)
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        return super.dispatchKeyEvent(event) || executeKeyEvent(event)
    }

    private fun executeKeyEvent(event: KeyEvent): Boolean {
        mTempRect.setEmpty()
        if (!canScroll()) {
            if (isFocused && event.keyCode != KeyEvent.KEYCODE_BACK) {
                var currentFocused = findFocus()
                if (currentFocused === this) currentFocused = null
                val nextFocused =
                    FocusFinder.getInstance().findNextFocus(this, currentFocused, FOCUS_DOWN)
                return nextFocused != null && nextFocused !== this && nextFocused.requestFocus(
                    FOCUS_DOWN
                )
            }
            return false
        }
        var handled = false
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> handled = if (!event.isAltPressed) {
                    arrowScroll(FOCUS_UP)
                } else {
                    fullScroll(FOCUS_UP)
                }

                KeyEvent.KEYCODE_DPAD_DOWN -> handled = if (!event.isAltPressed) {
                    arrowScroll(FOCUS_DOWN)
                } else {
                    fullScroll(FOCUS_DOWN)
                }

                KeyEvent.KEYCODE_SPACE -> pageScroll(if (event.isShiftPressed) FOCUS_UP else FOCUS_DOWN)
            }
        }
        return handled
    }

    private fun inChild(x: Int, y: Int): Boolean {
        if (childCount > 0) {
            val scrollY = scrollY
            val child = getChildAt(0)
            return !(y < child.top - scrollY || y >= child.bottom - scrollY || x < child.left || x >= child.right)
        }
        return false
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        val action = ev.action
        if (action == MotionEvent.ACTION_MOVE && mIsBeingDragged) {
            return true
        }
        when (action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_MOVE -> {
                val activePointerId = mActivePointerId
                if (activePointerId != INVALID_POINTER) {
                    val pointerIndex = ev.findPointerIndex(activePointerId)
                    val y = ev.getY(pointerIndex)
                    val yDiff = abs(y - mLastMotionY).toInt()
                    if (yDiff > mTouchSlop) {
                        mIsBeingDragged = true
                        mLastMotionY = y
                    }
                }
            }

            MotionEvent.ACTION_DOWN -> {
                val y = ev.y
                if (!inChild(ev.x.toInt(), y.toInt())) {
                    mIsBeingDragged = false
                }else {
                    mLastMotionY = y
                    mActivePointerId = ev.getPointerId(0)

                    mIsBeingDragged = !mScroller.isFinished
                }
            }

            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                mIsBeingDragged = false
                mActivePointerId = INVALID_POINTER
            }

            MotionEvent.ACTION_POINTER_UP -> onSecondaryPointerUp(ev)
        }

        return mIsBeingDragged
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN && ev.edgeFlags != 0) {
            return false
        }
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain()
        }
        mVelocityTracker?.addMovement(ev)
        val action = ev.action
        when (action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                val y = ev.y
                if (!inChild(ev.x.toInt(), y.toInt()).also { mIsBeingDragged = it }) {
                    return false
                }
                if (!mScroller.isFinished) {
                    mScroller.abortAnimation()
                }
                mLastMotionY = y
                mActivePointerId = ev.getPointerId(0)
            }

            MotionEvent.ACTION_MOVE -> if (mIsBeingDragged) {
                val activePointerIndex = ev.findPointerIndex(mActivePointerId)
                val y = ev.getY(activePointerIndex)
                val deltaY = (mLastMotionY - y).toInt()
                mLastMotionY = y
                if (isOverScrolled()) {
                    scrollBy(0, deltaY / 2)
                } else {
                    scrollBy(0, deltaY)
                }
            }

            MotionEvent.ACTION_UP -> if (mIsBeingDragged) {
                val velocityTracker = mVelocityTracker!!
                velocityTracker.computeCurrentVelocity(duration.toInt(), mMaximumVelocity.toFloat())
                val initialVelocity = velocityTracker.getYVelocity(mActivePointerId).toInt()
                if (childCount > 0 && abs(initialVelocity) > mMinimumVelocity) {
                    fling(-initialVelocity)
                }
                mActivePointerId = INVALID_POINTER
                mIsBeingDragged = false
                if (mVelocityTracker != null) {
                    mVelocityTracker?.recycle()
                    mVelocityTracker = null
                }
            }

            MotionEvent.ACTION_CANCEL -> if (mIsBeingDragged && childCount > 0) {
                mActivePointerId = INVALID_POINTER
                mIsBeingDragged = false
                if (mVelocityTracker != null) {
                    mVelocityTracker?.recycle()
                    mVelocityTracker = null
                }
            }

            MotionEvent.ACTION_POINTER_UP -> onSecondaryPointerUp(ev)
        }
        return true
    }

    private fun isOverScrolled(): Boolean {
        return scrollY < child!!.paddingTop || scrollY > child!!.bottom - child!!.paddingBottom - height
    }

    private fun onSecondaryPointerUp(ev: MotionEvent) {
        val pointerIndex =
            ev.action and MotionEvent.ACTION_POINTER_INDEX_MASK shr MotionEvent.ACTION_POINTER_INDEX_SHIFT
        val pointerId = ev.getPointerId(pointerIndex)
        if (pointerId == mActivePointerId) {
            val newPointerIndex = if (pointerIndex == 0) 1 else 0
            mLastMotionY = ev.getY(newPointerIndex)
            mActivePointerId = ev.getPointerId(newPointerIndex)
            if (mVelocityTracker != null) {
                mVelocityTracker?.clear()
            }
        }
    }

    private fun findFocusableViewInMyBounds(
        topFocus: Boolean,
        top: Int,
        preferredFocusable: View?
    ): View? {
        val fadingEdgeLength = verticalFadingEdgeLength / 2
        val topWithoutFadingEdge = top + fadingEdgeLength
        val bottomWithoutFadingEdge = top + height - fadingEdgeLength
        return if (preferredFocusable != null && preferredFocusable.top < bottomWithoutFadingEdge && preferredFocusable.bottom > topWithoutFadingEdge
        ) {
            preferredFocusable
        } else findFocusableViewInBounds(topFocus, topWithoutFadingEdge, bottomWithoutFadingEdge)
    }

    private fun findFocusableViewInBounds(topFocus: Boolean, top: Int, bottom: Int): View? {
        val focusables: List<View> = getFocusables(FOCUS_FORWARD)
        var focusCandidate: View? = null
        var foundFullyContainedFocusable = false
        val count = focusables.size
        for (i in 0 until count) {
            val view = focusables[i]
            val viewTop = view.top
            val viewBottom = view.bottom
            if (top < viewBottom && viewTop < bottom) {
                val viewIsFullyContained = top < viewTop && viewBottom < bottom
                if (focusCandidate == null) {
                    focusCandidate = view
                    foundFullyContainedFocusable = viewIsFullyContained
                } else {
                    val viewIsCloserToBoundary =
                        topFocus && viewTop < focusCandidate.top || !topFocus && viewBottom > focusCandidate.bottom
                    if (foundFullyContainedFocusable) {
                        if (viewIsFullyContained && viewIsCloserToBoundary) {
                            focusCandidate = view
                        }
                    } else {
                        if (viewIsFullyContained) {
                            focusCandidate = view
                            foundFullyContainedFocusable = true
                        } else if (viewIsCloserToBoundary) {
                            focusCandidate = view
                        }
                    }
                }
            }
        }
        return focusCandidate
    }

    private fun pageScroll(direction: Int): Boolean {
        val down = direction == FOCUS_DOWN
        val height = height
        if (down) {
            mTempRect.top = scrollY + height
            val count = childCount
            if (count > 0) {
                val view = getChildAt(count - 1)
                if (mTempRect.top + height > view.bottom) {
                    mTempRect.top = view.bottom - height
                }
            }
        } else {
            mTempRect.top = scrollY - height
            if (mTempRect.top < 0) {
                mTempRect.top = 0
            }
        }
        mTempRect.bottom = mTempRect.top + height
        return scrollAndFocus(direction, mTempRect.top, mTempRect.bottom)
    }

    private fun fullScroll(direction: Int): Boolean {
        val down = direction == FOCUS_DOWN
        val height = height
        mTempRect.top = 0
        mTempRect.bottom = height
        if (down) {
            val count = childCount
            if (count > 0) {
                val view = getChildAt(count - 1)
                mTempRect.bottom = view.bottom
                mTempRect.top = mTempRect.bottom - height
            }
        }
        return scrollAndFocus(direction, mTempRect.top, mTempRect.bottom)
    }

    private fun scrollAndFocus(direction: Int, top: Int, bottom: Int): Boolean {
        var handled = true
        val height = height
        val containerTop = scrollY
        val containerBottom = containerTop + height
        val up = direction == FOCUS_UP
        var newFocused = findFocusableViewInBounds(up, top, bottom)
        if (newFocused == null) {
            newFocused = this
        }
        if (top >= containerTop && bottom <= containerBottom) {
            handled = false
        } else {
            val delta = if (up) top - containerTop else bottom - containerBottom
            doScrollY(delta)
        }
        if (newFocused !== findFocus() && newFocused.requestFocus(direction)) {
            mScrollViewMovedFocus = true
            mScrollViewMovedFocus = false
        }
        return handled
    }

    private fun arrowScroll(direction: Int): Boolean {
        var currentFocused = findFocus()
        if (currentFocused === this) currentFocused = null
        val nextFocused = FocusFinder.getInstance().findNextFocus(this, currentFocused, direction)
        val maxJump = getMaxScrollAmount()
        if (nextFocused != null && isWithinDeltaOfScreen(nextFocused, maxJump, height)) {
            nextFocused.getDrawingRect(mTempRect)
            offsetDescendantRectToMyCoords(nextFocused, mTempRect)
            val scrollDelta = computeScrollDeltaToGetChildRectOnScreen(mTempRect)
            doScrollY(scrollDelta)
            nextFocused.requestFocus(direction)
        } else {
            var scrollDelta = maxJump
            if (direction == FOCUS_UP && scrollY < scrollDelta) {
                scrollDelta = scrollY
            } else if (direction == FOCUS_DOWN) {
                if (childCount > 0) {
                    val daBottom = getChildAt(0).bottom
                    val screenBottom = scrollY + height
                    if (daBottom - screenBottom < maxJump) {
                        scrollDelta = daBottom - screenBottom
                    }
                }
            }
            if (scrollDelta == 0) {
                return false
            }
            doScrollY(if (direction == FOCUS_DOWN) scrollDelta else -scrollDelta)
        }
        if (currentFocused != null && currentFocused.isFocused && isOffScreen(currentFocused)) {
            val descendantFocusability = descendantFocusability
            setDescendantFocusability(FOCUS_BEFORE_DESCENDANTS)
            requestFocus()
            setDescendantFocusability(descendantFocusability)
        }
        return true
    }

    private fun isOffScreen(descendant: View): Boolean {
        return !isWithinDeltaOfScreen(descendant, 0, height)
    }

    private fun isWithinDeltaOfScreen(descendant: View, delta: Int, height: Int): Boolean {
        descendant.getDrawingRect(mTempRect)
        offsetDescendantRectToMyCoords(descendant, mTempRect)
        return mTempRect.bottom + delta >= scrollY && mTempRect.top - delta <= scrollY + height
    }

    private fun doScrollY(delta: Int) {
        if (delta != 0) {
            if (mSmoothScrollingEnabled) {
                smoothScrollBy(0, delta)
            } else {
                scrollBy(0, delta)
            }
        }
    }

    private fun smoothScrollBy(dx: Int, mDy: Int) {
        var dy = mDy
        if (childCount == 0) {
            return
        }
        val duration: Long = AnimationUtils.currentAnimationTimeMillis() - mLastScroll
        if (duration > ANIMATED_SCROLL_GAP) {
            val height = height - paddingBottom - paddingTop
            val bottom = getChildAt(0).height
            val maxY = 0.coerceAtLeast(bottom - height)
            val scrollY = scrollY
            dy = 0.coerceAtLeast((scrollY + dy).coerceAtMost(maxY)) - scrollY
            mScroller.startScroll(scrollX, scrollY, 0, dy)
            invalidate()
        } else {
            if (!mScroller.isFinished) {
                mScroller.abortAnimation()
            }
            scrollBy(dx, dy)
        }
        mLastScroll = AnimationUtils.currentAnimationTimeMillis()
    }

    fun smoothScrollToTop() {
        smoothScrollTo(0, child!!.paddingTop)
    }

    fun smoothScrollToBottom() {
        smoothScrollTo(0, child!!.height - child!!.paddingTop - height)
    }

    private fun smoothScrollTo(x: Int, y: Int) {
        smoothScrollBy(x - scrollX, y - scrollY)
    }

    override fun computeVerticalScrollRange(): Int {
        val count = childCount
        val contentHeight = height - paddingBottom - paddingTop
        return if (count == 0) {
            contentHeight
        } else getChildAt(0).bottom
    }

    override fun computeVerticalScrollOffset(): Int {
        return 0.coerceAtLeast(super.computeVerticalScrollOffset())
    }

    override fun measureChild(
        child: View,
        parentWidthMeasureSpec: Int,
        parentHeightMeasureSpec: Int
    ) {
        val lp = child.layoutParams
        val childWidthMeasureSpec: Int =
            getChildMeasureSpec(parentWidthMeasureSpec, paddingLeft + paddingRight, lp.width)
        val childHeightMeasureSpec: Int = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        child.measure(childWidthMeasureSpec, childHeightMeasureSpec)
    }

    override fun measureChildWithMargins(
        child: View, parentWidthMeasureSpec: Int, widthUsed: Int, parentHeightMeasureSpec: Int,
        heightUsed: Int
    ) {
        val lp = child.layoutParams as MarginLayoutParams
        val childWidthMeasureSpec = getChildMeasureSpec(
            parentWidthMeasureSpec, paddingLeft + paddingRight + lp.leftMargin
                    + lp.rightMargin + widthUsed, lp.width
        )
        val childHeightMeasureSpec =
            MeasureSpec.makeMeasureSpec(lp.topMargin + lp.bottomMargin, MeasureSpec.UNSPECIFIED)
        child.measure(childWidthMeasureSpec, childHeightMeasureSpec)
    }

    override fun computeScroll() {

        if (mScroller.computeScrollOffset()) {
            val oldX = scrollX
            val oldY = scrollY
            var x = mScroller.currX
            var y = mScroller.currY
            if (childCount > 0) {
                val child = getChildAt(0)
                x = clamp(x, width - paddingRight - paddingLeft, child.width)
                y = clamp(y, height - paddingBottom - paddingTop, child.height)
                if (x != oldX || y != oldY) {
                    scrollX = x
                    scrollY = y
                    onScrollChanged(x, y, oldX, oldY)
                }
            }
            awakenScrollBars()
            postInvalidate()
        }
    }

    private fun scrollToChild(child: View?) {
        child?.getDrawingRect(mTempRect)

        offsetDescendantRectToMyCoords(
            child,
            mTempRect
        )
        val scrollDelta = computeScrollDeltaToGetChildRectOnScreen(mTempRect)
        if (scrollDelta != 0) {
            scrollBy(0, scrollDelta)
        }
    }

    private fun scrollToChildRect(rect: Rect, immediate: Boolean): Boolean {
        val delta = computeScrollDeltaToGetChildRectOnScreen(rect)
        val scroll = delta != 0
        if (scroll) {
            if (immediate) {
                scrollBy(0, delta)
            } else {
                smoothScrollBy(0, delta)
            }
        }
        return scroll
    }

    private fun computeScrollDeltaToGetChildRectOnScreen(rect: Rect): Int {
        if (childCount == 0) return 0
        val height = height
        var screenTop = scrollY
        var screenBottom = screenTop + height
        val fadingEdge = verticalFadingEdgeLength

        if (rect.top > 0) {
            screenTop += fadingEdge
        }

        if (rect.bottom < getChildAt(0).height) {
            screenBottom -= fadingEdge
        }
        var scrollYDelta = 0
        if (rect.bottom > screenBottom && rect.top > screenTop) {
            scrollYDelta += if (rect.height() > height) {
                rect.top - screenTop
            } else {
                rect.bottom - screenBottom
            }

            val bottom = getChildAt(0).bottom
            val distanceToBottom = bottom - screenBottom
            scrollYDelta = scrollYDelta.coerceAtMost(distanceToBottom)
        } else if (rect.top < screenTop && rect.bottom < screenBottom) {
            scrollYDelta -= if (rect.height() > height) {
                screenBottom - rect.bottom
            } else {
                screenTop - rect.top
            }

            scrollYDelta = scrollYDelta.coerceAtLeast(-scrollY)
        }
        return scrollYDelta
    }

    override fun requestChildFocus(child: View?, focused: View) {
        if (!mScrollViewMovedFocus) {
            if (!mIsLayoutDirty) {
                scrollToChild(focused)
            } else {
                mChildToScrollTo = focused
            }
        }
        super.requestChildFocus(child, focused)
    }

    override fun onRequestFocusInDescendants(
        mDirection: Int,
        previouslyFocusedRect: Rect?
    ): Boolean {

        var direction = mDirection
        if (direction == FOCUS_FORWARD) {
            direction = FOCUS_DOWN
        } else if (direction == FOCUS_BACKWARD) {
            direction = FOCUS_UP
        }
        val nextFocus = if (previouslyFocusedRect == null) FocusFinder.getInstance()
            .findNextFocus(this, null, direction) else FocusFinder
            .getInstance().findNextFocusFromRect(this, previouslyFocusedRect, direction)
        if (nextFocus == null) {
            return false
        }
        return if (isOffScreen(nextFocus)) {
            false
        } else nextFocus.requestFocus(direction, previouslyFocusedRect)
    }

    override fun requestChildRectangleOnScreen(
        child: View,
        rectangle: Rect,
        immediate: Boolean
    ): Boolean {
        rectangle.offset(child.left - child.scrollX, child.top - child.scrollY)
        return scrollToChildRect(rectangle, immediate)
    }

    override fun requestLayout() {
        mIsLayoutDirty = true
        super.requestLayout()
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        mIsLayoutDirty = false
        if (mChildToScrollTo != null && isViewDescendantOf(mChildToScrollTo, this)) {
            scrollToChild(mChildToScrollTo)
        }
        mChildToScrollTo = null

        scrollTo(scrollX, scrollY)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val currentFocused = findFocus()
        if (null == currentFocused || this === currentFocused) return

        if (isWithinDeltaOfScreen(currentFocused, 0, oldh)) {
            currentFocused.getDrawingRect(mTempRect)
            offsetDescendantRectToMyCoords(currentFocused, mTempRect)
            val scrollDelta = computeScrollDeltaToGetChildRectOnScreen(mTempRect)
            doScrollY(scrollDelta)
        }
    }

    override fun onScrollChanged(
        leftOfVisibleView: Int,
        topOfVisibleView: Int,
        oldLeftOfVisibleView: Int,
        oldTopOfVisibleView: Int
    ) {
        val displayHeight = height
        val paddingTop = child!!.paddingTop
        val contentBottom = child!!.height - child!!.paddingBottom
        if (isInFlingMode) {
            if (topOfVisibleView < paddingTop || topOfVisibleView > contentBottom - displayHeight) {
                if (topOfVisibleView < paddingTop) {
                    mScroller.startScroll(
                        0,
                        topOfVisibleView,
                        0,
                        paddingTop - topOfVisibleView,
                        duration.toInt()
                    )
                } else if (topOfVisibleView > contentBottom - displayHeight) {
                    mScroller.startScroll(
                        0,
                        topOfVisibleView,
                        0,
                        contentBottom - displayHeight - topOfVisibleView,
                        duration.toInt()
                    )
                }

                post(overScrollerSpringBackTask)
                isInFlingMode = false
                return
            }
        }
        super.onScrollChanged(
            leftOfVisibleView,
            topOfVisibleView,
            oldLeftOfVisibleView,
            oldTopOfVisibleView
        )
    }

    private fun isViewDescendantOf(child: View?, parent: View?): Boolean {
        if (child === parent) {
            return true
        }
        val theParent = child?.parent
        return theParent is ViewGroup && isViewDescendantOf(theParent as View, parent)
    }

    private fun fling(velocityY: Int) {
        if (childCount > 0) {
            val height = height - paddingBottom - paddingTop
            val bottom = getChildAt(0).height
            mScroller.fling(scrollX, scrollY, 0, velocityY, 0, 0, 0,
                0.coerceAtLeast(bottom - height)
            )
            val movingDown = velocityY > 0
            var newFocused =
                findFocusableViewInMyBounds(movingDown, mScroller.finalY, findFocus())
            if (newFocused == null) {
                newFocused = this
            }
            if (newFocused != findFocus() && newFocused.requestFocus(if (movingDown) FOCUS_DOWN else FOCUS_UP)) {
                mScrollViewMovedFocus = true
                mScrollViewMovedFocus = false
            }
            invalidate()
        }
    }

    override fun scrollTo(tx: Int, ty: Int) {
        var x = tx
        var y = ty
        if (childCount > 0) {
            val child = getChildAt(0)
            x = clamp(x, width - paddingRight - paddingLeft, child.width)
            y = clamp(y, height - paddingBottom - paddingTop, child.height)
            if (x != scrollX || y != scrollY) {
                super.scrollTo(x, y)
            }
        }
    }

    private fun clamp(n: Int, my: Int, child: Int): Int {
        if (my >= child || n < 0) {
            return 0
        }
        return if (my + n > child) {
            child - my
        } else n
    }

    override fun onTouch(v: View?, event: MotionEvent): Boolean {
        mScroller.forceFinished(true)
        removeCallbacks(overScrollerSpringBackTask)
        if (event.action == MotionEvent.ACTION_UP) {
            return overScrollView()
        } else if (event.action == MotionEvent.ACTION_CANCEL) {
            return overScrollView()
        }
        return false
    }

    private fun overScrollView(): Boolean {
        val displayHeight = height
        val contentTop = child?.paddingTop ?: 0
        val contentBottom = child!!.height - child!!.paddingBottom
        val currScrollY = scrollY
        var scrollBy: Int

        if (currScrollY < contentTop) {
            onOverScroll(currScrollY)
            scrollBy = contentTop - currScrollY
        } else if (currScrollY + displayHeight > contentBottom) {
            scrollBy =
                if ((child?.height ?: 0) - (child?.paddingTop ?: 0) - (child?.paddingBottom ?: 0) < displayHeight) {
                    contentTop - currScrollY
                } else {
                    contentBottom - displayHeight - currScrollY
                }
            scrollBy += onOverScroll(currScrollY)
        } else {
            isInFlingMode = true
            return false
        }
        mScroller.startScroll(0, currScrollY, 0, scrollBy, 500)

        post(overScrollerSpringBackTask)
        prevScrollY = currScrollY
        return true
    }

    private fun onOverScroll(scrollY: Int): Int {
        return 0
    }
}