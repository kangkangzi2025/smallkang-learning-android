package com.smallkang.ankirsi

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.TimeZone

/** Expected values come from py-fsrs 6 (no learning steps, no fuzzing) for the same review times. */
class FsrsTest {
    private data class Step(val time: Long, val grade: Int, val stability: Double, val difficulty: Double)

    @Test
    fun matchesPyFsrs() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"))
        val steps = listOf(
            Step(1767585600000, 3, 2.3065000000, 2.1181039705), // new card
            Step(1767758400000, 3, 10.9643323358, 2.1112142358),
            Step(1768363200000, 1, 1.4689194908, 7.3922381323), // lapse
            Step(1768366800000, 3, 1.5048620121, 7.3800742635), // same day
            Step(1768539600000, 2, 3.5117613613, 8.2459995769),
            Step(1769317200000, 4, 17.8901361549, 7.6451227412),
            Step(1769320800000, 1, 5.2537488609, 9.2111962275), // same day
            Step(1769407200000, 3, 6.2244959639, 9.1972134006),
        )
        var card = Card(1, "q")
        for (step in steps) {
            card = Fsrs.review(card, step.grade, step.time)
            assertEquals(step.stability, card.stability, 1e-9)
            assertEquals(step.difficulty, card.difficulty, 1e-9)
        }
    }
}
