package com.smallkang.ankirsi

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** One deck: its cards, and the button that starts today's session. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckScreen(library: Library, deckId: Long, study: () -> Unit, back: () -> Unit) {
    val deck = library.deck(deckId) ?: return
    var editing by rememberSaveable { mutableStateOf<Long?>(null) } // card id, 0 for a new card
    var renaming by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    val today = studyDay()
    val due = deck.queue(today).size
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            PageBar(deck.name, scroll, navigationIcon = { BackButton(back) }) {
                IconAction(R.drawable.ic_add, "添加卡片") { editing = 0 }
                OverflowMenu("重命名" to { renaming = true }, "删除卡组" to { deleting = true })
            }
        },
        bottomBar = {
            if (deck.cards.isNotEmpty()) {
                Box(
                    Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).navigationBarsPadding(),
                    contentAlignment = Alignment.Center,
                ) {
                    PrimaryButton(
                        if (due > 0) "开始学习 · $due" else "今天学完了",
                        study,
                        Modifier.readingWidth().padding(horizontal = Margin, vertical = 12.dp),
                        enabled = due > 0,
                    )
                }
            }
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize(), contentPadding = padding, horizontalAlignment = Alignment.CenterHorizontally) {
            item { Note(if (deck.cards.isEmpty()) "还没有卡片。点右上角 + 添加，或在首页导入文件。" else "共 ${deck.cards.size} 张") }
            itemsIndexed(deck.cards, key = { _, card -> card.id }) { i, card ->
                if (i > 0) Hairline()
                CardRow(card, today) { editing = card.id }
            }
        }
    }

    editing?.let { id ->
        CardEditor(
            card = deck.cards.find { it.id == id } ?: Card(0, ""),
            onSave = { library.saveCard(deckId, it) },
            onDelete = { library.deleteCard(deckId, id) },
            onDismiss = { editing = null },
        )
    }
    if (renaming) NameDialog("重命名卡组", deck.name, { renaming = false }) { library.renameDeck(deckId, it) }
    if (deleting) {
        AlertDialog(
            onDismissRequest = { deleting = false },
            title = { Text("删除「${deck.name}」？") },
            text = { Text("${deck.cards.size} 张卡片和学习进度会一起删除。") },
            confirmButton = {
                TextButton({ library.deleteDeck(deckId); back() }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton({ deleting = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun CardRow(card: Card, today: Long, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.readingWidth().clickable(onClick = onClick).padding(horizontal = Margin, vertical = 14.dp)) {
        Column(Modifier.weight(1f)) {
            Text(clozed(card.front, true, colors.tertiary), maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (card.back.isNotEmpty()) {
                Text(
                    card.back,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Text(
            when {
                card.isNew -> "新"
                card.due <= today -> "今天"
                else -> "${span(card.due - today)}后"
            },
            Modifier.padding(start = 16.dp, top = 2.dp),
            color = colors.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

/** Adds cards one after another, or edits and deletes an existing one. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardEditor(card: Card, onSave: (Card) -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit) {
    val front = rememberTextFieldState(card.front)
    val back = rememberTextFieldState(card.back)
    val focus = remember { FocusRequester() }
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val isNew = card.id == 0L
    fun close(then: () -> Unit = {}) = scope.launch { sheet.hide() }.invokeOnCompletion { then(); onDismiss() }

    ModalBottomSheet(onDismiss, sheetState = sheet) {
        Column(
            Modifier.padding(horizontal = Margin).padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                front,
                Modifier.fillMaxWidth().focusRequester(focus),
                label = { Text("正面") },
                placeholder = { Text("问题，或带 {{挖空}} 的句子") },
            )
            OutlinedTextField(back, Modifier.fillMaxWidth(), label = { Text("背面") })
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!isNew) TextButton({ close(onDelete) }) { Text("删除", color = MaterialTheme.colorScheme.error) }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = {
                        onSave(card.copy(front = front.text.trim().toString(), back = back.text.trim().toString()))
                        if (isNew) {
                            front.clearText()
                            back.clearText()
                            focus.requestFocus()
                        } else {
                            close()
                        }
                    },
                    enabled = front.text.isNotBlank(),
                ) { Text(if (isNew) "添加" else "保存") }
            }
        }
        LaunchedEffect(Unit) { focus.requestFocus() }
    }
}
