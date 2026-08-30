package com.ekko.inputeditor

import com.ekko.editor.EditorInputBar
import com.ekko.editor.ChipIconType
import com.ekko.editor.rememberEditorInputController
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val ScreenBackground = Color(0xFFF7F7F8)
private val BubbleMineBackground = Color(0xFF4D6BFE)
private val BubbleOtherBackground = Color.White
private val BubbleMineText = Color.White
private val BubbleOtherText = Color(0xFF1A1A1A)

private data class ChatMessage(
    val content: String,
    val isMine: Boolean,
)

@Composable
@Preview
fun App() {
    MaterialTheme {
        var text by remember { mutableStateOf("") }
        var messages by remember {
            mutableStateOf(
                listOf(
                    ChatMessage(content = "你好！我是豆包，有什么可以帮你？", isMine = false),
                ),
            )
        }
        val listState = rememberLazyListState()
        val scope = rememberCoroutineScope()
        val inputController = rememberEditorInputController()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ScreenBackground)
                .safeDrawingPadding(),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(messages) { message ->
                    MessageBubble(message)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                TextButton(
                    onClick = {
                        inputController.insertChip(
                            "产品需求文档.pdf",
                            "doc",
                            ChipIconType.Document,
                        )
                    },
                ) {
                    Text("插入文档")
                }
            }

            EditorInputBar(
                text = text,
                onTextChange = { text = it },
                onSend = { message ->
                    messages = messages + ChatMessage(content = message, isMine = true)
                    scope.launch {
                        listState.animateScrollToItem(messages.size - 1)
                    }
                },
                controller = inputController,
            )
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start,
    ) {
        Text(
            text = message.content,
            color = if (message.isMine) BubbleMineText else BubbleOtherText,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    color = if (message.isMine) BubbleMineBackground else BubbleOtherBackground,
                    shape = RoundedCornerShape(18.dp),
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}
