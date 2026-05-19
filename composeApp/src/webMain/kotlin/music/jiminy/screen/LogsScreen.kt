package music.jiminy.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import music.jiminy.LogEntry
import music.jiminy.LogType
import music.jiminy.viewmodel.LogsViewModel
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Instant

@Composable
fun LogsScreen(
    modifier: Modifier = Modifier,
) {
    LogsRoot(modifier)
}

@Composable
fun LogsRoot(
    modifier: Modifier = Modifier,
    viewModel: LogsViewModel = koinViewModel(),
) {
    val logs by viewModel.logs.collectAsStateWithLifecycle()

    LogsContent(
        logs = logs.toImmutableList(),
        modifier = modifier,
    )
}

@Composable
fun LogsContent(
    logs: ImmutableList<LogEntry>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(logs) { entry ->
            LogItem(entry)
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )
        }
    }
}

@Composable
fun LogItem(entry: LogEntry) {
    val textColor = when (entry.type) {
        LogType.INFO -> MaterialTheme.colorScheme.onSurface
        LogType.WARNING -> Color(0xFFFFA500) // Orange
        LogType.ERROR -> MaterialTheme.colorScheme.error
    }

    val timestamp = formatTimestamp(entry.timestamp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "[$timestamp]",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
            )
            Text(
                text = entry.type.name,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            )
        }
        Text(
            text = entry.message,
            color = textColor,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

private fun formatTimestamp(millis: Long): String {
    val instant = Instant.fromEpochMilliseconds(millis)
    val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())

    val hour = dateTime.hour.toString().padStart(2, '0')
    val minute = dateTime.minute.toString().padStart(2, '0')
    val second = dateTime.second.toString().padStart(2, '0')
    val nano = (dateTime.nanosecond / 1_000_000).toString().padStart(3, '0')

    return "$hour:$minute:$second.$nano"
}
