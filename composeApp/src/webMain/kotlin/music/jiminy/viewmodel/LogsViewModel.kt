package music.jiminy.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import music.jiminy.LogEntry
import music.jiminy.service.JiminyLogger
import music.jiminy.service.MainService

enum class LogSource {
    Client,
    Server,
}

data class UiLogEntry(
    val entry: LogEntry,
    val source: LogSource,
)

class LogsViewModel(
    private val clientLogger: JiminyLogger,
    private val mainService: MainService,
) : ViewModel() {
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _serverLogs = MutableStateFlow<List<LogEntry>>(emptyList())

    val logs: StateFlow<List<UiLogEntry>> = combine(
        clientLogger.logs,
        _serverLogs,
    ) { client, server ->
        val combined = client.map { UiLogEntry(it, LogSource.Client) } +
                server.map { UiLogEntry(it, LogSource.Server) }
        combined.sortedByDescending { it.entry.timestamp }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
    )

    fun resetError() {
        _errorMessage.update { null }
    }

    fun loadServerLogs() {
        viewModelScope.launch {
            mainService.getServerLogs(
                onSuccess = { response ->
                    _serverLogs.update { response.value }
                },
                onError = { /* Handle error */ },
            )
        }
    }

    fun flushLogs() {
        clientLogger.clear()
        viewModelScope.launch {
            mainService.flushServerLogs(
                onSuccess = {
                    _serverLogs.update { emptyList() }
                },
                onError = { /* Handle error */ },
            )
        }
    }
}
