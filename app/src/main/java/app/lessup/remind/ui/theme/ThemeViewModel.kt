package app.lessup.remind.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.lessup.remind.data.settings.SettingsRepository
import app.lessup.remind.data.settings.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class ThemeViewModel @Inject constructor(
    settingsRepository: SettingsRepository
) : ViewModel() {
    val themeMode = settingsRepository.themeMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ThemeMode.SYSTEM
    )
}
