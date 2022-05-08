package com.s16.smluncher.views

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import java.util.*
import kotlin.math.min

// https://github.com/JaynmBo/ClockCustomizeView/
class ClockView : View {

    private val mClock = ClockDrawable()

    constructor(context: Context)
            : super(context) {
    }

    constructor(context: Context, attrs: AttributeSet)
            : super(context, attrs) {
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int)
            : super(context, attrs, defStyle) {
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

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        postInvalidateDelayed(1000)
    }

    override fun onDrawForeground(canvas: Canvas?) {
        super.onDrawForeground(canvas)
        canvas?.let {
            mClock.draw(it)
        }
    }

    class ClockDrawable : Drawable() {

        private var mWidth = 0
        private var mHeight = 0
        private var radius = RADIUS
        private val mRect = Rect()

        private val mPaint = Paint().apply {
            textSize = 35F
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }

        override fun draw(canvas: Canvas) {
            val centerX: Float = (mWidth / 2).toFloat()
            val centerY: Float = (mHeight / 2).toFloat()

            canvas.translate(centerX, centerY)

            drawClock(canvas)
            drawClockScale(canvas)
            drawPointer(canvas)
        }

        private fun drawClock(canvas: Canvas) {
            mPaint.strokeWidth = mCircleWidth
            mPaint.color = Color.BLACK
            mPaint.style = Paint.Style.STROKE
            canvas.drawCircle(0F, 0F, radius, mPaint)
        }

        private fun drawClockScale(canvas: Canvas) {
            for (index in 1..60) {
                canvas.rotate(6F, 0F, 0F)
                if (index % 5 == 0) {
                    mPaint.strokeWidth = 4.0F
                    canvas.drawLine(0F, -radius, 0F, -radius + scaleMax, mPaint)
                    canvas.save()
                    mPaint.strokeWidth = 1.0F
                    mPaint.style = Paint.Style.FILL
                    mPaint.getTextBounds(
                        (index / 5).toString(),
                        0,
                        (index / 5).toString().length,
                        mRect
                    )
                    canvas.translate(0F, -radius + mNumberSpace + scaleMax + (mRect.height() / 2))
                    canvas.rotate((index * -6).toFloat())
                    canvas.drawText(
                        (index / 5).toString(), -mRect.width() / 2.toFloat(),
                        mRect.height().toFloat() / 2, mPaint
                    )
                    canvas.restore()
                }
                else {
                    mPaint.strokeWidth = 2.0F
                    canvas.drawLine(0F, -radius, 0F, -radius + scaleMin, mPaint)
                }
            }
        }

        private fun drawPointer(canvas: Canvas) {
            val calendar = Calendar.getInstance()
            val hour = calendar[Calendar.HOUR]
            val minute = calendar[Calendar.MINUTE]
            val second = calendar[Calendar.SECOND]

            val angleHour = (hour + minute.toFloat() / 60) * 360 / 12
            val angleMinute = (minute + second.toFloat() / 60) * 360 / 60
            val angleSecond = second * 360 / 60

            canvas.save()
            canvas.rotate(angleHour, 0F, 0F)
            val rectHour = RectF(
                -mHourPointWidth / 2,
                -radius / 2,
                mHourPointWidth / 2,
                radius / 6
            )
            mPaint.color = Color.BLUE
            mPaint.style = Paint.Style.STROKE
            mPaint.strokeWidth = mHourPointWidth
            canvas.drawRoundRect(rectHour, mPointRange, mPointRange, mPaint)
            canvas.restore()

            canvas.save()
            canvas.rotate(angleMinute, 0F, 0F)
            val rectMinute = RectF(
                -mMinutePointWidth / 2,
                -radius * 3.5f / 5,
                mMinutePointWidth / 2,
                radius / 6
            )
            mPaint.color = Color.BLACK
            mPaint.strokeWidth = mMinutePointWidth
            canvas.drawRoundRect(rectMinute, mPointRange, mPointRange, mPaint)
            canvas.restore()

            canvas.save()
            canvas.rotate(angleSecond.toFloat(), 0F, 0F)
            val rectSecond = RectF(
                -mSecondPointWidth / 2,
                -radius + 10,
                mSecondPointWidth / 2,
                radius / 6
            )
            mPaint.strokeWidth = mSecondPointWidth
            mPaint.color = Color.RED
            canvas.drawRoundRect(rectSecond, mPointRange, mPointRange, mPaint)
            canvas.restore()

            mPaint.style = Paint.Style.FILL
            canvas.drawCircle(
                0F,
                0F, mSecondPointWidth * 4, mPaint
            )
        }

        override fun onBoundsChange(bounds: Rect?) {
            super.onBoundsChange(bounds)

            mWidth = (bounds?.width() ?: intrinsicWidth) - (mCircleWidth * 2).toInt()
            mHeight = (bounds?.height() ?: intrinsicHeight) - (mCircleWidth * 2).toInt()

            radius = (mWidth - mCircleWidth * 2) / 2
        }

        override fun setAlpha(alpha: Int) {
            mPaint.alpha = alpha
        }

        override fun setColorFilter(colorFilter: ColorFilter?) {
        }

        override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

        override fun getIntrinsicHeight(): Int = (RADIUS * 2).toInt()

        override fun getIntrinsicWidth(): Int = (RADIUS * 2).toInt()

        companion object {
            private const val RADIUS = 300f
            private const val mHourPointWidth = 15f
            private const val mMinutePointWidth = 10f
            private const val mSecondPointWidth = 4f
            private const val mPointRange = 20F
            private const val mNumberSpace = 10f
            private const val mCircleWidth = 4.0F

            private const val scaleMax = 50
            private const val scaleMin = 25
        }
    }
}