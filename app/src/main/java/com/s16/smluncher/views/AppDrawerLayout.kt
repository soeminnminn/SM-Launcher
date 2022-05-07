package com.s16.smluncher.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import androidx.annotation.IdRes
import androidx.annotation.RequiresApi
import androidx.annotation.StyleableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.s16.smluncher.R
import kotlin.math.max
import kotlin.math.min

class AppDrawerLayout : FrameLayout,
    ViewTreeObserver.OnGlobalLayoutListener, ViewTreeObserver.OnDrawListener {

    private var cornerRadius: Float = 0f
    private val ATTRS = intArrayOf(android.R.attr.backgroundTint, com.google.android.material.R.attr.cornerRadius)
    private var backgroundTint: ColorStateList? = null
    private val backgroundDrawable: MaterialShapeDrawable
    private val drawableRect = Rect()

    private val handleArrow = HandleArrowDrawable()

    private var slideOffset: Float = 0f

    private var insetTop: Int = 0
    private var heightPixels : Int = 0
    private var fullHeight: Int = 0
    private var handleHeight: Int = 0

    private var container: ViewGroup
    private var handleId: Int = 0
    private var mHandle : View? = null

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    @SuppressLint("ResourceType")
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr) {

        setWillNotDraw(false)

        val metrics = resources.displayMetrics
        val dpMultiplier = metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT
        val defCornerRadius = CORNER_RADIUS * dpMultiplier

        val a = context.obtainStyledAttributes(attrs, ATTRS)
        backgroundTint = getColorStateList(context, a, 0)
        cornerRadius = a.getDimensionPixelSize(1, defCornerRadius).toFloat()
        a.recycle()

        backgroundDrawable = MaterialShapeDrawable(
            ShapeAppearanceModel().toBuilder().apply {
                setTopLeftCorner(CornerFamily.ROUNDED, cornerRadius)
                setTopRightCorner(CornerFamily.ROUNDED, cornerRadius)
            }.build()
        ).apply {
            fillColor = backgroundTint
        }

        heightPixels = context.resources.displayMetrics.heightPixels

        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            val systemBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            insetTop = systemBarInsets.top
            insets
        }

        viewTreeObserver.addOnGlobalLayoutListener(this)
        viewTreeObserver.addOnDrawListener(this)

        container = FrameLayout(context).apply {
            layoutParams = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            // setBackgroundColor(ContextCompat.getColor(context, R.color.teal_200))
        }
        super.addView(container)
    }

    override fun onDraw(canvas: Canvas?) {
        canvas?.let { c ->
            backgroundDrawable.bounds = drawableRect
            backgroundDrawable.draw(c)
        }
        super.onDraw(canvas)
    }

    override fun onDrawForeground(canvas: Canvas?) {
        super.onDrawForeground(canvas)

        canvas?.let { c ->
            handleArrow.draw(c)
        }
    }

    override fun onDraw() {
        val collapsedOffset = fullHeight - handleHeight
        val offset = if (top > collapsedOffset)
                collapsedOffset.toFloat() / (fullHeight - collapsedOffset)
            else
                (collapsedOffset - top).toFloat() / collapsedOffset

        if (slideOffset != offset) {
            slideOffset = offset
            onSlide(offset)
        }
    }

    override fun onGlobalLayout() {
        if (top > 0) {
            handleHeight = fullHeight - top
            handleArrow.setTopOffset((handleHeight * 0.25).toInt())

            mHandle?.let {
                if (it.measuredHeight != handleHeight) {
                    Log.i(LOG_TAG, "Handle height not match")
                }
            }

            onSlide(0f)
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        drawableRect.set(left, top, right - left, bottom - top)

        val parentHeight = (parent as ViewGroup).measuredHeight
        heightPixels = context.resources.displayMetrics.heightPixels
        fullHeight = max(parentHeight, heightPixels)

        val containerHeight = heightPixels - insetTop
        if (container.measuredHeight != containerHeight) {
            container.layoutParams.height = containerHeight
            container.requestLayout()
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val ss = SavedState(superState!!)
        ss.handleHeight = handleHeight
        ss.insetTop = insetTop
        return ss
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        val ss = state as SavedState
        super.onRestoreInstanceState(ss.superState)
        handleHeight = ss.handleHeight
        insetTop = ss.insetTop
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        ViewCompat.requestApplyInsets(this)

        if (mHandle == null && handleId != 0) {
            mHandle = findViewById(handleId)
        }

        val children = mutableListOf<View>()
        repeat(childCount) { idx ->
            getChildAt(idx)?.let { child ->
                val isHandle = mHandle != null && child == mHandle
                if (!isHandle && child != container) {
                    children.add(child)
                }
            }
        }
        for (i in children.indices) {
            val child = children[i]
            removeViewInLayout(child)
            container.addView(child)
        }
        container.requestLayout()
    }

    private fun onSlide(offset: Float) {
        var transY = handleHeight * (1f - offset)
        transY += insetTop.toFloat() * offset

        container.translationY = transY

        val alpha = max(0f, min(1f - (offset * 2f), 1f))
        mHandle?.alpha = alpha
        mHandle?.visibility = if (alpha > 1f) View.INVISIBLE else View.VISIBLE

        if (mHandle == null) {
            handleArrow.alpha = (255 * alpha).toInt()
        } else {
            handleArrow.alpha = 0
        }

        if (offset > OFFSET_TRIGGER) {
            val interOffset = ((offset - OFFSET_TRIGGER) * (1f / (1f - OFFSET_TRIGGER)))
            backgroundDrawable.interpolation = 1f - interOffset
            drawableRect.top = (transY - (insetTop * interOffset)).toInt()
        } else {
            backgroundDrawable.interpolation = 1f
            drawableRect.top = transY.toInt()
        }
        invalidate()
    }

    fun setHandle(@IdRes id: Int) {
        handleId = id
    }

    fun setCornerRadius(radius: Float) {
        cornerRadius = radius
        backgroundDrawable.shapeAppearanceModel = ShapeAppearanceModel().toBuilder().apply {
            setTopLeftCorner(CornerFamily.ROUNDED, cornerRadius)
            setTopRightCorner(CornerFamily.ROUNDED, cornerRadius)
        }.build()
        invalidate()
    }

    class SavedState : BaseSavedState {
        var handleHeight: Int = 0
        var insetTop: Int = 0

        constructor(superState: Parcelable) : super(superState)

        /**
         * Constructor called from [.CREATOR]
         */
        private constructor(source: Parcel) : super(source) {
            handleHeight = source.readInt()
            insetTop = source.readInt()
        }

        @RequiresApi(Build.VERSION_CODES.N)
        constructor(source: Parcel, loader: ClassLoader?) : super(source, loader) {
            handleHeight = source.readInt()
            insetTop = source.readInt()
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeInt(handleHeight)
            out.writeInt(insetTop)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(parcel: Parcel): SavedState {
                return SavedState(parcel)
            }

            override fun newArray(size: Int): Array<SavedState?> {
                return arrayOfNulls(size)
            }
        }
    }

    private class HandleArrowDrawable : Drawable() {

        private var topOffset = 0

        private val inner = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = 1.8f * SCALE
        }

        private val outer = Paint().apply {
            isAntiAlias = true
            color = Color.GRAY
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = 3.6f * SCALE
        }

        override fun draw(canvas: Canvas) {
            val saveCount = canvas.save()
            val x = (canvas.clipBounds.width() - intrinsicWidth) / 2f
            canvas.translate(getScaled(11.5f) + x, getScaled(11.5f) + topOffset)

            canvas.drawLine(getScaled(2f), getScaled(8.5f),
                getScaled(6.5f), getScaled(4f), outer)
            canvas.drawLine(getScaled(6.5f), getScaled(4f),
                getScaled(11f), getScaled(8.5f), outer)

            canvas.drawLine(getScaled(2f), getScaled(8.5f),
                getScaled(6.5f), getScaled(4f), inner)
            canvas.drawLine(getScaled(6.5f), getScaled(4f),
                getScaled(11f), getScaled(8.5f), inner)

            canvas.restoreToCount(saveCount)
        }

        override fun setAlpha(alpha: Int) {
            inner.alpha = alpha
            outer.alpha = alpha
            invalidateSelf()
        }

        override fun setColorFilter(cf: ColorFilter?) {
        }

        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

        override fun getIntrinsicHeight(): Int = getScaled(SIZE).toInt()

        override fun getIntrinsicWidth(): Int = getScaled(SIZE).toInt()

        private fun getScaled(value: Float) : Float = value * SCALE

        fun setTopOffset(offset: Int) {
            if (topOffset != offset) {
                topOffset = offset
                invalidateSelf()
            }
        }

        companion object {
            private const val SIZE = 36f
            private const val SCALE = 3.5f
        }
    }

    companion object {
        private const val CORNER_RADIUS = 25
        private const val OFFSET_TRIGGER = 0.75f
        private const val LOG_TAG = "APP_DRAWER"

        @SuppressLint("ObsoleteSdkInt")
        private fun getColorStateList(
            context: Context, attributes: TypedArray, @StyleableRes index: Int
        ): ColorStateList? {
            if (attributes.hasValue(index)) {
                val resourceId = attributes.getResourceId(index, 0)
                if (resourceId != 0) {
                    val value = AppCompatResources.getColorStateList(context, resourceId)
                    if (value != null) {
                        return value
                    }
                }
            }

            // Reading a single color with getColorStateList() on API 15 and below doesn't always correctly
            // read the value. Instead we'll first try to read the color directly here.
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                val color = attributes.getColor(index, -1)
                if (color != -1) {
                    return ColorStateList.valueOf(color)
                }
            }
            return attributes.getColorStateList(index)
        }
    }
}