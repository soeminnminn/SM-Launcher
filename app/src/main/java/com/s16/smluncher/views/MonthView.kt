package com.s16.smluncher.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.text.TextPaint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.os.ConfigurationCompat
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.*
import kotlin.math.min


class MonthView : AppCompatTextView, View.OnTouchListener {

    private data class DayCBean(
        var year: Int,
        var month: Int,
        var day: Int,
        var isToday: Boolean,
        var isCurrentMon: Boolean = true
    ) {
        override fun toString(): String = "$day"
    }

    private var mCellWidth = 0
    private var mCellHeight = 0
    private var mCellRadius = 48f

    private var mCurrentCalendar: Calendar = Calendar.getInstance()

    private var mMonth = 0
    private var mYear = 0
    private var mCalendars = ArrayList<DayCBean>()
    private val mDayNames = mutableListOf<String>()
    private val mMonthNames = MONTH_NAMES

    private var mTodayColor = Color.parseColor("#DAF8E7")
    private val mDayTextPaint = TextPaint()

    private val mTodayPaint = Paint().apply {
        isAntiAlias = true
        color = mTodayColor
    }

    private val mArrowPaint = Paint().apply {
        isAntiAlias = true
        color = Color.GRAY
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private val mDayRect = RectF()
    private val mLeftArrowRect = RectF()
    private val mRightArrowRect = RectF()
    private val mYmRect = RectF()

    private val mPressPaint = Paint().apply {
        isAntiAlias = true
        color = Color.LTGRAY
    }
    private var mLeftPressing = false
    private var mCenterPressing = false
    private var mRightPressing = false

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
        val colorPrimary = getPrimaryColor(context)
        if (colorPrimary > 0) {
            mTodayColor = Color.argb(120, Color.red(colorPrimary), Color.green(colorPrimary), Color.blue(colorPrimary))
        }
        setWillNotDraw(false)

        mDayNames.addAll(getDayNames(context))
        setMonthParameter()

        setOnTouchListener(this)
    }

    private fun getPrimaryColor(context: Context): Int {
        val colorAttr = context.resources.getIdentifier("colorPrimary", "attr", context.packageName)
        val outValue = TypedValue()
        context.theme.resolveAttribute(colorAttr, outValue, true)
        return outValue.data
    }

