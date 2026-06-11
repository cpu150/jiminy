package music.jiminy.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.browser.window
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import music.jiminy.JiminyLoggerI
import music.jiminy.JiminyThemeType
import music.jiminy.THEME_KEY

data class ThemeState(
    val currentTheme: JiminyThemeType = JiminyThemeType.DARK,
    val showThemePopup: Boolean = false,
)

sealed interface ThemeAction {
    data object OnThemeButtonClick : ThemeAction
    data object OnDismissPopup : ThemeAction
    data class OnThemeSelect(val theme: JiminyThemeType) : ThemeAction
}

class ThemeViewModel(
    private val logger: JiminyLoggerI,
) : ViewModel() {
    private val _state = MutableStateFlow(ThemeState(currentTheme = loadTheme()))
    val state: StateFlow<ThemeState> = _state.asStateFlow()

    fun onAction(action: ThemeAction) {
        when (action) {
            ThemeAction.OnThemeButtonClick -> _state.update { it.copy(showThemePopup = true) }
            ThemeAction.OnDismissPopup -> _state.update { it.copy(showThemePopup = false) }
            is ThemeAction.OnThemeSelect -> {
                _state.update {
                    it.copy(
                        currentTheme = action.theme,
                        showThemePopup = false,
                    )
                }
                window.localStorage.setItem(THEME_KEY, action.theme.name)
            }
        }
    }

    private fun loadTheme(): JiminyThemeType = try {
        val savedTheme = window.localStorage.getItem(THEME_KEY)
        JiminyThemeType.valueOf(savedTheme ?: JiminyThemeType.DARK.name)
    } catch (e: Exception) {
        logger.error("Failed to load theme: ${e.message}")
        JiminyThemeType.DARK
    }
}
