package music.jiminy.utils

import kotlin.test.Test
import kotlin.test.assertTrue

class LogUtilsTest {
    @Test
    fun testFormatTimestamp() {
        val timestamp = 1718217142000L // 2024-06-12T18:32:22Z
        val formatted = LogUtils.formatTimestamp(timestamp)

        // Format is HH:mm:ss.SSS
        // We can't strictly assert the hour because of TimeZone.currentSystemDefault()
        // but we can check the pattern and the seconds/milliseconds.
        val pattern = Regex("""\d{2}:\d{2}:22\.000""")
        assertTrue(
            pattern.matches(formatted),
            "Formatted timestamp '$formatted' should match pattern HH:mm:ss.SSS",
        )
    }
}
