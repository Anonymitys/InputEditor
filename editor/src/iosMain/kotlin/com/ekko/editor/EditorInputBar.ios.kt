package com.ekko.editor

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/** 命令式控制入口的 iOS 实现，持有 [IosNativeInputBar] 并转发调用。 */
internal class IosInputBarController : EditorInputController {
    internal var view: IosNativeInputBar? = null

    override fun setText(value: String) {
        view?.setPlainText(value)
    }

    override fun clear() {
        view?.setPlainText("")
    }

    override fun insertChip(label: String, data: Any?, icon: ChipIconType?) {
        view?.insertChip(label, data, icon)
    }

    override fun requestFocus() {
        view?.requestInputFocus()
    }
}

@Composable
internal actual fun rememberEditorInputControllerImpl(): EditorInputController {
    return remember { IosInputBarController() }
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
    var barHeight by remember { mutableStateOf(IosBarMetrics.initialBarHeight.dp) }

    UIKitView(
        factory = {
            IosNativeInputBar().apply {
                onHeightChanged = { newHeight ->
                    barHeight = newHeight.dp
                }
            }
        },
        modifier = modifier.height(barHeight),
        // 默认 Cooperative 模式会先让 Compose 拦截触摸，长按等连续手势
        // 到不了 UITextView，文本无法选中；这里交给原生视图直接处理。
        properties = UIKitInteropProperties(
            interactionMode = UIKitInteropInteractionMode.NonCooperative,
        ),
        update = { view ->
            view.onTextChanged = onTextChange
            view.onSend = onSend
            view.onChipRemoved = onChipRemoved
            view.placeholder = placeholder
            view.minLines = minLines
            view.maxLines = maxLines
            (controller as? IosInputBarController)?.view = view
            syncPlainText(view, text)
        },
    )
}

/**
 * 把 Compose 侧的纯文本状态同步进原生输入框。
 *
 * 与 Android 端一致：先比较、延后到主线程下一轮再写，
 * 并且写入前再比较一次——如果等待期间用户又输入了新内容，
 * 就放弃这次覆盖，保留用户输入。
 */
private fun syncPlainText(view: IosNativeInputBar, target: String) {
    if (view.readableText() == target) return
    dispatch_async(dispatch_get_main_queue()) {
        if (view.readableText() != target) {
            view.setPlainText(target)
        }
    }
}
