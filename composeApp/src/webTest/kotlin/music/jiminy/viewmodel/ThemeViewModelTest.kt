package music.jiminy.viewmodel

import kotlinx.browser.window
import music.jiminy.JiminyThemeType
import music.jiminy.THEME_KEY
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ThemeViewModelTest {

    private val logger = FakeLogger()

    @BeforeTest
    fun setUp() {
        window.localStorage.removeItem(THEME_KEY)
        logger.clear()
    }

    @Test
    fun testInitialThemeIsDarkByDefault() {
        val viewModel = ThemeViewModel(logger)
        assertEquals(JiminyThemeType.DARK, viewModel.state.value.currentTheme)
    }

    @Test
    fun testInitialThemeLoadedFromLocalStorage() {
        window.localStorage.setItem(THEME_KEY, JiminyThemeType.LIGHT.name)
        val viewModel = ThemeViewModel(logger)
        assertEquals(JiminyThemeType.LIGHT, viewModel.state.value.currentTheme)
    }

    @Test
    fun testThemePopupLogic() {
        val viewModel = ThemeViewModel(logger)

        // Initial state
        assertEquals(false, viewModel.state.value.showThemePopup)

        // Show popup
        viewModel.onAction(ThemeAction.OnThemeButtonClick)
        assertEquals(true, viewModel.state.value.showThemePopup)

        // Set theme (should close popup)
        viewModel.onAction(ThemeAction.OnThemeSelect(JiminyThemeType.LIGHT))
        assertEquals(JiminyThemeType.LIGHT, viewModel.state.value.currentTheme)
        assertEquals(false, viewModel.state.value.showThemePopup)
        assertEquals(JiminyThemeType.LIGHT.name, window.localStorage.getItem(THEME_KEY))

        // Re-show and dismiss
        viewModel.onAction(ThemeAction.OnThemeButtonClick)
        assertEquals(true, viewModel.state.value.showThemePopup)
        viewModel.onAction(ThemeAction.OnDismissPopup)
        assertEquals(false, viewModel.state.value.showThemePopup)
    }

    @Test
    fun testLoadThemeErrorLogging() {
        window.localStorage.setItem(THEME_KEY, "INVALID_THEME")
        val viewModel = ThemeViewModel(logger)

        assertEquals(JiminyThemeType.DARK, viewModel.state.value.currentTheme)
        assertEquals(1, logger.loggedError.size)
        assertTrue(logger.loggedError[0].contains("Failed to load theme"))
    }
}
