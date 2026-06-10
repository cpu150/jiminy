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

    private val _showThemePopup = MutableStateFlow(false)
    val showThemePopup: StateFlow<Boolean> = _showThemePopup.asStateFlow()

    private fun loadTheme(): JiminyThemeType {
        val savedTheme = window.localStorage.getItem(THEME_KEY)
        return try {
            JiminyThemeType.valueOf(savedTheme ?: JiminyThemeType.DARK.name)
        } catch (_: Exception) {
            JiminyThemeType.DARK
        }
    }

    fun onThemeButtonClick() {
        _showThemePopup.update { true }
    }

    fun dismissPopup() {
        _showThemePopup.update { false }
    }

    fun setTheme(theme: JiminyThemeType) {
        _currentTheme.update { theme }
        window.localStorage.setItem(THEME_KEY, theme.name)
        dismissPopup()
    }
}
