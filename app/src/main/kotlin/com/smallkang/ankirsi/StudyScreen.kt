package com.smallkang.ankirsi

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val grades = listOf("重来", "困难", "良好", "简单")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(library: Library, deckId: Long, back: () -> Unit) {
    val deck = library.deck(deckId) ?: return
    val today = studyDay()
    val queue = remember(deck, today) { deck.queue(today) }
    val card = queue.firstOrNull()
    var revealed by rememberSaveable(card) { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val colors = MaterialTheme.colorScheme

    Scaffold(
        topBar = {
            PageBar(navigationIcon = { BackButton(back) }) {
                if (card != null) Text("${queue.size}", Modifier.padding(end = Margin), color = colors.onSurfaceVariant)
            }
        },
    ) { padding ->
        if (card == null) {
            val tomorrow = deck.cards.count { !it.isNew && it.due == today + 1 }
            Finished(if (tomorrow > 0) "明天有 $tomorrow 张要复习" else null, Modifier.padding(padding))
            return@Scaffold
        }
        Column(Modifier.padding(padding), horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedContent(
                card,
                Modifier.weight(1f),
                transitionSpec = {
                    (fadeIn(tween(240, 80)) + slideInVertically(tween(240, 80)) { it / 24 }) togetherWith fadeOut(tween(80))
                },
            ) { shown ->
                CardFace(shown, revealed || shown != card) { revealed = true }
            }
            Box(Modifier.readingWidth().padding(horizontal = Margin, vertical = 12.dp).height(52.dp)) {
                if (revealed) {
                    Grades(card, today) { grade ->
                        haptics.performHapticFeedback(if (grade == 1) HapticFeedbackType.Reject else HapticFeedbackType.Confirm)
                        library.review(deckId, card, grade)
                    }
                } else {
                    PrimaryButton("显示答案", { revealed = true })
                }
            }
        }
    }
}

/** Tap anywhere to reveal the answer. */
@Composable
private fun CardFace(card: Card, revealed: Boolean, onReveal: () -> Unit) {
    Box(
        Modifier.fillMaxSize().clickable(interactionSource = null, indication = null, onClick = onReveal),
        contentAlignment = Alignment.Center,
    ) {
        Column(Modifier.readingWidth().verticalScroll(rememberScrollState()).padding(horizontal = 32.dp, vertical = 24.dp)) {
            CardText(clozed(card.front, revealed, MaterialTheme.colorScheme.tertiary), prompt = true)
            AnimatedVisibility(revealed && card.back.isNotEmpty(), enter = fadeIn() + expandVertically()) {
                Column {
                    HorizontalDivider(Modifier.padding(vertical = 28.dp), thickness = 0.5.dp)
                    CardText(AnnotatedString(card.back), prompt = false)
                }
            }
        }
    }
}

/** Card content is set in serif: centred with balanced lines while short, left-aligned once it reads as a passage. */
@Composable
private fun CardText(text: AnnotatedString, prompt: Boolean) {
    val type = MaterialTheme.typography
    val passage = text.length > 60
    val style = when {
        text.length <= 12 -> if (prompt) type.headlineLarge else type.headlineSmall
        prompt && !passage -> type.headlineSmall
        prompt -> type.titleLarge.copy(fontFamily = FontFamily.Serif, lineHeight = 32.sp)
        else -> type.bodyLarge.copy(fontFamily = FontFamily.Serif, fontSize = 18.sp, lineHeight = 28.sp)
    }
    Text(
        text,
        Modifier.fillMaxWidth(),
        style = style.copy(lineBreak = if (passage) LineBreak.Paragraph else LineBreak.Heading),
        textAlign = if (passage) TextAlign.Start else TextAlign.Center,
    )
}

/** Four answers, each labelled with when the card would come back. "Good" is the usual answer, so it is filled. */
@Composable
private fun Grades(card: Card, today: Long, onGrade: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxSize(), Arrangement.spacedBy(8.dp)) {
        grades.forEachIndexed { i, label ->
            val grade = i + 1
            val content: @Composable () -> Unit = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(label, color = if (grade == 1) colors.tertiary else Color.Unspecified, style = MaterialTheme.typography.labelLarge)
                    Text(span(Fsrs.review(card, grade).due - today), style = MaterialTheme.typography.labelSmall)
                }
            }
            val modifier = Modifier.weight(1f).fillMaxHeight()
            if (grade == 3) {
                Button({ onGrade(grade) }, modifier, contentPadding = PaddingValues(0.dp)) { content() }
            } else {
                OutlinedButton(
                    { onGrade(grade) },
                    modifier,
                    border = BorderStroke(0.5.dp, colors.outline),
                    contentPadding = PaddingValues(0.dp),
                ) { content() }
            }
        }
    }
}

@Composable
private fun Finished(detail: String?, modifier: Modifier) {
    Column(
        modifier.fillMaxSize().padding(Margin),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("今天学完了", style = MaterialTheme.typography.headlineMedium)
        if (detail != null) {
            Text(detail, Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
