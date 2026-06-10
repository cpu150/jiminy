package music.jiminy.viewmodel

import kotlinx.browser.window
import music.jiminy.JiminyThemeType
import music.jiminy.THEME_KEY
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ThemeViewModelTest {

    @BeforeTest
    fun setUp() {
        window.localStorage.removeItem(THEME_KEY)
    }

    @Test
    fun testInitialThemeIsDarkByDefault() {
        val viewModel = ThemeViewModel()
        assertEquals(JiminyThemeType.DARK, viewModel.currentTheme.value)
    }

    @Test
    fun testInitialThemeLoadedFromLocalStorage() {
        window.localStorage.setItem(THEME_KEY, JiminyThemeType.LIGHT.name)
        val viewModel = ThemeViewModel()
        assertEquals(JiminyThemeType.LIGHT, viewModel.currentTheme.value)
    }

    @Test
    fun testToggleThemeCyclesThroughAllThemes() {
        val viewModel = ThemeViewModel()

        // Dark -> Light
        viewModel.toggleTheme()
        assertEquals(JiminyThemeType.LIGHT, viewModel.currentTheme.value)
        assertEquals(JiminyThemeType.LIGHT.name, window.localStorage.getItem(THEME_KEY))

        // Light -> Ocean
        viewModel.toggleTheme()
        assertEquals(JiminyThemeType.OCEAN, viewModel.currentTheme.value)
        assertEquals(JiminyThemeType.OCEAN.name, window.localStorage.getItem(THEME_KEY))

        // Ocean -> Dark
        viewModel.toggleTheme()
        assertEquals(JiminyThemeType.DARK, viewModel.currentTheme.value)
        assertEquals(JiminyThemeType.DARK.name, window.localStorage.getItem(THEME_KEY))
    }
}