    private fun getDayNames(context: Context): List<String> {
        val names = mutableListOf<String>()
        val currentLocale = ConfigurationCompat.getLocales(context.resources.configuration)[0]
        for (d in DayOfWeek.values()) {
            val dayName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                d.getDisplayName(TextStyle.SHORT, currentLocale)
            } else {
                d.name.subSequence(0, 3).toString()
            }
            names.add(dayName)
        }
        if (names.size == 7) {
            val sunday = names[6]
            names.add(0, sunday)
            names.removeAt(7)
        } else {
            names.clear()
            names.addAll(DAY_NAMES)
        }
        return names
    }

    private fun setMonthParameter(calendar: Calendar = Calendar.getInstance()) {
        if (mMonth == calendar[Calendar.MONTH] && mYear == calendar[Calendar.YEAR]) {
            return
        }

        mCalendars.clear()
        mMonth = calendar[Calendar.MONTH]
        mYear = calendar[Calendar.YEAR]

        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfMonthWeek = calendar.get(Calendar.DAY_OF_WEEK)
        var offDay = firstDayOfMonthWeek - 1
        if (offDay == 0) offDay = 7

        calendar.add(Calendar.MONTH, -1)
        getCalendarRang(calendar, calendar.getActualMaximum(Calendar.DAY_OF_MONTH) - offDay + 1,
            calendar.getActualMaximum(Calendar.DAY_OF_MONTH))

        calendar.add(Calendar.MONTH, 1)
        getCalendarRang(calendar, 1, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))

        calendar.add(Calendar.MONTH, 1)
        val nextMonthDay = 42 - mCalendars.size
        if (nextMonthDay > 0) {
            getCalendarRang(calendar, 1, nextMonthDay)
        }
        invalidate()
    }

    private fun getCalendarRang(calendar: Calendar, startDay: Int, endDay: Int) {
        for (day in startDay..endDay) {
            calendar.set(Calendar.DAY_OF_MONTH, day)
            mCalendars.add(
                DayCBean(
                    calendar[Calendar.YEAR],
                    calendar[Calendar.MONTH],
                    calendar[Calendar.DAY_OF_MONTH],
                    isToday = isCurrentDay(calendar),
                    isCurrentMon = calendar[Calendar.MONTH] == mMonth
                )
            )
        }
    }

    private fun isCurrentDay(calendar: Calendar): Boolean {
        return mCurrentCalendar[Calendar.YEAR] == calendar[Calendar.YEAR] &&
                mCurrentCalendar[Calendar.MONTH] == calendar[Calendar.MONTH]
                && mCurrentCalendar[Calendar.DAY_OF_MONTH] == calendar[Calendar.DAY_OF_MONTH]
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

        val vWidth = (right - left) - (paddingStart + paddingEnd)
        val vHeight = (bottom - top) - (paddingTop + paddingBottom)

        mCellWidth = vWidth / 7
        mCellHeight = min(mCellWidth, vHeight / 8)
        mCellRadius = min(mCellWidth, mCellHeight) * 0.5f

        mLeftArrowRect.set(0f, 0f, mCellWidth.toFloat(), mCellHeight.toFloat())
        mYmRect.set(mCellWidth.toFloat(), 0f, (vWidth - mCellWidth).toFloat(), mCellHeight.toFloat())
        mRightArrowRect.set((vWidth - mCellWidth).toFloat(), 0f, vWidth.toFloat(), mCellHeight.toFloat())
    }

    override fun onDraw(canvas: Canvas?) {
        // super.onDraw(canvas)
        var row = 1
        var cell = 0
        canvas?.let {  c ->
            mDayTextPaint.set(textMetricsParamsCompat.textPaint)
            mDayTextPaint.textAlign = Paint.Align.CENTER
            mDayTextPaint.isFakeBoldText = true
            mDayTextPaint.textSize = textSize * 1.1f

            val textColor = currentTextColor
            val textColorHint = currentHintTextColor

            var fontMetrics = mDayTextPaint.fontMetrics
            var halfLineHeight = (fontMetrics.ascent + fontMetrics.descent) * 0.5f

            if (paddingStart > 0 || paddingTop > 0) {
                c.translate(paddingStart.toFloat(), paddingTop.toFloat())
            }

            mPressPaint.color = if (mLeftPressing) Color.LTGRAY else Color.TRANSPARENT
            c.drawCircle(mLeftArrowRect.centerX(), mLeftArrowRect.centerY(), mCellRadius, mPressPaint)
            drawArrow(c, mLeftArrowRect, false)

            mPressPaint.color = if (mCenterPressing) Color.LTGRAY else Color.TRANSPARENT
            c.drawRoundRect(mYmRect, mCellRadius, mCellRadius, mPressPaint)
            c.drawText("$mYear - ${mMonthNames[mMonth]}", mYmRect.centerX(), mYmRect.centerY() - halfLineHeight, mDayTextPaint)

            mPressPaint.color = if (mRightPressing) Color.LTGRAY else Color.TRANSPARENT
            c.drawCircle(mRightArrowRect.centerX(), mRightArrowRect.centerY(), mCellRadius, mPressPaint)
            drawArrow(c, mRightArrowRect, true)

            mDayTextPaint.isFakeBoldText = false
            mDayTextPaint.textSize = textSize

            fontMetrics = mDayTextPaint.fontMetrics
            halfLineHeight = (fontMetrics.ascent + fontMetrics.descent) * 0.5f

            mDayNames.forEach {
                val x = (mCellWidth * cell + mCellWidth.toFloat() / 2)
                val y = (mCellHeight * row + mCellHeight.toFloat() / 2)
                c.drawText(it, x, y - halfLineHeight, mDayTextPaint)
                cell++
            }
            row++
            cell = 0

            mCalendars.forEach { day ->
                val x = (mCellWidth * cell + mCellWidth.toFloat() / 2)
                val y = (mCellHeight * row + mCellHeight.toFloat() / 2)
                mDayRect.set(x - mCellRadius, y - mCellRadius, x + mCellRadius, y + mCellRadius)

                when {
                    day.isToday && day.isCurrentMon -> {
                        mDayTextPaint.color = textColor
                        mTodayPaint.color = if (isNightMode()) {
                            darkenColor(mTodayColor)
                        } else {
                            mTodayColor
                        }

                        c.drawCircle(mDayRect.centerX(), mDayRect.centerY(), mCellRadius, mTodayPaint)
                        c.drawText("$day", mDayRect.centerX(), mDayRect.centerY() - halfLineHeight, mDayTextPaint)
                    }
                    day.isCurrentMon -> {
                        mDayTextPaint.color = textColor
                        c.drawText("$day", mDayRect.centerX(), mDayRect.centerY() - halfLineHeight, mDayTextPaint)
                    }
                    else -> {
                        mDayTextPaint.color = textColorHint
                        c.drawText("$day", mDayRect.centerX(), mDayRect.centerY() - halfLineHeight, mDayTextPaint)
                    }
                }
                cell++
                if (cell == 7) {
                    cell = 0
                    row++
                }
            }
        }
    }

    private fun drawArrow(canvas: Canvas, rect: RectF, isRight: Boolean) {
        val scale = min(rect.width(), rect.height()) / 24f
        mArrowPaint.strokeWidth = 2.4f.times(scale)

        val saveCount = canvas.save()
        canvas.translate(rect.centerX(), rect.centerY())

        if (isRight) {
            canvas.drawLine(-(1.5f.times(scale)), -(4f.times(scale)), 1.5f.times(scale), 0f, mArrowPaint)
            canvas.drawLine(1.5f.times(scale), 0f, -(1.5f.times(scale)), 4f.times(scale), mArrowPaint)
        } else {
            canvas.drawLine(1.5f.times(scale), -(4f.times(scale)), -(1.5f.times(scale)), 0f, mArrowPaint)
            canvas.drawLine(-(1.5f.times(scale)), 0f, 1.5f.times(scale), 4f.times(scale), mArrowPaint)
        }
        canvas.restoreToCount(saveCount)
    }

    private fun isNightMode(): Boolean {
        val nightModeFlags = context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES
    }

    private fun darkenColor(color: Int): Int {
        return Color.HSVToColor(FloatArray(3).apply {
            Color.colorToHSV(color, this)
            this[2] *= 0.8f
        })
    }

    override fun onTouch(view: View?, event: MotionEvent?): Boolean {
        return event?.let { e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (mLeftArrowRect.contains(e.x, e.y)) {
                        mLeftPressing = true
                        invalidate()
                    } else if (mRightArrowRect.contains(e.x, e.y)) {
                        mRightPressing = true
                        invalidate()
                    } else if (mYmRect.contains(e.x, e.y)) {
                        mCenterPressing = true
                        invalidate()
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (mLeftPressing) previous()
                    mLeftPressing = false

                    if (mRightPressing) next()
                    mRightPressing = false

                    if (mCenterPressing) today()
                    mCenterPressing = false

                    invalidate()
                    true
                }
                else -> false
            }
        } ?: false
    }

    fun previous() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.MONTH, mMonth - 1)
        setMonthParameter(calendar)
    }

    fun next() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.MONTH, mMonth + 1)
        setMonthParameter(calendar)
    }

    fun today() {
        setMonthParameter()
    }

    companion object {
        private val MONTH_NAMES = arrayListOf(
            "January", "February", "March", "April", "May", "June", "July",
            "August", "September", "October", "November", "December"
        )
        private val DAY_NAMES = arrayListOf(
            "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"
        )
    }
}