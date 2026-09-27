package net.thunderbird.wear.ui.reader

import assertk.assertThat
import assertk.assertions.containsExactly
import kotlin.test.Test

class ParagraphsTest {
    @Test
    fun `text is split at blank lines, including Windows line breaks`() {
        val text = "Hi Ann,\r\n\r\nthe meeting moved.\nIt's at 10 now.\n\n\n  \nThanks,\nBen\n"

        assertThat(text.toParagraphs()).containsExactly(
            "Hi Ann,",
            "the meeting moved.\nIt's at 10 now.",
            "Thanks,\nBen",
        )
    }
}
