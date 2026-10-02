package com.smallkang.ankirsi

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecksScreen(library: Library, open: (Long) -> Unit) {
    var creating by rememberSaveable { mutableStateOf(false) }
    val openFile = rememberLauncherForActivityResult(OpenDocument()) { uri -> uri?.let(library::import) }
    val saveBackup = rememberLauncherForActivityResult(CreateDocument("application/json")) { uri -> uri?.let(library::backup) }
    val saveReviews = rememberLauncherForActivityResult(CreateDocument("text/csv")) { uri -> uri?.let(library::exportReviews) }
    val today = studyDay()
    val due = library.decks.map { it.queue(today).size }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            val title = when {
                library.decks.isEmpty() -> "还没有卡组"
                due.sum() > 0 -> "今天要学 ${due.sum()} 张"
                else -> "今天学完了"
            }
            PageBar(title, scroll) {
                IconAction(R.drawable.ic_add, "新建卡组") { creating = true }
                OverflowMenu(
                    "导入文件" to { openFile.launch(arrayOf("text/*", "application/json")) },
                    "备份" to { saveBackup.launch("anki-rsi-${LocalDate.now()}.json") },
                    "导出复习记录" to { saveReviews.launch("anki-rsi-reviews.csv") },
                )
            }
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize(), contentPadding = padding, horizontalAlignment = Alignment.CenterHorizontally) {
            if (library.decks.isEmpty()) item { Note("点右上角 + 新建卡组，或从 ⋯ 导入 TSV / CSV 文件。") }
            itemsIndexed(library.decks, key = { _, deck -> deck.id }) { i, deck ->
                if (i > 0) Hairline()
                DeckRow(deck, due[i]) { open(deck.id) }
            }
        }
    }

    if (creating) NameDialog("新建卡组", onDismiss = { creating = false }, onConfirm = library::addDeck)
}

@Composable
private fun DeckRow(deck: Deck, due: Int, onClick: () -> Unit) {
    Row(
        Modifier.readingWidth().clickable(onClick = onClick).padding(horizontal = Margin, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(deck.name, style = MaterialTheme.typography.titleMedium)
            Text(
                "${deck.cards.size} 张",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (due > 0) Text("$due", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.tertiary)
    }
}
