package com.smallkang.ankirsi

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import android.text.Html
import android.util.AtomicFile
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.util.readText
import androidx.core.util.writeText
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileNotFoundException
import java.io.OutputStream
import java.util.concurrent.Callable
import java.util.concurrent.Executors

/** One thread for all file access, so writes land in the order they were made. */
private val disk = Executors.newSingleThreadExecutor()

/** All decks, held in memory and saved to disk after every change. Every review is also appended to a CSV log. */
class Library(private val app: Application) : AndroidViewModel(app) {
    private val json = Json { ignoreUnknownKeys = true }
    private val store = AtomicFile(File(app.filesDir, "decks.json"))
    private val reviewLog = File(app.filesDir, "reviews.csv").apply {
        if (!exists()) writeText("card_id,review_time,review_rating\n")
    }

    // Loaded on the file thread, so it waits for any write still pending from an earlier Library.
    var decks: List<Deck> by mutableStateOf(disk.submit(Callable { load() }).get())
        private set

    private var lastId = maxId(decks)

    fun deck(id: Long) = decks.find { it.id == id }

    fun addDeck(name: String) = update { it + Deck(newId(), name) }

    fun renameDeck(id: Long, name: String) = edit(id) { it.copy(name = name) }

    fun deleteDeck(id: Long) = update { decks -> decks.filter { it.id != id } }

    /** Adds [card] if its id is 0, otherwise replaces the card with the same id. */
    fun saveCard(deckId: Long, card: Card) = edit(deckId) { deck ->
        deck.copy(
            cards = if (card.id == 0L) deck.cards + card.copy(id = newId())
            else deck.cards.map { if (it.id == card.id) card else it },
        )
    }

    fun deleteCard(deckId: Long, cardId: Long) = edit(deckId) { deck ->
        deck.copy(cards = deck.cards.filter { it.id != cardId })
    }

    fun review(deckId: Long, card: Card, grade: Int) {
        val reviewed = Fsrs.review(card, grade)
        saveCard(deckId, reviewed)
        disk.execute { reviewLog.appendText("${card.id},${reviewed.lastReview},$grade\n") }
    }

    /** Restores a backup, or adds the rows of a TSV/CSV file (Anki's plain-text export works) to the deck named after it. */
    fun import(uri: Uri) = viewModelScope.launch {
        val result = runCatching {
            val (name, text) = withContext(Dispatchers.IO) { displayName(uri) to read(uri) }
            if (text.trimStart().startsWith('[')) restore(json.decodeFromString(text))
            else addCards(name.substringBeforeLast('.'), parseTable(text))
        }
        toast(result.fold({ "已导入 $it 张卡片" }, { "无法导入：${it.message}" }))
    }

    fun backup(uri: Uri) = export(uri) { it.write(json.encodeToString(decks).encodeToByteArray()) }

    /** The log's columns are what the FSRS optimizer reads to fit personal parameters. */
    fun exportReviews(uri: Uri) = export(uri) { out -> reviewLog.inputStream().use { it.copyTo(out) } }

    private fun export(uri: Uri, write: (OutputStream) -> Unit) = viewModelScope.launch {
        val result = runCatching {
            withContext(Dispatchers.IO) { app.contentResolver.openOutputStream(uri)!!.use(write) }
        }
        toast(result.fold({ "已导出" }, { "导出失败：${it.message}" }))
    }

    /** Backed-up decks replace the decks with the same id; the rest are added. */
    private fun restore(backup: List<Deck>): Int {
        update { decks -> (decks.associateBy { it.id } + backup.associateBy { it.id }).values.toList() }
        lastId = maxOf(lastId, maxId(backup))
        return backup.sumOf { it.cards.size }
    }

    private fun addCards(deckName: String, rows: List<List<String>>): Int {
        val deck = decks.find { it.name == deckName }
        val seen = deck?.cards.orEmpty().mapTo(HashSet()) { it.front to it.back }
        val cards = rows
            .filterNot { it[0].startsWith("#") } // Anki header lines such as #separator:tab
            .map { plain(it[0]) to plain(it.getOrElse(1) { "" }) }
            .filter { it.first.isNotEmpty() && seen.add(it) } // skips blanks and duplicates
            .map { (front, back) -> Card(newId(), front, back) }
        when {
            deck != null -> edit(deck.id) { it.copy(cards = it.cards + cards) }
            cards.isNotEmpty() -> update { it + Deck(newId(), deckName, cards) }
        }
        return cards.size
    }

    private fun update(change: (List<Deck>) -> List<Deck>) {
        val next = change(decks)
        decks = next
        disk.execute { store.writeText(json.encodeToString(next)) }
    }

    private fun edit(deckId: Long, change: (Deck) -> Deck) =
        update { decks -> decks.map { if (it.id == deckId) change(it) else it } }

    private fun load(): List<Deck> = try {
        json.decodeFromString(store.readText())
    } catch (_: FileNotFoundException) {
        emptyList()
    }

    /** Millisecond timestamps, bumped by one when two would collide. */
    private fun newId() = maxOf(lastId + 1, System.currentTimeMillis()).also { lastId = it }

    private fun read(uri: Uri) =
        app.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }.removePrefix("﻿")

    private fun displayName(uri: Uri) =
        app.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { if (it.moveToFirst()) it.getString(0) else null } ?: "导入"

    private fun toast(text: String) = Toast.makeText(app, text, Toast.LENGTH_SHORT).show()
}

private fun maxId(decks: List<Deck>) = decks.flatMap { deck -> deck.cards.map { it.id } + deck.id }.maxOrNull() ?: 0L

/** Anki exports fields as HTML; keep the text. U+FFFC is what images turn into. */
private fun plain(field: String): String {
    val text = if ('<' in field || '&' in field) Html.fromHtml(field, Html.FROM_HTML_MODE_COMPACT).toString() else field
    return text.replace('￼', ' ').trim()
}

/** Splits TSV or CSV text into rows of fields, honoring "quoted" fields. */
internal fun parseTable(text: String): List<List<String>> {
    val separator = if ('\t' in text) '\t' else ','
    val rows = mutableListOf<List<String>>()
    val row = mutableListOf<String>()
    val field = StringBuilder()
    var quoted = false
    fun endField() = row.add(field.toString()).also { field.clear() }
    fun endRow() = rows.add(row.toList()).also { row.clear() }

    var i = 0
    while (i < text.length) {
        val c = text[i++]
        when {
            quoted && c == '"' && text.getOrNull(i) == '"' -> field.append(text[i++])
            c == '"' && (quoted || field.isEmpty()) -> quoted = !quoted
            quoted || (c != separator && c != '\n' && c != '\r') -> field.append(c)
            c == separator -> endField()
            c == '\n' -> { endField(); endRow() }
        }
    }
    if (field.isNotEmpty() || row.isNotEmpty()) { endField(); endRow() }
    return rows.filter { fields -> fields.any { it.isNotBlank() } }
}
