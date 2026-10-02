package com.smallkang.ankirsi

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import java.util.Locale

/** Page margin shared by titles, rows and buttons. */
val Margin = 24.dp

/**
 * Fills the width up to a comfortable reading column. On phones that is the whole screen; on tablets
 * (an 8.8" OPPO Pad Mini is about 840 dp wide in portrait) content stays in a centred column.
 */
fun Modifier.readingWidth() = widthIn(max = 680.dp).fillMaxWidth()

/** The top bar over the reading column. With a title it is a large serif title that collapses on scroll. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageBar(
    title: String? = null,
    scroll: TopAppBarScrollBehavior? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    val page = MaterialTheme.colorScheme.background
    val colors = TopAppBarDefaults.topAppBarColors(containerColor = page, scrolledContainerColor = page)
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        if (title == null) {
            TopAppBar(
                title = {},
                modifier = Modifier.readingWidth(),
                navigationIcon = navigationIcon,
                actions = actions,
                colors = colors,
            )
        } else {
            LargeTopAppBar(
                title = { Text(title, Modifier.padding(start = 8.dp), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                modifier = Modifier.readingWidth(),
                navigationIcon = navigationIcon,
                actions = actions,
                colors = colors,
                scrollBehavior = scroll,
            )
        }
    }
}

@Composable
fun IconAction(icon: Int, description: String, onClick: () -> Unit) {
    IconButton(onClick) { Icon(painterResource(icon), contentDescription = description) }
}

@Composable
fun BackButton(onClick: () -> Unit) = IconAction(R.drawable.ic_back, "返回", onClick)

@Composable
fun OverflowMenu(vararg items: Pair<String, () -> Unit>) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconAction(R.drawable.ic_more, "更多") { expanded = true }
        DropdownMenu(expanded, { expanded = false }, shape = RoundedCornerShape(14.dp)) {
            for ((label, action) in items) {
                DropdownMenuItem(text = { Text(label) }, onClick = { expanded = false; action() })
            }
        }
    }
}

/** The one filled button on a screen. */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(onClick, modifier.fillMaxWidth().height(52.dp), enabled) { Text(text) }
}

@Composable
fun Hairline() = HorizontalDivider(Modifier.readingWidth().padding(horizontal = Margin), thickness = 0.5.dp)

/** A quiet line of secondary text under a page title. */
@Composable
fun Note(text: String) {
    Text(
        text,
        Modifier.readingWidth().padding(start = Margin, end = Margin, bottom = 12.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
fun NameDialog(title: String, initial: String = "", onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val name = rememberTextFieldState(initial)
    val focus = remember { FocusRequester() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(name, Modifier.focusRequester(focus), lineLimits = TextFieldLineLimits.SingleLine)
            LaunchedEffect(Unit) { focus.requestFocus() }
        },
        confirmButton = {
            TextButton({ onConfirm(name.text.trim().toString()); onDismiss() }, enabled = name.text.isNotBlank()) {
                Text("确定")
            }
        },
        dismissButton = { TextButton(onDismiss) { Text("取消") } },
    )
}

/** "今天", "3天", "1.5个月", "2年". */
fun span(days: Long): String = when {
    days < 1 -> "今天"
    days < 30 -> "${days}天"
    days < 365 -> "${tenths(days / 30.0)}个月"
    else -> "${tenths(days / 365.0)}年"
}

private fun tenths(x: Double) = "%.1f".format(Locale.ROOT, x).removeSuffix(".0")

private val cloze = Regex("""\{\{(?:c\d+::)?(.+?)(?:::(.+?))?\}\}""")

/** Renders `{{answer}}` (and Anki's `{{c1::answer::hint}}`) as a blank, or highlighted once revealed. */
fun clozed(text: String, revealed: Boolean, color: Color) = buildAnnotatedString {
    var end = 0
    for (match in cloze.findAll(text)) {
        append(text, end, match.range.first)
        val (answer, hint) = match.destructured
        withStyle(SpanStyle(color, fontWeight = FontWeight.Medium)) {
            append(if (revealed) answer else "[${hint.ifEmpty { "…" }}]")
        }
        end = match.range.last + 1
    }
    append(text, end, text.length)
}
