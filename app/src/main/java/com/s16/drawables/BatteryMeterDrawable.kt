package com.s16.drawables

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Build
import com.s16.smluncher.R
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import kotlin.math.roundToInt

interface BatteryMeter {
    enum class Theme {
        ROUNDED,
        SHARP
    }

    var theme: Theme
    var chargeLevel: Int?
    var criticalChargeLevel: Int?
    var isCharging: Boolean

    // Colors
    var color: Int
    var indicatorColor: Int
    var chargingColor: Int?
    var criticalColor: Int?
    var unknownColor: Int?
}

/**
 * Wrap the specified [block] in calls to [Canvas.save]
 * and [Canvas.restoreToCount].
 */
inline fun Canvas.withSave(block: Canvas.() -> Unit) {
    val checkpoint = save()
    try {
        block()
    } finally {
        restoreToCount(checkpoint)
    }
}

internal fun Canvas.clipOutPathCompat(path: Path) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        clipOutPath(path)
    } else {
        @Suppress("DEPRECATION")
        clipPath(path, Region.Op.DIFFERENCE)
    }
}

internal fun Context.getColorAttr(attr: Int): Int {
    val typedArray = obtainStyledAttributes(intArrayOf(attr))
    val color = typedArray.getColor(0, 0)
    typedArray.recycle()

    return color
}

internal fun Int.colorWithAlpha(alpha: Float): Int {
    require (alpha >= 0f || alpha <= 1f) {
        "alpha must be between 0 and 1."
    }

    val alphaComponent = (alpha * Color.alpha(this)).roundToInt()
    return (this and 0x00FFFFFF) or (alphaComponent shl 24)
}

