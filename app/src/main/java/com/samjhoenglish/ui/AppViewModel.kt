package com.samjhoenglish.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.samjhoenglish.data.ProgressRepository
import com.samjhoenglish.data.SettingsRepository
import com.samjhoenglish.data.db.Child
import com.samjhoenglish.i18n.AppLanguage
import com.samjhoenglish.speech.EnglishVoice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AppUiState {
    data object Loading : AppUiState

    data class Ready(
        /** null until the user picks one on first launch. */
        val language: AppLanguage?,
        val children: List<Child>,
        val currentChildId: Long?,
    ) : AppUiState {
        /** The chosen child; with only one child on the phone, that one. */
        val currentChild: Child?
            get() = children.firstOrNull { it.id == currentChildId } ?: children.singleOrNull()
    }
}

class AppViewModel(
    private val settings: SettingsRepository,
    private val progress: ProgressRepository,
) : ViewModel() {
    // Applied instantly so the next screen already speaks the new language; saving finishes in the background.
    private val languageNow = MutableStateFlow<AppLanguage?>(null)
    private val childNow = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<AppUiState> = combine(
        settings.appLanguage,
        languageNow,
        progress.children(),
        settings.currentChildId,
        childNow,
    ) { savedLanguage, language, children, savedChild, child ->
        AppUiState.Ready(language ?: savedLanguage, children, child ?: savedChild)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppUiState.Loading)

    val englishVoice: StateFlow<EnglishVoice> =
        settings.englishVoice.stateIn(viewModelScope, SharingStarted.Eagerly, EnglishVoice.AMERICAN)

    val chimesOn: StateFlow<Boolean> = settings.chimesOn.stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun setChimesOn(on: Boolean) {
        viewModelScope.launch { settings.setChimesOn(on) }
    }

    fun setEnglishVoice(voice: EnglishVoice) {
        viewModelScope.launch { settings.setEnglishVoice(voice) }
    }

    fun setLanguage(language: AppLanguage) {
        languageNow.value = language
        viewModelScope.launch { settings.setAppLanguage(language) }
    }

    fun setCurrentChild(childId: Long) {
        childNow.value = childId
        viewModelScope.launch { settings.setCurrentChild(childId) }
    }

    companion object {
        fun factory(settings: SettingsRepository, progress: ProgressRepository): ViewModelProvider.Factory =
            viewModelFactory { initializer { AppViewModel(settings, progress) } }
    }
}
