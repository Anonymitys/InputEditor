package com.ekko.editor

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.LinearLayout

/**
 * 类似豆包的输入框（Android 原生 View 实现）：
 * 默认单行，最多 [setLineLimits] 指定的行数，内容超过最大行数后在输入框内部滚动。
 * 支持通过 [insertChip] 插入富文本块。
 */
class DoubaoInputBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private val inputEditText: ChipEditText
    private val sendButton: ImageButton

    /** 点击发送按钮时的回调，参数为去掉首尾空白后的文本。 */
    var onSend: ((String) -> Unit)? = null

    /** 富文本块被删除时的回调，参数为插入时传入的 data。 */
    var onChipRemoved: ((Any?) -> Unit)? = null

    /** 文本变化时的回调，参数为可读文本（chip 展开为 `[label]`）。 */
    var onTextChanged: ((String) -> Unit)? = null

    /** 原始文本；富文本块会被替换成 [label] 以便阅读和发送。 */
    val text: String
        get() = inputEditText.readableText()

    init {
        orientation = HORIZONTAL
        gravity = Gravity.BOTTOM
        setAddStatesFromChildren(true)
        LayoutInflater.from(context).inflate(R.layout.view_editor_input_bar, this, true)

        inputEditText = findViewById(R.id.editorInput)
        sendButton = findViewById(R.id.editorSendButton)

        inputEditText.onChipRemoved = { data -> onChipRemoved?.invoke(data) }

        sendButton.setOnClickListener {
            val message = text.trim()
            if (message.isNotEmpty()) {
                onSend?.invoke(message)
                inputEditText.setText("")
                inputEditText.requestFocus()
            }
        }

        inputEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                sendButton.isEnabled = !s.isNullOrBlank()
                onTextChanged?.invoke(inputEditText.readableText())
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })
        sendButton.isEnabled = false
    }

    fun setText(value: CharSequence) {
        inputEditText.setText(value)
        inputEditText.setSelection(inputEditText.text?.length ?: 0)
    }

    fun clear() {
        inputEditText.setText("")
    }

    fun setPlaceholder(hint: CharSequence) {
        inputEditText.hint = hint
    }

    fun setLineLimits(min: Int, max: Int) {
        val minLines = min.coerceAtLeast(1)
        inputEditText.setMinLines(minLines)
        inputEditText.setMaxLines(max.coerceAtLeast(minLines))
    }

    /**
     * 在光标处插入一个富文本块；如果当前有选区，则先替换选区。
     *
     * @param label 显示在中间的文字
     * @param data 自定义数据，删除时会通过 [onChipRemoved] 传回
     * @param icon 左侧图标，可选
     */
    fun insertChip(label: String, data: Any? = null, icon: Drawable? = null) {
        inputEditText.insertChip(label, data, icon)
    }

    fun requestInputFocus() {
        inputEditText.requestFocus()
    }
}
