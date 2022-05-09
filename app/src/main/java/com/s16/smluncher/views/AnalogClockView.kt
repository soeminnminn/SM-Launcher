package com.s16.smluncher.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import java.util.*
import kotlin.math.*

// REF: https://github.com/MohammadRezaei92/MinimalistAnalogClock
class AnalogClockView : View {

    private val mClock = ClockDrawable()
    private val ATTRS = intArrayOf(
        android.R.attr.textColorHint,
        android.R.attr.colorError
    )

    var showSecond = mClock.showSecond
        set(value) {
            if (field != value) {
                field = value
                mClock.showSecond = value
                invalidate()
            }
        }

    constructor(context: Context)
            : super(context) {
        initialize(context, null)
    }

    constructor(context: Context, attrs: AttributeSet)
            : super(context, attrs) {
        initialize(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int)
            : super(context, attrs, defStyle) {
        initialize(context, attrs)
    }

    @SuppressLint("ResourceType")
    private fun initialize(context: Context, attrs: AttributeSet?) {
        val a = context.obtainStyledAttributes(attrs, ATTRS)
        val textColorHint = a.getColor(0, 0)
        val errorColor = a.getColor(1, 0)
        a.recycle()

        val colorPrimary = getPrimaryColor(context)
        val colorSecondary = getSecondaryColor(context)
        val colorOnSecondary = getOnSecondaryColor(context)

        mClock.hourDotColor = if (colorSecondary == 0) ClockDrawable.HOUR_DOT_COLOR
            else colorSecondary

        mClock.minuteDotColor = if (textColorHint == 0) ClockDrawable.MINUTE_DOT_COLOR
            else textColorHint

        mClock.secondDotColor = if (errorColor == 0) ClockDrawable.SECOND_DOT_COLOR
            else errorColor

        mClock.handsColor = if (colorOnSecondary == 0) ClockDrawable.HAND_COLOR
            else colorOnSecondary

        mClock.centerCirclePrimaryColor = if (colorSecondary == 0) ClockDrawable.CENTER_CIRCLE_PRIMARY_COLOR
            else colorSecondary

        mClock.centerCircleSecondaryColor = if (colorPrimary == 0) ClockDrawable.CENTER_CIRCLE_SECONDARY_COLOR
            else colorPrimary
    }

    private fun getPrimaryColor(context: Context): Int {
        val colorAttr = context.resources.getIdentifier("colorPrimary", "attr", context.packageName)
        val outValue = TypedValue()
        context.theme.resolveAttribute(colorAttr, outValue, true)
        return outValue.data
    }

    private fun getSecondaryColor(context: Context): Int {
        val colorAttr = context.resources.getIdentifier("colorSecondary", "attr", context.packageName)
        val outValue = TypedValue()
        context.theme.resolveAttribute(colorAttr, outValue, true)
        return outValue.data
    }

    private fun getOnSecondaryColor(context: Context): Int {
        val colorAttr = context.resources.getIdentifier("colorOnSecondary", "attr", context.packageName)
        val outValue = TypedValue()
        context.theme.resolveAttribute(colorAttr, outValue, true)
        return outValue.data
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var widthSpec = widthMeasureSpec
        var heightSpec = heightMeasureSpec

        val initialWidth = MeasureSpec.getSize(widthMeasureSpec)
        val initialHeight = MeasureSpec.getSize(heightMeasureSpec)

        if (initialWidth != initialHeight) {
            val widthMode = MeasureSpec.getMode(widthMeasureSpec)
            val heightMode = MeasureSpec.getMode(heightMeasureSpec)
            when {
                widthMode == MeasureSpec.EXACTLY -> {
                    heightSpec = MeasureSpec.makeMeasureSpec(initialWidth, MeasureSpec.EXACTLY)
                }
                heightMode == MeasureSpec.EXACTLY -> {
                    widthSpec = MeasureSpec.makeMeasureSpec(initialHeight, MeasureSpec.EXACTLY)
                }
                else -> {
                    val baseSize = when {
                        initialWidth <= 0 -> {
                            initialHeight
                        }
                        initialHeight <= 0 -> {
                            initialWidth
                        }
                        else -> {
                            min(initialWidth, initialHeight)
                        }
                    }
                    widthSpec = MeasureSpec.makeMeasureSpec(baseSize, MeasureSpec.EXACTLY)
                    heightSpec = MeasureSpec.makeMeasureSpec(baseSize, MeasureSpec.EXACTLY)
                }
            }
        }

        super.onMeasure(widthSpec, heightSpec)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        mClock.setBounds(left, top, right, bottom)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        postInvalidateDelayed(1000)
    }

    override fun onDrawForeground(canvas: Canvas?) {
        canvas?.let {
            mClock.tick()
            mClock.draw(it)
        }
    }

    private class ClockDrawable : Drawable() {

        /** The coordinates used to paint the clock hands.  */
        private var xHandSec: Int = 0
        private var yHandSec: Int = 0
        private var xHandMin: Int = 0
        private var yHandMin: Int = 0
        private var xHandHour: Int = 0
        private var yHandHour: Int = 0

        private var mWidth = 0
        private var mHeight = 0

        private val mPaint = Paint().apply {
            isAntiAlias = true
        }

        var hourDotColor = HOUR_DOT_COLOR
            set(value) {
                if (field != value) {
                    field = value
                    invalidateSelf()
                }
            }
        var minuteDotColor = MINUTE_DOT_COLOR
            set(value) {
                if (field != value) {
                    field = value
                    invalidateSelf()
                }
            }
        var handsColor = HAND_COLOR
            set(value) {
                if (field != value) {
                    field = value
                    invalidateSelf()
                }
            }
        var secondDotColor = SECOND_DOT_COLOR
            set(value) {
                if (field != value) {
                    field = value
                    invalidateSelf()
                }
            }
        var showSecond = true
            set(value) {
                if (field != value) {
                    field = value
                    invalidateSelf()
                }
            }
        var centerCirclePrimaryColor = CENTER_CIRCLE_PRIMARY_COLOR
            set(value) {
                if (field != value) {
                    field = value
                    invalidateSelf()
                }
            }
        var centerCircleSecondaryColor = CENTER_CIRCLE_SECONDARY_COLOR
            set(value) {
                if (field != value) {
                    field = value
                    invalidateSelf()
                }
            }

        var mCalendar = Calendar.getInstance()
            set(value) {
                if (field != value) {
                    field = value
                    getTime()
                }
            }

        /**
         * Converts current second/minute/hour to x and y coordinates.
         * @param radius The radius length
         * @return the coordinates point
         */
        private var xIsPos = true
        private var yIsPos = true
        private fun minToLocation(timeStep: Int, radius: Int): Point {
            val t =  2.0 * Math.PI * (timeStep - 15).toDouble() / 60
            when (timeStep) {
                in 0..15 -> {
                    xIsPos = true
                    yIsPos = true
                }
                in 16..30 -> {
                    xIsPos = true
                    yIsPos = false
                }
                in 31..45 -> {
                    xIsPos = false
                    yIsPos = false
                }
                in 46..60 -> {
                    xIsPos = false
                    yIsPos = true
                }
            }

            val xDegree = if(xIsPos) cos(t).absoluteValue.pow(9.0/11)
            else cos(t).absoluteValue.pow(9.0/11).times(-1)

            val yDegree = - if(yIsPos) sin(t).absoluteValue.pow(9.0/11)
            else sin(t).absoluteValue.pow(9.0/11).times(-1)

            val x = (WIDTH / 2 + radius * xDegree)
            val y = (HEIGHT / 2 + radius * yDegree)

            return Point(x.toInt(),y.toInt())
        }

        fun tick() {
            mCalendar = Calendar.getInstance()
            getTime()
        }

        /**
         * At each iteration we recalculate the coordinates of the clock hands,
         * and repaint everything.
         */
        private fun getTime() {
            val currentSecond = mCalendar.get(Calendar.SECOND)
            val currentMinute = mCalendar.get(Calendar.MINUTE)
            val currentHour = mCalendar.get(Calendar.HOUR)

            xHandSec = minToLocation(currentSecond, secondHandLength).x
            yHandSec = minToLocation(currentSecond, secondHandLength).y
            xHandMin = minToLocation(currentMinute, minuteHandLength).x
            yHandMin = minToLocation(currentMinute, minuteHandLength).y
            xHandHour = minToLocation(currentHour * 5 + getRelativeHour(currentMinute), hourHandLength).x
            yHandHour = minToLocation(currentHour * 5 + getRelativeHour(currentMinute), hourHandLength).y
            invalidateSelf()
        }

        private fun getRelativeHour(min: Int): Int {
            return min / 12
        }

        override fun onBoundsChange(bounds: Rect?) {
            super.onBoundsChange(bounds)
            bounds?.let {
                mWidth = it.right - it.left
                mHeight = it.bottom - it.top
            }
        }

        override fun draw(canvas: Canvas) {
            val scale = min(mWidth.toFloat() / WIDTH.toFloat(),
                mHeight.toFloat() / HEIGHT.toFloat())

            val saveCount = canvas.save()
            canvas.scale(scale, scale, 0f, 0f)

            // Draw the dots
            for (i in 0..59) {
                val dotCoordinates = minToLocation(i, DISTANCE_DOT_FROM_ORIGIN)

                if (i % 5 == 0) {
                    // big dot
                    mPaint.color = hourDotColor
                    canvas.drawCircle((dotCoordinates.x - DIAMETER_BIG_DOT / 2),
                        (dotCoordinates.y - DIAMETER_BIG_DOT / 2),
                        DIAMETER_BIG_DOT,
                        mPaint)
                } else {
                    // small dot
                    mPaint.color = minuteDotColor
                    canvas.drawCircle((dotCoordinates.x - DIAMETER_SMALL_DOT / 2),
                        (dotCoordinates.y - DIAMETER_SMALL_DOT / 2),
                        DIAMETER_SMALL_DOT,
                        mPaint)
                }
            }

            //Draw clock second hands
            if(showSecond) {
                mPaint.color = centerCirclePrimaryColor
                canvas.drawCircle((xHandSec - DIAMETER_SECOND_DOT / 2)
                    , (yHandSec - DIAMETER_SECOND_DOT / 2)
                    , (DIAMETER_SECOND_DOT + 2)
                    , mPaint)

                mPaint.color = secondDotColor
                canvas.drawCircle((xHandSec - DIAMETER_SECOND_DOT / 2)
                    , (yHandSec - DIAMETER_SECOND_DOT / 2)
                    , DIAMETER_SECOND_DOT
                    , mPaint)
            }

            // Draw the clock hands
            mPaint.color = handsColor
            mPaint.strokeWidth = 8f
            mPaint.strokeCap = Paint.Cap.ROUND
            canvas.drawLine((WIDTH.toFloat() / 2)
                , (HEIGHT.toFloat() / 2)
                , xHandMin.toFloat(), yHandMin.toFloat(), mPaint)
            canvas.drawLine((WIDTH.toFloat() / 2)
                , (HEIGHT.toFloat() / 2)
                , xHandHour.toFloat()
                , yHandHour.toFloat(), mPaint)

            //Draw center circle
            mPaint.color = centerCirclePrimaryColor
            canvas.drawCircle((WIDTH.toFloat() / 2)
                , (HEIGHT.toFloat() / 2)
                , DIAMETER_CENTER_BIG_DOT
                , mPaint)
            mPaint.color = centerCircleSecondaryColor
            canvas.drawCircle((WIDTH.toFloat() / 2)
                , (HEIGHT.toFloat() / 2)
                , DIAMETER_CENTER_SMALL_DOT
                , mPaint)

            canvas.restoreToCount(saveCount)
        }

        override fun setAlpha(alpha: Int) {
            mPaint.alpha = alpha
        }

        override fun setColorFilter(cf: ColorFilter?) {
        }

        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

        override fun getIntrinsicHeight(): Int = HEIGHT

        override fun getIntrinsicWidth(): Int = WIDTH

        companion object {
            /** The size of the clock.  */
            private const val WIDTH = 400
            private const val HEIGHT = 400

            /** The length of the clock hands relative to the clock size.  */
            private const val secondHandLength = WIDTH / 2 - 30
            private const val minuteHandLength = WIDTH / 2 - 100
            private const val hourHandLength = WIDTH / 2 - 130

            /** The distance of the dots from the origin (center of the clock).  */
            private const val DISTANCE_DOT_FROM_ORIGIN = WIDTH / 2 - 30

            private const val DIAMETER_BIG_DOT = 5f
            private const val DIAMETER_SMALL_DOT = 3f
            private const val DIAMETER_SECOND_DOT = 8f
            private const val DIAMETER_CENTER_BIG_DOT = 14f
            private const val DIAMETER_CENTER_SMALL_DOT = 6f

            internal val HOUR_DOT_COLOR = Color.parseColor("#53cfff")
            internal val MINUTE_DOT_COLOR = Color.WHITE
            internal val HAND_COLOR = Color.WHITE
            internal val SECOND_DOT_COLOR = Color.parseColor("#fe6f70")
            internal val CENTER_CIRCLE_PRIMARY_COLOR = Color.parseColor("#0e5876")
            internal val CENTER_CIRCLE_SECONDARY_COLOR = Color.parseColor("#fe6f70")
        }
    }
}