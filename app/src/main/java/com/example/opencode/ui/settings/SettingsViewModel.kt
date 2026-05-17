package com.example.opencode.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.opencode.data.local.SettingsStore
import com.example.opencode.data.model.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsStore: SettingsStore,
) : ViewModel() {

    val settings: StateFlow<UserSettings> = settingsStore.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    fun updateUserName(name: String) {
        viewModelScope.launch {
            settingsStore.save(settings.value.copy(userName = name))
        }
    }

    fun updateThemeColor(color: Int) {
        viewModelScope.launch {
            settingsStore.save(settings.value.copy(themeColor = color))
        }
    }
}
