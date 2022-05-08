package com.s16.smluncher.views

import android.content.Context
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.view.View
import com.s16.smluncher.R
import kotlin.math.roundToInt

class BatteryMeterView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.batteryMeterStyle
) : View(context, attrs, defStyleAttr) {

    private val padding = Rect()
    private val bounds = Rect()

    private val shapeBounds = Rect()
    private val batteryPath = Path()
    private val indicatorPath = Path()
    private val chargeLevelClipRect = RectF()

    private var aspectRatio: Float = 0.5f
        set(value) {
            if (field != value) {
                field = value
                updateBatteryShapeBounds()
            }
        }

    private val batteryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val chargeLevelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val indicatorPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = Color.TRANSPARENT
    }

    var chargeLevel: Int? = null
        set(value) {
            val newChargeLevel = value?.coerceIn(
                MINIMUM_CHARGE_LEVEL,
                MAXIMUM_CHARGE_LEVEL
            )
            if (field != newChargeLevel) {
                field = newChargeLevel
                updateIndicatorPath()
                updateChargeLevelClipRect()
                updatePaintColors()
                invalidate()
            }
        }

    var criticalChargeLevel: Int? = CRITICAL_CHARGE_LEVEL
        set(value) {
            val newCriticalChargeLevel = value?.coerceIn(
                MINIMUM_CHARGE_LEVEL,
                MAXIMUM_CHARGE_LEVEL
            )
            if (field != newCriticalChargeLevel) {
                field = newCriticalChargeLevel
                updateIndicatorPath()
                updatePaintColors()
                invalidate()
            }
        }

    var isCharging: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                updateIndicatorPath()
                updatePaintColors()
                invalidate()
            }
        }

    var color: Int = getColorAttr(context, android.R.attr.colorForeground)
        set(value) {
            if (field != value) {
                field = value
                updatePaintColors()
                invalidate()
            }
        }

    var indicatorColor: Int
        get() = indicatorPaint.color
        set(value) {
            indicatorPaint.color = value
            invalidate()
        }

    var chargingColor: Int? = null
        set(value) {
            if (field != value) {
                field = value
                updatePaintColors()
                invalidate()
            }
        }

    var criticalColor: Int? = null
        set(value) {
            if (field != value) {
                field = value
                updatePaintColors()
                invalidate()
            }
        }

    var unknownColor: Int? = null
        set(value) {
            if (field != value) {
                field = value
                updatePaintColors()
                invalidate()
            }
        }


    init {
        val typedArray = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.BatteryMeterView,
            defStyleAttr,
            R.style.Widget_BatteryMeter
        )

        if (typedArray.hasValue(R.styleable.BatteryMeterView_batteryMeterChargeLevel)) {
            chargeLevel = typedArray.getInt(R.styleable.BatteryMeterView_batteryMeterChargeLevel, 0)
        }

        if (typedArray.hasValue(R.styleable.BatteryMeterView_batteryMeterCriticalChargeLevel)) {
            criticalChargeLevel = typedArray.getInt(
                R.styleable.BatteryMeterView_batteryMeterCriticalChargeLevel, 0
            )
        }

        isCharging =
            typedArray.getBoolean(R.styleable.BatteryMeterView_batteryMeterIsCharging, isCharging)

        color = typedArray.getColor(R.styleable.BatteryMeterView_batteryMeterColor, color)
        indicatorColor = typedArray.getColor(
            R.styleable.BatteryMeterView_batteryMeterIndicatorColor,
            indicatorColor
        )

        if (typedArray.hasValue(R.styleable.BatteryMeterView_batteryMeterChargingColor)) {
            chargingColor =
                typedArray.getColor(R.styleable.BatteryMeterView_batteryMeterChargingColor, color)
        }

        if (typedArray.hasValue(R.styleable.BatteryMeterView_batteryMeterCriticalColor)) {
            criticalColor =
                typedArray.getColor(R.styleable.BatteryMeterView_batteryMeterCriticalColor, color)
        }

        if (typedArray.hasValue(R.styleable.BatteryMeterView_batteryMeterUnknownColor)) {
            unknownColor =
                typedArray.getColor(R.styleable.BatteryMeterView_batteryMeterUnknownColor, color)
        }

        typedArray.recycle()
        setPaddingInternal(paddingLeft, paddingTop, paddingRight, paddingBottom)
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        val saveCount = canvas.save()
        if (!indicatorPath.isEmpty) {
            canvas.drawPath(indicatorPath, indicatorPaint)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                canvas.clipOutPath(indicatorPath)
            } else {
                @Suppress("DEPRECATION")
                canvas.clipPath(indicatorPath, Region.Op.DIFFERENCE)
            }
        }

        canvas.drawPath(batteryPath, batteryPaint)

        if (!chargeLevelClipRect.isEmpty) {
            canvas.clipRect(chargeLevelClipRect)
            canvas.drawPath(batteryPath, chargeLevelPaint)
        }
        canvas.restoreToCount(saveCount)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        bounds.set(left, top, right, bottom)
        updateBatteryShapeBounds()
    }

    override fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        super.setPadding(left, top, right, bottom)
        setPaddingInternal(left, top, right, bottom)
    }

    override fun setPaddingRelative(start: Int, top: Int, end: Int, bottom: Int) {
        super.setPaddingRelative(start, top, end, bottom)
        when (layoutDirection) {
            LAYOUT_DIRECTION_RTL -> setPaddingInternal(end, top, start, bottom)
            else -> setPaddingInternal(start, top, end, bottom)
        }
    }

    private fun setPaddingInternal(left: Int, top: Int, right: Int, bottom: Int) {
        padding.set(left, top, right, bottom)
        updateBatteryShapeBounds()
    }

    private fun updateBatteryShapeBounds() {
        if (bounds.isEmpty) return

        val availableWidth = bounds.width() - padding.left - padding.right
        val availableHeight = bounds.height() - padding.top - padding.bottom
        val availableAspectRatio = availableWidth.toFloat() / availableHeight

        if (availableAspectRatio > aspectRatio) {
            shapeBounds.set(0, 0, (availableHeight * aspectRatio).toInt(), availableHeight)
        } else {
            shapeBounds.set(0, 0, availableWidth, (availableWidth / aspectRatio).toInt())
        }

        shapeBounds.offset(
            padding.left + (availableWidth - shapeBounds.width()) / 2,
            padding.top + (availableHeight - shapeBounds.height()) / 2
        )

        updateBatteryPath()
        updateIndicatorPath()
        updateChargeLevelClipRect()
    }

    private fun updateBatteryPath() {
        batteryPath.reset()
        performPathCommand(batteryPath, 'M', 0.87f, 0.1f)
        performPathCommand(batteryPath,'L', 0.7f, 0.1f)
        performPathCommand(batteryPath,'L', 0.7f, 0.05f)
        performPathCommand(batteryPath,'C', 0.7f, 0.025f, 0.66f, 0f, 0.6f, 0f)
        performPathCommand(batteryPath,'L', 0.4f, 0f)
        performPathCommand(batteryPath,'C', 0.34f, 0f, 0.3f, 0.025f, 0.3f, 0.05f)
        performPathCommand(batteryPath,'L', 0.3f, 0.1f)
        performPathCommand(batteryPath,'L', 0.13f, 0.1f)
        performPathCommand(batteryPath,'C', 0.06f, 0.1f, 0f, 0.13f, 0f, 0.165f)
        performPathCommand(batteryPath,'L', 0f, 0.935f)
        performPathCommand(batteryPath,'C', 0f, 0.975f, 0.06f, 1f, 0.13f, 1f)
        performPathCommand(batteryPath,'L', 0.87f, 1f)
        performPathCommand(batteryPath,'C', 0.94f, 1f, 1f, 0.97f, 1f, 0.935f)
        performPathCommand(batteryPath,'L', 1f, 0.165f)
        performPathCommand(batteryPath,'C', 1.01f, 0.13f, 0.94f, 0.1f, 0.87f, 0.1f)
        batteryPath.close()
    }

    private fun updateIndicatorPath() {
        val currentLevel = chargeLevel
        val currentCriticalLevel = criticalChargeLevel

        if (currentLevel == null) {
            performUnknownIndicatorPath(indicatorPath)
        } else if (isCharging) {
            performChargingIndicatorPath(indicatorPath)
        } else if (currentCriticalLevel != null && currentLevel <= currentCriticalLevel) {
            performAlertIndicatorPath(indicatorPath)
        } else {
            indicatorPath.reset()
        }
    }

    private fun performUnknownIndicatorPath(path: Path) {
        path.reset()
        performPathCommand(path, 'M', 0.6f, 0.8f)
        performPathCommand(path, 'L', 0.4f, 0.8f)
        performPathCommand(path, 'L', 0.4f, 0.7f)
        performPathCommand(path, 'L', 0.6f, 0.7f)
        path.close()
        performPathCommand(path, 'M', 0.73f, 0.5345f)
        performPathCommand(path, 'C', 0.73f, 0.5345f, 0.692f, 0.5555f, 0.663f, 0.57f)
        performPathCommand(path, 'C', 0.649f, 0.577f, 0.636f, 0.585f, 0.624f, 0.5935f)
        performPathCommand(path, 'L', 0.615f, 0.601f)
        performPathCommand(path, 'C', 0.607f, 0.607f, 0.601f, 0.6135f, 0.596f, 0.6195f)
        performPathCommand(path, 'C', 0.587f, 0.6305f, 0.58f, 0.641f, 0.58f, 0.65f)
        performPathCommand(path, 'L', 0.42f, 0.65f)
        performPathCommand(path, 'C', 0.42f, 0.629f, 0.432f, 0.61f, 0.449f, 0.5935f)
        performPathCommand(path, 'L', 0.449f, 0.5935f)
        performPathCommand(path, 'C', 0.455f, 0.588f, 0.462f, 0.583f, 0.469f, 0.578f)
        performPathCommand(path, 'C', 0.472f, 0.5755f, 0.475f, 0.5725f, 0.479f, 0.57f)
        performPathCommand(path, 'C', 0.49f, 0.563f, 0.502f, 0.556f, 0.513f, 0.55f)
        performPathCommand(path, 'L', 0.606f, 0.503f)
        performPathCommand(path, 'C', 0.633f, 0.4895f, 0.65f, 0.4705f, 0.65f, 0.45f)
        performPathCommand(path, 'C', 0.65f, 0.4085f, 0.583f, 0.375f, 0.5f, 0.375f)
        performPathCommand(path, 'C', 0.435f, 0.375f, 0.379f, 0.3955f, 0.359f, 0.4245f)
        performPathCommand(path, 'C', 0.348f, 0.44f, 0.32f, 0.45f, 0.288f, 0.45f)
        performPathCommand(path, 'C', 0.236f, 0.45f, 0.2f, 0.424f, 0.217f, 0.3995f)
        performPathCommand(path, 'C', 0.259f, 0.3415f, 0.369f, 0.3f, 0.5f, 0.3f)
        performPathCommand(path, 'C', 0.666f, 0.3f, 0.8f, 0.367f, 0.8f, 0.45f)
        performPathCommand(path, 'C', 0.8f, 0.483f, 0.773f, 0.513f, 0.73f, 0.5345f)
        path.close()
    }

    private fun performChargingIndicatorPath(path: Path) {
        path.reset()
        performPathCommand(path, 'M', 0.76f, 0.56f)
        performPathCommand(path, 'L', 0.49f, 0.81f)
        performPathCommand(path, 'C', 0.47f, 0.835f, 0.4f, 0.825f, 0.4f, 0.8f)
        performPathCommand(path, 'L', 0.4f, 0.625f)
        performPathCommand(path, 'L', 0.28f, 0.625f)
        performPathCommand(path, 'C', 0.24f, 0.625f, 0.22f, 0.605f, 0.24f, 0.59f)
        performPathCommand(path, 'L', 0.51f, 0.34f)
        performPathCommand(path, 'C', 0.53f, 0.315f, 0.6f, 0.325f, 0.6f, 0.35f)
        performPathCommand(path, 'L', 0.6f, 0.525f)
        performPathCommand(path, 'L', 0.72f, 0.525f)
        performPathCommand(path, 'C', 0.75f, 0.525f, 0.78f, 0.545f, 0.76f, 0.56f)
        path.close()
    }

    private fun performAlertIndicatorPath(path: Path) {
        path.reset()
        performPathCommand(path, 'M', 0.6f, 0.8f)
        performPathCommand(path, 'L', 0.4f, 0.8f)
        performPathCommand(path, 'L', 0.4f, 0.7f)
        performPathCommand(path, 'L', 0.6f, 0.7f)
        performPathCommand(path, 'L', 0.6f, 0.8f)
        path.close()
        performPathCommand(path, 'M', 0.6f, 0.55f)
        performPathCommand(path, 'C', 0.6f, 0.58f, 0.56f, 0.6f, 0.5f, 0.6f)
        performPathCommand(path, 'C', 0.44f, 0.6f, 0.4f, 0.58f, 0.4f, 0.55f)
        performPathCommand(path, 'L', 0.4f, 0.4f)
        performPathCommand(path, 'C', 0.4f, 0.37f, 0.44f, 0.35f, 0.5f, 0.35f)
        performPathCommand(path, 'C', 0.56f, 0.35f, 0.6f, 0.37f, 0.6f, 0.4f)
        performPathCommand(path, 'L', 0.6f, 0.55f)
        path.close()
    }

    private fun performPathCommand(path: Path, command: Char, vararg value: Float) {
        when (command) {
            'C' -> path.cubicTo(
                xRatioToCoordinate(value[0]),
                yRatioToCoordinate(value[1]),
                xRatioToCoordinate(value[2]),
                yRatioToCoordinate(value[3]),
                xRatioToCoordinate(value[4]),
                yRatioToCoordinate(value[5])
            )
            'L' -> path.lineTo(
                xRatioToCoordinate(value[0]),
                yRatioToCoordinate(value[1])
            )
            'M' -> path.moveTo(
                xRatioToCoordinate(value[0]),
                yRatioToCoordinate(value[1])
            )
        }
    }

    private fun xRatioToCoordinate(xRatio: Float) =
        shapeBounds.left + xRatio * shapeBounds.width()

    private fun yRatioToCoordinate(yRatio: Float) =
        shapeBounds.top + yRatio * shapeBounds.height()

    private fun updateChargeLevelClipRect() {
        val level = chargeLevel ?: MINIMUM_CHARGE_LEVEL
        chargeLevelClipRect.set(shapeBounds)
        chargeLevelClipRect.top +=
            chargeLevelClipRect.height() * (1f - level.toFloat() / MAXIMUM_CHARGE_LEVEL)
    }

    private fun updatePaintColors() {
        val currentLevel = chargeLevel
        val currentCriticalLevel = criticalChargeLevel

        chargeLevelPaint.color = color
        batteryPaint.color = colorWithAlpha(color, BATTERY_COLOR_ALPHA)

        if (currentLevel == null) {
            batteryPaint.color = unknownColor ?: color
        } else if (isCharging) {
            chargingColor?.let {
                chargeLevelPaint.color = it
                batteryPaint.color = colorWithAlpha(it, BATTERY_COLOR_ALPHA)
            }
        } else if (currentCriticalLevel != null && currentLevel <= currentCriticalLevel) {
            criticalColor?.let {
                chargeLevelPaint.color = it
                batteryPaint.color = colorWithAlpha(it, BATTERY_COLOR_ALPHA)
            }
        }
    }

    companion object {
        const val MINIMUM_CHARGE_LEVEL = 0
        const val MAXIMUM_CHARGE_LEVEL = 100

        const val BATTERY_COLOR_ALPHA = 0.3f
        const val CRITICAL_CHARGE_LEVEL = 10

        private fun colorWithAlpha(value: Int, alpha: Float): Int {
            require (alpha >= 0f || alpha <= 1f) {
                "alpha must be between 0 and 1."
            }

            val alphaComponent = (alpha * Color.alpha(value)).roundToInt()
            return (value and 0x00FFFFFF) or (alphaComponent shl 24)
        }

        private fun getColorAttr(context: Context, attr: Int): Int {
            val typedArray = context.obtainStyledAttributes(intArrayOf(attr))
            val color = typedArray.getColor(0, 0)
            typedArray.recycle()

            return color
        }
    }
}