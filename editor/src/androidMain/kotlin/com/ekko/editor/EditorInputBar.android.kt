package com.ekko.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

internal class AndroidEditorInputController : EditorInputController {
    internal var view: DoubaoInputBar? = null

    override fun setText(value: String) {
        view?.setText(value)
    }

    override fun clear() {
        view?.clear()
    }

    override fun insertChip(label: String, data: Any?, icon: ChipIconType?) {
        val drawable = when (icon) {
            ChipIconType.Document -> view?.context?.getDrawable(R.drawable.ic_chip_doc)
            null -> null
        }
        view?.insertChip(label, data, drawable)
    }

    override fun requestFocus() {
        view?.requestInputFocus()
    }
}

@Composable
internal actual fun rememberEditorInputControllerImpl(): EditorInputController {
    return remember { AndroidEditorInputController() }
}

@Composable
internal actual fun EditorInputBarImpl(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: (String) -> Unit,
    modifier: Modifier,
    placeholder: String,
    minLines: Int,
    maxLines: Int,
    onChipRemoved: (Any?) -> Unit,
    controller: EditorInputController?,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            DoubaoInputBar(context).apply {
                setPlaceholder(placeholder)
                setLineLimits(minLines, maxLines)
                this.onTextChanged = onTextChange
                this.onSend = onSend
                this.onChipRemoved = onChipRemoved
            }
        },
        update = { view ->
            view.onTextChanged = onTextChange
            view.onSend = onSend
            view.onChipRemoved = onChipRemoved
            view.setPlaceholder(placeholder)
            view.setLineLimits(minLines, maxLines)
            (controller as? AndroidEditorInputController)?.view = view

            // 延后到主线程下一帧再写，避免在 Compose 应用阶段同步触发 TextWatcher 回写状态。
            if (view.text != text) {
                view.post {
                    if (view.text != text) {
                        view.setText(text)
                    }
                }
            }
        },
    )
}