class BatteryMeterDrawable @JvmOverloads constructor(
    private val context: Context,
    theme: BatteryMeter.Theme = BatteryMeter.Theme.SHARP
) : Drawable(), BatteryMeter {

    companion object {
        const val MINIMUM_CHARGE_LEVEL = 0
        const val MAXIMUM_CHARGE_LEVEL = 100

        const val BATTERY_COLOR_ALPHA = 0.3f
        const val CRITICAL_CHARGE_LEVEL = 10
    }

    private val padding = Rect()

    private val batteryShapeBounds = Rect()
    private val batteryPath = Path()
    private val indicatorPath = Path()
    private val chargeLevelClipRect = RectF()

    private val intrinsicSize =
        context.resources.getDimensionPixelSize(R.dimen.battery_meter_intrinsic_size)

    private var aspectRatio: Float = 1f
        set(value) {
            if (field != value) {
                field = value
                updateBatteryShapeBounds()
            }
        }

    private lateinit var batteryShapeDataStream: DataInputStream
    private lateinit var alertIndicatorDataStream: DataInputStream
    private lateinit var chargingIndicatorDataStream: DataInputStream
    private lateinit var unknownIndicatorDataStream: DataInputStream

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

    override var theme = theme
        set(value) {
            if (field != value) {
                field = value
                loadThemeShapes()
                updateBatteryPath()
                updateIndicatorPath()
                invalidateSelf()
            }
        }

    override var chargeLevel: Int? = null
        set(value) {
            val newChargeLevel = value?.coerceIn(MINIMUM_CHARGE_LEVEL, MAXIMUM_CHARGE_LEVEL)
            if (field != newChargeLevel) {
                field = newChargeLevel
                updateIndicatorPath()
                updateChargeLevelClipRect()
                updatePaintColors()
                invalidateSelf()
            }
        }

    override var isCharging: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                updateIndicatorPath()
                updatePaintColors()
                invalidateSelf()
            }
        }

    override var criticalChargeLevel: Int? = CRITICAL_CHARGE_LEVEL
        set(value) {
            val newCriticalChargeLevel = value?.coerceIn(MINIMUM_CHARGE_LEVEL, MAXIMUM_CHARGE_LEVEL)
            if (field != newCriticalChargeLevel) {
                field = newCriticalChargeLevel
                updateIndicatorPath()
                updatePaintColors()
                invalidateSelf()
            }
        }

    override var color: Int = context.getColorAttr(android.R.attr.colorForeground)
        set(value) {
            if (field != value) {
                field = value
                updatePaintColors()
                invalidateSelf()
            }
        }

    override var chargingColor: Int? = null
        set(value) {
            if (field != value) {
                field = value
                updatePaintColors()
                invalidateSelf()
            }
        }

    override var criticalColor: Int? = null
        set(value) {
            if (field != value) {
                field = value
                updatePaintColors()
                invalidateSelf()
            }
        }

    override var unknownColor: Int? = null
        set(value) {
            if (field != value) {
                field = value
                updatePaintColors()
                invalidateSelf()
            }
        }

    override var indicatorColor: Int
        get() = indicatorPaint.color
        set(value) {
            indicatorPaint.color = value
            invalidateSelf()
        }

    init {
        loadThemeShapes()
    }

    override fun getIntrinsicWidth() = if (aspectRatio < 1) {
        (intrinsicSize * aspectRatio).toInt()
    } else {
        intrinsicSize
    } + padding.left + padding.right

    override fun getIntrinsicHeight() = if (aspectRatio < 1) {
        intrinsicSize
    } else {
        (intrinsicSize / aspectRatio).toInt()
    } + padding.top + padding.bottom

    override fun getPadding(padding: Rect): Boolean {
        if (padding.left == 0 && padding.top == 0 && padding.right == 0 && padding.bottom == 0) {
            return super.getPadding(padding)
        }

        padding.set(this.padding)
        return true
    }

    fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        padding.set(left, top, right, bottom)
        updateBatteryShapeBounds()
    }

    override fun onBoundsChange(bounds: Rect) {
        updateBatteryShapeBounds()
    }

    override fun draw(canvas: Canvas) {
        canvas.withSave {
            if (!indicatorPath.isEmpty) {
                drawPath(indicatorPath, indicatorPaint)
                clipOutPathCompat(indicatorPath)
            }

            drawPath(batteryPath, batteryPaint)

            if (!chargeLevelClipRect.isEmpty) {
                clipRect(chargeLevelClipRect)
                drawPath(batteryPath, chargeLevelPaint)
            }
        }
    }

    override fun setAlpha(alpha: Int) {
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        batteryPaint.colorFilter = colorFilter
        chargeLevelPaint.colorFilter = colorFilter
        indicatorPaint.colorFilter = colorFilter
    }

    private fun updateBatteryShapeBounds() {
        if (bounds.isEmpty) return

        val availableWidth = bounds.width() - padding.left - padding.right
        val availableHeight = bounds.height() - padding.top - padding.bottom
        val availableAspectRatio = availableWidth.toFloat() / availableHeight

        if (availableAspectRatio > aspectRatio) {
            batteryShapeBounds.set(0, 0, (availableHeight * aspectRatio).toInt(), availableHeight)
        } else {
            batteryShapeBounds.set(0, 0, availableWidth, (availableWidth / aspectRatio).toInt())
        }

        batteryShapeBounds.offset(
            padding.left + (availableWidth - batteryShapeBounds.width()) / 2,
            padding.top + (availableHeight - batteryShapeBounds.height()) / 2
        )

        updateBatteryPath()
        updateIndicatorPath()
        updateChargeLevelClipRect()
    }

    private fun updateBatteryPath() {
        performPathCommands(batteryShapeDataStream, batteryPath)
    }

    private fun updateIndicatorPath() {
        val currentLevel = chargeLevel
        val currentCriticalLevel = criticalChargeLevel

        if (currentLevel == null) {
            performPathCommands(unknownIndicatorDataStream, indicatorPath)
        } else if (isCharging) {
            performPathCommands(chargingIndicatorDataStream, indicatorPath)
        } else if (currentCriticalLevel != null && currentLevel <= currentCriticalLevel) {
            performPathCommands(alertIndicatorDataStream, indicatorPath)
        } else {
            indicatorPath.reset()
        }
    }

    private fun updateChargeLevelClipRect() {
        val level = chargeLevel ?: MINIMUM_CHARGE_LEVEL
        chargeLevelClipRect.set(batteryShapeBounds)
        chargeLevelClipRect.top +=
            chargeLevelClipRect.height() * (1f - level.toFloat() / MAXIMUM_CHARGE_LEVEL)
    }

    private fun updatePaintColors() {
        val currentLevel = chargeLevel
        val currentCriticalLevel = criticalChargeLevel

        chargeLevelPaint.color = color
        batteryPaint.color = color.colorWithAlpha(BATTERY_COLOR_ALPHA)

        if (currentLevel == null) {
            batteryPaint.color = unknownColor ?: color
        } else if (isCharging) {
            chargingColor?.let {
                chargeLevelPaint.color = it
                batteryPaint.color = it.colorWithAlpha(BATTERY_COLOR_ALPHA)
            }
        } else if (currentCriticalLevel != null && currentLevel <= currentCriticalLevel) {
            criticalColor?.let {
                chargeLevelPaint.color = it
                batteryPaint.color = it.colorWithAlpha(BATTERY_COLOR_ALPHA)
            }
        }
    }

    private fun loadThemeShapes() {
        val rawResourceId = when (theme) {
            BatteryMeter.Theme.SHARP -> R.raw.battery_shapes_sharp
            BatteryMeter.Theme.ROUNDED -> R.raw.battery_shapes_rounded
        }

        val rawBytes = context.resources.openRawResource(rawResourceId).readBytes()
        DataInputStream(ByteArrayInputStream(rawBytes)).use { input ->
            val newAspectRatio = input.readFloat()

            batteryShapeDataStream = createShapeDataInputStream(input, rawBytes)
            alertIndicatorDataStream = createShapeDataInputStream(input, rawBytes)
            chargingIndicatorDataStream = createShapeDataInputStream(input, rawBytes)
            unknownIndicatorDataStream = createShapeDataInputStream(input, rawBytes)

            aspectRatio = newAspectRatio
        }
    }

    private fun createShapeDataInputStream(
        input: DataInputStream,
        rawBytes: ByteArray
    ): DataInputStream {
        val shapeBytesCount = input.readInt()
        val shapeBytesOffset = rawBytes.size - input.available()
        val shapeDataInputStream = DataInputStream(
            ByteArrayInputStream(rawBytes, shapeBytesOffset, shapeBytesCount)
        )
        input.skipBytes(shapeBytesCount)

        return shapeDataInputStream
    }

    private fun performPathCommands(pathDataStream: DataInputStream, path: Path) {
        path.reset()
        pathDataStream.reset()

        while (pathDataStream.available() > 0) {
            val command = pathDataStream.readChar()

            when (command) {
                'C' -> path.cubicTo(
                    xRatioToCoordinate(pathDataStream.readFloat()),
                    yRatioToCoordinate(pathDataStream.readFloat()),
                    xRatioToCoordinate(pathDataStream.readFloat()),
                    yRatioToCoordinate(pathDataStream.readFloat()),
                    xRatioToCoordinate(pathDataStream.readFloat()),
                    yRatioToCoordinate(pathDataStream.readFloat())
                )
                'L' -> path.lineTo(
                    xRatioToCoordinate(pathDataStream.readFloat()),
                    yRatioToCoordinate(pathDataStream.readFloat())
                )
                'M' -> path.moveTo(
                    xRatioToCoordinate(pathDataStream.readFloat()),
                    yRatioToCoordinate(pathDataStream.readFloat())
                )
                'Z' -> path.close()
            }
        }
    }

    private fun xRatioToCoordinate(xRatio: Float) =
        batteryShapeBounds.left + xRatio * batteryShapeBounds.width()

    private fun yRatioToCoordinate(yRatio: Float) =
        batteryShapeBounds.top + yRatio * batteryShapeBounds.height()
}