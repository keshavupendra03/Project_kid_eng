package com.samjhoenglish.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.samjhoenglish.AppContainer
import com.samjhoenglish.data.db.Child
import com.samjhoenglish.i18n.AppLanguage
import com.samjhoenglish.i18n.LocalAppLanguage
import com.samjhoenglish.speech.LocalPhonics
import com.samjhoenglish.speech.LocalSoundFx
import com.samjhoenglish.speech.LocalSpeaker
import com.samjhoenglish.speech.LocalVoiceListener
import com.samjhoenglish.ui.home.HomeScreen
import com.samjhoenglish.ui.home.JourneyViewModel
import com.samjhoenglish.ui.language.LanguagePickerScreen
import com.samjhoenglish.ui.lesson.LessonScreen
import com.samjhoenglish.ui.lesson.LessonViewModel
import com.samjhoenglish.ui.level.LevelScreen
import com.samjhoenglish.ui.placement.PlacementScreen
import com.samjhoenglish.ui.placement.PlacementViewModel
import com.samjhoenglish.ui.profile.ProfilePickerScreen
import com.samjhoenglish.ui.profile.ProfileSetupScreen
import com.samjhoenglish.ui.settings.SettingsScreen
import kotlinx.serialization.Serializable

@Serializable
private object LanguagePickerRoute

@Serializable
private object ProfileSetupRoute

@Serializable
private object ProfilePickerRoute

@Serializable
private data class PlacementRoute(val childId: Long)

@Serializable
private object HomeRoute

@Serializable
private object SettingsRoute

@Serializable
private data class LevelRoute(val number: Int)

/** [lessonId] null means "today's task", planned by the app. */
@Serializable
private data class LessonRoute(val lessonId: String? = null)

@Composable
fun SamjhoApp(container: AppContainer) {
    val viewModel: AppViewModel = viewModel(factory = AppViewModel.factory(container.settings, container.progress))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val missingVoices by container.speaker.missingVoices.collectAsStateWithLifecycle()
    val englishVoice by viewModel.englishVoice.collectAsStateWithLifecycle()
    LaunchedEffect(englishVoice) { container.speaker.englishVoice = englishVoice }

    val ready = state as? AppUiState.Ready
    if (ready == null) {
        // Settings load in a few milliseconds; show the plain background meanwhile.
        Box(Modifier.fillMaxSize())
        return
    }

    val language = ready.language ?: AppLanguage.HINGLISH
    val child = ready.currentChild
    LaunchedEffect(child?.id) { child?.let { container.studyTimer.setChild(it.id) } }

    val startDestination: Any = remember {
        when {
            ready.language == null -> LanguagePickerRoute
            ready.children.isEmpty() -> ProfileSetupRoute
            child == null -> ProfilePickerRoute
            !child.placementDone -> PlacementRoute(child.id)
            else -> HomeRoute
        }
    }

    CompositionLocalProvider(
        LocalAppLanguage provides language,
        LocalSpeaker provides container.speaker,
        LocalSoundFx provides container.sounds,
        LocalPhonics provides container.phonics,
        LocalVoiceListener provides container.listener,
    ) {
        val nav = rememberNavController()
        NavHost(navController = nav, startDestination = startDestination) {
            composable<LanguagePickerRoute> {
                LanguagePickerScreen(
                    onChosen = { chosen ->
                        viewModel.setLanguage(chosen)
                        nav.startFresh(if (ready.children.isEmpty()) ProfileSetupRoute else HomeRoute)
                    },
                )
            }
            composable<ProfileSetupRoute> {
                ProfileSetupScreen(
                    progress = container.progress,
                    onCreated = { id ->
                        viewModel.setCurrentChild(id)
                        nav.startFresh(PlacementRoute(id))
                    },
                )
            }
            composable<ProfilePickerRoute> {
                ProfilePickerScreen(
                    children = ready.children,
                    onPick = { id ->
                        viewModel.setCurrentChild(id)
                        val picked = ready.children.first { it.id == id }
                        nav.startFresh(if (picked.placementDone) HomeRoute else PlacementRoute(id))
                    },
                    onAddChild = dropUnlessResumed { nav.navigate(ProfileSetupRoute) },
                )
            }
            composable<PlacementRoute> { entry ->
                val childId = entry.toRoute<PlacementRoute>().childId
                PlacementScreen(
                    viewModel = viewModel(factory = PlacementViewModel.factory(childId, container.content, container.progress)),
                    onDone = { nav.startFresh(HomeRoute) },
                )
            }
            composable<HomeRoute> {
                WithChild(child) { c ->
                    val journey: JourneyViewModel = viewModel(
                        key = "journey-${c.id}",
                        factory = JourneyViewModel.factory(c.id, container.content, container.progress),
                    )
                    val journeyState by journey.uiState.collectAsStateWithLifecycle()
                    HomeScreen(
                        child = c,
                        journey = journeyState,
                        voiceMissing = missingVoices.isNotEmpty(),
                        onSwitchChild = dropUnlessResumed { nav.navigate(ProfilePickerRoute) },
                        onOpenSettings = dropUnlessResumed { nav.navigate(SettingsRoute) },
                        onOpenLevel = { number -> nav.navigate(LevelRoute(number)) },
                        onStartToday = dropUnlessResumed { nav.navigate(LessonRoute()) },
                    )
                }
            }
            composable<SettingsRoute> {
                SettingsScreen(
                    current = language,
                    onLanguageChange = viewModel::setLanguage,
                    englishVoice = englishVoice,
                    onEnglishVoiceChange = viewModel::setEnglishVoice,
                    onBack = dropUnlessResumed { nav.popBackStack() },
                )
            }
            composable<LevelRoute> { entry ->
                val number = entry.toRoute<LevelRoute>().number
                WithChild(child) { c ->
                    val journey: JourneyViewModel = viewModel(
                        key = "journey-${c.id}",
                        factory = JourneyViewModel.factory(c.id, container.content, container.progress),
                    )
                    val journeyState by journey.uiState.collectAsStateWithLifecycle()
                    journeyState.levels.firstOrNull { it.number == number }?.let { level ->
                        LevelScreen(
                            level = level,
                            journey = journeyState,
                            onBack = dropUnlessResumed { nav.popBackStack() },
                            onOpenLesson = { id -> nav.navigate(LessonRoute(id)) },
                        )
                    }
                }
            }
            composable<LessonRoute> { entry ->
                val lessonId = entry.toRoute<LessonRoute>().lessonId
                WithChild(child) { c ->
                    LessonScreen(
                        viewModel = viewModel(
                            key = "lesson-${c.id}-$lessonId",
                            factory = LessonViewModel.factory(c.id, lessonId, container.progress),
                        ),
                        onExit = dropUnlessResumed { nav.popBackStack() },
                        onHome = dropUnlessResumed { nav.popBackStack<HomeRoute>(inclusive = false) },
                        onNextLesson = { next ->
                            nav.navigate(LessonRoute(next)) { popUpTo<LessonRoute> { inclusive = true } }
                        },
                    )
                }
            }
        }
    }
}

/** Screens that belong to a child only render once one is chosen. */
@Composable
private fun WithChild(child: Child?, content: @Composable (Child) -> Unit) {
    if (child != null) content(child)
}

/** Go to [route] and clear everything behind it (used after setup steps the child shouldn't go back to). */
private fun NavHostController.startFresh(route: Any) {
    navigate(route) { popUpTo(graph.id) { inclusive = true } }
}
