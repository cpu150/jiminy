package music.jiminy.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.browser.window
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import music.jiminy.JiminyThemeType
import music.jiminy.THEME_KEY

class ThemeViewModel : ViewModel() {
    private val _currentTheme = MutableStateFlow(loadTheme())
    val currentTheme: StateFlow<JiminyThemeType> = _currentTheme.asStateFlow()

    private fun loadTheme(): JiminyThemeType {
        val savedTheme = window.localStorage.getItem(THEME_KEY)
        return try {
            JiminyThemeType.valueOf(savedTheme ?: JiminyThemeType.DARK.name)
        } catch (_: Exception) {
            JiminyThemeType.DARK
        }
    }

    fun toggleTheme() {
        val themes = JiminyThemeType.entries
        val nextIndex = (currentTheme.value.ordinal + 1) % themes.size
        val nextTheme = themes[nextIndex]

        _currentTheme.update { nextTheme }
        window.localStorage.setItem(THEME_KEY, nextTheme.name)
    }
}
