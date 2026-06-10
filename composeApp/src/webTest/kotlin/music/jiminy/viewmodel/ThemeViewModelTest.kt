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
    fun testThemePopupLogic() {
        val viewModel = ThemeViewModel()

        // Initial state
        assertEquals(false, viewModel.showThemePopup.value)

        // Show popup
        viewModel.onThemeButtonClick()
        assertEquals(true, viewModel.showThemePopup.value)

        // Set theme (should close popup)
        viewModel.setTheme(JiminyThemeType.LIGHT)
        assertEquals(JiminyThemeType.LIGHT, viewModel.currentTheme.value)
        assertEquals(false, viewModel.showThemePopup.value)
        assertEquals(JiminyThemeType.LIGHT.name, window.localStorage.getItem(THEME_KEY))

        // Re-show and dismiss
        viewModel.onThemeButtonClick()
        assertEquals(true, viewModel.showThemePopup.value)
        viewModel.dismissPopup()
        assertEquals(false, viewModel.showThemePopup.value)
    }
}
