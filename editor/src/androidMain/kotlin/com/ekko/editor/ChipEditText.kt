package com.ekko.editor

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.Selection
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.method.ArrowKeyMovementMethod
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.EditText
import android.widget.TextView

/**
 * 支持 [ChipSpan] 的输入框：点击 chip 右侧的关闭按钮会删除整个 chip。
 */
class ChipEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.editTextStyle,
) : EditText(context, attrs, defStyleAttr) {

    /** 删除 chip 时的回调，参数为插入时传入的 [ChipSpan.data]。 */
    var onChipRemoved: ((Any?) -> Unit)? = null

    init {
        movementMethod = ChipMovementMethod.instance
    }

    /**
     * 在光标处插入一个富文本块；如果当前有选区，则先替换选区。
     *
     * 富文本块以 [ChipSpan] 的形式直接存在 [text]（Editable）中，
     * 是文本内容的一部分，会跟随文本一起换行、滚动、选中和删除。
     *
     * @param label 显示在中间的文字
     * @param data 自定义数据，删除时会通过 [onChipRemoved] 传回
     * @param icon 左侧图标，可选
     */
    fun insertChip(label: String, data: Any? = null, icon: Drawable? = null) {
        val editable = text ?: return
        val selStart = selectionStart.coerceIn(0, editable.length)
        val selEnd = selectionEnd.coerceIn(0, editable.length)

        val chip = SpannableStringBuilder()
        if (selStart > 0 &&
            editable[selStart - 1] != '\n' &&
            editable[selStart - 1] != ' '
        ) {
            chip.append(' ')
        }
        chip.append('\uFFFC')
        chip.setSpan(
            ChipSpan(label, data, icon, context),
            chip.length - 1,
            chip.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        chip.append(' ')

        editable.replace(selStart, selEnd, chip)
        setSelection(selStart + chip.length)
        requestFocus()
    }

    /** 把富文本块还原成 [label] 形式的可读文本，普通文本保持原样。 */
    fun readableText(): String {
        val editable = text ?: return ""
        return buildString {
            var cursor = 0
            val spans = editable.getSpans(0, editable.length, ChipSpan::class.java)
                .sortedBy { editable.getSpanStart(it) }
            for (span in spans) {
                val start = editable.getSpanStart(span)
                val end = editable.getSpanEnd(span)
                append(editable.subSequence(cursor, start))
                append("[${span.label}]")
                cursor = end
            }
            append(editable.subSequence(cursor, editable.length))
        }
    }

    private class ChipMovementMethod private constructor() : ArrowKeyMovementMethod() {

        override fun onTouchEvent(
            widget: TextView,
            buffer: Spannable,
            event: MotionEvent,
        ): Boolean {
            if (event.actionMasked == MotionEvent.ACTION_DOWN ||
                event.actionMasked == MotionEvent.ACTION_UP
            ) {
                val span = findCloseHit(widget, buffer, event.x, event.y)
                if (span != null) {
                    if (event.actionMasked == MotionEvent.ACTION_UP) {
                        removeChip(widget, buffer, span)
                    }
                    return true
                }
            }
            return super.onTouchEvent(widget, buffer, event)
        }

        private fun findCloseHit(
            widget: TextView,
            buffer: Spannable,
            x: Float,
            y: Float,
        ): ChipSpan? {
            val layout = widget.layout ?: return null
            // 触摸事件是视图坐标，而 Layout 的方法是布局坐标：
            // 需要去掉 padding 并补回滚动偏移，滚动状态下才能正确命中。
            val localX = x - widget.totalPaddingLeft + widget.scrollX
            val localY = y - widget.totalPaddingTop + widget.scrollY
            val line = layout.getLineForVertical(localY.toInt())
            val spans = buffer.getSpans(
                layout.getLineStart(line),
                layout.getLineEnd(line),
                ChipSpan::class.java,
            )
            return spans.firstOrNull { span ->
                val start = buffer.getSpanStart(span)
                span.closeTouchRect(layout, start).contains(localX, localY)
            }
        }

        private fun removeChip(widget: TextView, buffer: Spannable, span: ChipSpan) {
            val start = buffer.getSpanStart(span)
            val end = buffer.getSpanEnd(span)
            val data = span.data
            val editable = buffer as Editable
            editable.delete(start, end)
            Selection.setSelection(editable, start)
            (widget as? ChipEditText)?.onChipRemoved?.invoke(data)
        }

        companion object {
            val instance: ChipMovementMethod by lazy { ChipMovementMethod() }
        }
    }
}
