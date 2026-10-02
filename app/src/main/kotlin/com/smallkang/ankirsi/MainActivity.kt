package com.smallkang.ankirsi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import kotlinx.serialization.Serializable

@Serializable data object DecksRoute : NavKey
@Serializable data class DeckRoute(val deckId: Long) : NavKey
@Serializable data class StudyRoute(val deckId: Long) : NavKey

class MainActivity : ComponentActivity() {
    private val library: Library by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                val stack = rememberNavBackStack(DecksRoute)
                val go = { route: NavKey -> if (stack.last() != route) stack.add(route) } // ignores double taps
                val back: () -> Unit = { stack.removeLastOrNull() }
                NavDisplay(
                    backStack = stack,
                    onBack = back,
                    entryProvider = entryProvider {
                        entry<DecksRoute> { DecksScreen(library, open = { go(DeckRoute(it)) }) }
                        entry<DeckRoute> { route ->
                            DeckScreen(library, route.deckId, study = { go(StudyRoute(route.deckId)) }, back = back)
                        }
                        entry<StudyRoute> { route -> StudyScreen(library, route.deckId, back) }
                    },
                )
            }
        }
    }
}
