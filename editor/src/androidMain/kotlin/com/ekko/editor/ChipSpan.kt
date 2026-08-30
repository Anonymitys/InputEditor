package com.ekko.editor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.Layout
import android.text.TextPaint
import android.text.style.ReplacementSpan
import android.util.TypedValue
import java.text.BreakIterator
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 输入框中的富文本块：左侧 icon、中间文字、右侧关闭按钮，圆角背景。
 * 它是一个不可拆分的整体，长度放不下时整块换行。
 */
class ChipSpan(
    val label: CharSequence,
    val data: Any?,
    icon: Drawable?,
    context: Context,
) : ReplacementSpan() {

    companion object {
        /** chip 文字最多显示的字符数，超出部分用省略号代替。 */
        private const val MAX_LABEL_CHARS = 5
        private const val ELLIPSIS = "…"
    }

    /** 实际绘制在 chip 中间的文字；[label] 本身保留完整内容。 */
    private val displayLabel: CharSequence = buildDisplayLabel(label)

    private val icon: Drawable? = icon?.mutate()
    private val density = context.resources.displayMetrics.density

    private fun dp(value: Float): Float = value * density

    private val chipHeight = dp(26f)
    private val cornerRadius = chipHeight / 2f
    private val paddingStart = dp(8f)
    private val iconSize = dp(16f)
    private val iconTextGap = dp(6f)
    private val textCloseGap = dp(4f)
    private val closeSize = dp(16f)
    private val paddingEnd = dp(6f)

    private val backgroundColor = 0xFFE8ECFF.toInt()
    private val contentColor = 0xFF4D6BFE.toInt()

    private val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = contentColor
        typeface = Typeface.DEFAULT
        textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            14f,
            context.resources.displayMetrics,
        )
    }
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = backgroundColor }
    private val closeCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ((0.18f * 255).toInt() shl 24) or (contentColor and 0x00FFFFFF)
    }
    private val closeStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = contentColor
        style = Paint.Style.STROKE
        strokeWidth = dp(1.6f)
        strokeCap = Paint.Cap.ROUND
    }

    /** 文字的光学中心相对 baseline 的偏移，用于让 chip 内文字看起来居中。 */
    private val labelCenterOffset = (labelPaint.ascent() + labelPaint.descent()) / 2f

    private var measuredWidth = -1f
    private val closeRect = RectF()

    private fun buildDisplayLabel(source: CharSequence): CharSequence {
        val text = source.toString()
        // 按字形集群边界截断，避免把 emoji 的 ZWJ 连字/肤色修饰符等从中间切开
        val iterator = BreakIterator.getCharacterInstance().apply { setText(text) }
        var count = 0
        var boundary = iterator.first()
        while (boundary != BreakIterator.DONE) {
            val next = iterator.next()
            if (next == BreakIterator.DONE) break
            count++
            boundary = next
            if (count == MAX_LABEL_CHARS) {
                return text.substring(0, boundary) + ELLIPSIS
            }
        }
        return source
    }

    private fun computeWidth(): Float {
        val labelWidth = labelPaint.measureText(displayLabel, 0, displayLabel.length)
        val iconBlock = if (icon != null) iconSize + iconTextGap else 0f
        return paddingStart + iconBlock + labelWidth +
            textCloseGap + closeSize + paddingEnd
    }

    private fun currentWidth(): Float {
        if (measuredWidth < 0f) {
            measuredWidth = computeWidth()
        }
        return measuredWidth
    }

    override fun getSize(
        paint: Paint,
        text: CharSequence?,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?,
    ): Int {
        val width = currentWidth()
        if (fm != null) {
            // 把所在行的行高撑到至少一个 chip 的高度，chip 以 baseline 为中心。
            val half = (chipHeight / 2f).roundToInt()
            fm.ascent = min(fm.ascent, -half)
            fm.descent = max(fm.descent, half)
            fm.top = min(fm.top, fm.ascent)
            fm.bottom = max(fm.bottom, fm.descent)
        }
        return width.roundToInt()
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence?,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint,
    ) {
        val width = currentWidth()
        val left = x
        val right = x + width
        val centerY = y.toFloat()

        canvas.drawRoundRect(
            left,
            centerY - chipHeight / 2f,
            right,
            centerY + chipHeight / 2f,
            cornerRadius,
            cornerRadius,
            backgroundPaint,
        )

        icon?.let { drawable ->
            val iconLeft = left + paddingStart
            val iconTop = centerY - iconSize / 2f
            drawable.setBounds(
                iconLeft.roundToInt(),
                iconTop.roundToInt(),
                (iconLeft + iconSize).roundToInt(),
                (iconTop + iconSize).roundToInt(),
            )
            drawable.setTint(contentColor)
            drawable.draw(canvas)
        }

        val labelLeft = left + paddingStart + if (icon != null) iconSize + iconTextGap else 0f
        canvas.drawText(
            displayLabel,
            0,
            displayLabel.length,
            labelLeft,
            centerY - labelCenterOffset,
            labelPaint,
        )

        val closeCenterX = right - paddingEnd - closeSize / 2f
        canvas.drawCircle(closeCenterX, centerY, closeSize / 2f, closeCirclePaint)
        val arm = closeSize * 0.42f
        canvas.drawLine(
            closeCenterX - arm,
            centerY - arm,
            closeCenterX + arm,
            centerY + arm,
            closeStrokePaint,
        )
        canvas.drawLine(
            closeCenterX - arm,
            centerY + arm,
            closeCenterX + arm,
            centerY - arm,
            closeStrokePaint,
        )
    }

    /** 根据布局计算关闭按钮的可点击区域（比视觉区域略大）。 */
    fun closeTouchRect(layout: Layout, spanStart: Int): RectF {
        val line = layout.getLineForOffset(spanStart)
        val centerY = layout.getLineBaseline(line).toFloat()
        val spanLeft = layout.getPrimaryHorizontal(spanStart)
        val closeCenterX = spanLeft + currentWidth() - paddingEnd - closeSize / 2f
        val touchHalf = closeSize * 0.7f
        closeRect.set(
            closeCenterX - touchHalf,
            centerY - touchHalf,
            closeCenterX + touchHalf,
            centerY + touchHalf,
        )
        return closeRect
    }
}
