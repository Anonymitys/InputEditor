package com.ekko.editor

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** chip 左侧可选的图标类型，各平台用本地资源/绘制实现。 */
enum class ChipIconType {
    /** 文档图标（Material insert_drive_file）。 */
    Document,
}

/**
 * 对平台原生输入框的命令式控制入口。
 *
 * Android / iOS 各有自己的本地实现：
 * - Android 使用原生 View（EditText + ChipSpan）；
 * - iOS 使用原生 UIKit（UITextView + NSTextAttachment）。
 */
interface EditorInputController {
    /** 设置为纯文本内容，光标移到末尾。 */
    fun setText(value: String)

    /** 清空内容。 */
    fun clear()

    /**
     * 在光标处插入一个富文本块（chip），删除时通过 [EditorInputBar] 的 onChipRemoved 传回 data。
     *
     * @param icon 左侧图标，可选。
     */
    fun insertChip(label: String, data: Any? = null, icon: ChipIconType? = null)

    /** 请求输入焦点并弹出键盘。 */
    fun requestFocus()
}

@Composable
fun rememberEditorInputController(): EditorInputController = rememberEditorInputControllerImpl()

@Composable
internal expect fun rememberEditorInputControllerImpl(): EditorInputController

/**
 * 类似豆包的底部输入栏，Android / iOS 均使用各自平台的原生输入框实现：
 * - 默认 [minLines] 行，随内容自动长高，最多 [maxLines] 行；
 * - 超过 [maxLines] 后，在输入框内部滚动；
 * - 有文字时发送按钮可用，点击后通过 [onSend] 返回去掉首尾空白后的文本并清空输入框。
 *
 * @param text 当前文本；chip 会被展开成 `[label]` 形式，由平台实现回传给 [onTextChange]。
 * @param onTextChange 文本变化回调，返回可读文本（chip 展开为 `[label]`）。
 * @param onSend 发送回调，参数为去掉首尾空白后的文本；输入框随后自动清空。
 * @param onChipRemoved chip 被删除时的回调，参数为插入时传入的 data。
 * @param controller 可选，用于命令式控制输入框（setText/clear/insertChip/requestFocus）。
 */
@Composable
fun EditorInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    placeholder: String = "发消息或输入 / 提问",
    minLines: Int = 1,
    maxLines: Int = 8,
    onChipRemoved: (Any?) -> Unit = {},
    controller: EditorInputController? = null,
) {
    EditorInputBarImpl(
        text = text,
        onTextChange = onTextChange,
        onSend = onSend,
        modifier = modifier,
        placeholder = placeholder,
        minLines = minLines,
        maxLines = maxLines,
        onChipRemoved = onChipRemoved,
        controller = controller,
    )
}

@Composable
internal expect fun EditorInputBarImpl(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: (String) -> Unit,
    modifier: Modifier,
    placeholder: String,
    minLines: Int,
    maxLines: Int,
    onChipRemoved: (Any?) -> Unit,
    controller: EditorInputController?,
)
