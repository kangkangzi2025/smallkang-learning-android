package com.smallkang.ankirsi

import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.ZoneId

const val NEW_CARDS_PER_DAY = 20

/** A flashcard and its FSRS memory state. `{{…}}` in [front] marks a cloze blank. */
@Serializable
data class Card(
    val id: Long,
    val front: String,
    val back: String = "",
    val due: Long = 0, // study day
    val firstReview: Long = 0, // epoch millis, 0 while new
    val lastReview: Long = 0, // epoch millis, 0 while new
    val stability: Double = 0.0, // days until recall probability falls to 90%
    val difficulty: Double = 0.0, // 1–10
) {
    val isNew get() = lastReview == 0L
}

@Serializable
data class Deck(val id: Long, val name: String, val cards: List<Card> = emptyList())

/** The study day of [millis] as an epoch day. As in Anki, a day starts at 4 a.m. */
fun studyDay(millis: Long = System.currentTimeMillis()): Long =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).minusHours(4).toLocalDate().toEpochDay()

/** Today's cards in study order: due reviews, new cards up to the daily limit, then cards failed today. */
fun Deck.queue(today: Long): List<Card> {
    val (new, seen) = cards.partition { it.isNew }
    val (failed, due) = seen.filter { it.due <= today }.partition { studyDay(it.lastReview) == today }
    val newLeft = NEW_CARDS_PER_DAY - seen.count { studyDay(it.firstReview) == today }
    return due.sortedBy { it.due } + new.take(newLeft.coerceAtLeast(0)) + failed.sortedBy { it.lastReview }
}
