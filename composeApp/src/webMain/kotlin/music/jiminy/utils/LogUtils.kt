package music.jiminy.utils

import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

object LogUtils {
    fun formatTimestamp(millis: Long): String {
        val instant = Instant.fromEpochMilliseconds(millis)
        val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())

        val hour = dateTime.hour.toString().padStart(2, '0')
        val minute = dateTime.minute.toString().padStart(2, '0')
        val second = dateTime.second.toString().padStart(2, '0')
        val nano = (dateTime.nanosecond / 1_000_000).toString().padStart(3, '0')

        return "$hour:$minute:$second.$nano"
    }
}
