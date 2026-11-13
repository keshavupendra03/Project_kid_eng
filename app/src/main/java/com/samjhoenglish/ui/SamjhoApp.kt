package com.samjhoenglish.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
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
import com.samjhoenglish.speech.LocalReadingListener
import com.samjhoenglish.speech.LocalVoiceListener
import com.samjhoenglish.content.isReadingLesson
import com.samjhoenglish.learning.Learning
import com.samjhoenglish.ui.reading.ReadingScreen
import com.samjhoenglish.ui.express.ExpressGameScreen
import com.samjhoenglish.ui.express.ExpressScreen
import com.samjhoenglish.ui.express.ExpressViewModel
import com.samjhoenglish.ui.words.WordPowerScreen
import com.samjhoenglish.ui.words.WordPowerViewModel
import com.samjhoenglish.ui.components.LocalTinkuPieces
import com.samjhoenglish.ui.home.HomeScreen
import com.samjhoenglish.ui.test.SchoolTestScreen
import com.samjhoenglish.ui.home.JourneyViewModel
import com.samjhoenglish.ui.language.LanguagePickerScreen
import com.samjhoenglish.ui.lesson.LessonScreen
import com.samjhoenglish.ui.lesson.LocalChildName
import com.samjhoenglish.ui.theme.SamjhoTheme
import com.samjhoenglish.ui.lesson.LessonViewModel
import com.samjhoenglish.ui.level.LevelScreen
import com.samjhoenglish.ui.book.BookScreen
import com.samjhoenglish.ui.book.ChapterScreen
import com.samjhoenglish.ui.parent.ParentArea
import com.samjhoenglish.ui.parent.TimeUpScreen
import com.samjhoenglish.ui.placement.PlacementScreen
import com.samjhoenglish.ui.placement.PlacementViewModel
import com.samjhoenglish.ui.profile.ProfilePickerScreen
import com.samjhoenglish.ui.profile.ProfileSetupScreen
import com.samjhoenglish.ui.settings.SettingsScreen
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
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

@Serializable
private object ReadingRoute

@Serializable
private object WordPowerRoute

@Serializable
private object BookRoute

@Serializable
private data class ChapterRoute(val chapterId: String)

@Serializable
private object ExpressRoute

@Serializable
private data class ExpressRoundRoute(val passageId: String, val challenge: Boolean)

@Serializable
private object ParentRoute

@Serializable
private object TimeUpRoute

@Serializable
private data class SchoolTestRoute(val testId: Long)

@Composable
fun SamjhoApp(container: AppContainer) {
    val viewModel: AppViewModel = viewModel(factory = AppViewModel.factory(container.settings, container.progress))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val missingVoices by container.speaker.missingVoices.collectAsStateWithLifecycle()
    val englishVoice by viewModel.englishVoice.collectAsStateWithLifecycle()
    LaunchedEffect(englishVoice) { container.speaker.englishVoice = englishVoice }
    val chimesOn by viewModel.chimesOn.collectAsStateWithLifecycle()
    LaunchedEffect(chimesOn) { container.sounds.chimesOn = chimesOn }

    val ready = state as? AppUiState.Ready
    if (ready == null) {
        // Settings load in a few milliseconds; show the plain background meanwhile.
        Box(Modifier.fillMaxSize())
        return
    }

    val language = ready.language ?: AppLanguage.HINGLISH
    val child = ready.currentChild
    LaunchedEffect(child?.id) {
        if (child != null) container.studyTimer.setChild(child.id) else container.studyTimer.clearChild()
    }

    val startDestination: Any = remember {
        when {
            ready.language == null -> LanguagePickerRoute
            ready.children.isEmpty() -> ProfileSetupRoute
            child == null -> ProfilePickerRoute
            !child.placementDone -> PlacementRoute(child.id)
            else -> HomeRoute
        }
    }

    val stars by remember(child?.id) { child?.let { container.progress.totalStars(it.id) } ?: flowOf(0) }
        .collectAsStateWithLifecycle(initialValue = 0)

    // The look follows the child: bright for Class 1–4, calmer for Class 5–8.
    SamjhoTheme(senior = child != null && !child.isJunior) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
    CompositionLocalProvider(
        LocalAppLanguage provides language,
        LocalSpeaker provides container.speaker,
        LocalSoundFx provides container.sounds,
        LocalPhonics provides container.phonics,
        LocalVoiceListener provides container.listener,
        LocalReadingListener provides container.reader,
        LocalChildName provides child?.name.orEmpty(),
        LocalTinkuPieces provides Learning.unlockedPieces(stars),
    ) {
        val nav = rememberNavController()
        val scope = rememberCoroutineScope()
        var checkingTime by remember { mutableStateOf(false) }

        // Every new lesson or game round goes through here, so the parent's daily limit is respected.
        fun openTimed(c: Child, route: Any, replaceCurrent: Boolean = false) {
            if (checkingTime || nav.currentBackStackEntry?.lifecycle?.currentState != Lifecycle.State.RESUMED) return
            checkingTime = true
            // The listener's model takes a few seconds to load; start now so reading doesn't wait.
            if ((route as? LessonRoute)?.lessonId?.isReadingLesson == true || route is ExpressRoundRoute) container.reader.warmUp()
            scope.launch {
                val timeUp = container.parent.timeIsUp(c)
                checkingTime = false
                nav.navigate(if (timeUp) TimeUpRoute else route) {
                    if (replaceCurrent && route is LessonRoute) popUpTo<LessonRoute> { inclusive = true }
                    if (replaceCurrent && route is ExpressRoundRoute) popUpTo<ExpressRoundRoute> { inclusive = true }
                }
            }
        }

        fun openLesson(c: Child, route: LessonRoute, replaceCurrent: Boolean = false) = openTimed(c, route, replaceCurrent)

        NavHost(
            navController = nav,
            startDestination = startDestination,
            enterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 10 } },
            exitTransition = { fadeOut(tween(150)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { fadeOut(tween(150)) + slideOutHorizontally(tween(220)) { it / 10 } },
        ) {
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
                        factory = JourneyViewModel.factory(c.id, container.content, container.progress, container.parent, c.classGrade),
                    )
                    val journeyState by journey.uiState.collectAsStateWithLifecycle()
                    val tests by remember(c.id) { container.school.activeTests(c.id) }.collectAsStateWithLifecycle(emptyList())
                    HomeScreen(
                        child = c,
                        journey = journeyState,
                        voiceMissing = missingVoices.isNotEmpty(),
                        onSwitchChild = dropUnlessResumed { nav.navigate(ProfilePickerRoute) },
                        onOpenParents = dropUnlessResumed { nav.navigate(ParentRoute) },
                        onOpenSettings = dropUnlessResumed { nav.navigate(SettingsRoute) },
                        onOpenLevel = { number -> nav.navigate(LevelRoute(number)) },
                        onStartToday = { openLesson(c, LessonRoute()) },
                        onMissionDone = journey::missionDone,
                        onOpenReading = dropUnlessResumed { nav.navigate(ReadingRoute) },
                        onOpenExpress = dropUnlessResumed { nav.navigate(ExpressRoute) },
                        onOpenWordPower = dropUnlessResumed { nav.navigate(WordPowerRoute) },
                        onOpenBook = dropUnlessResumed { nav.navigate(BookRoute) },
                        activeTests = tests,
                        onOpenTest = { id -> nav.navigate(SchoolTestRoute(id)) },
                    )
                }
            }
            composable<SchoolTestRoute> { entry ->
                val testId = entry.toRoute<SchoolTestRoute>().testId
                WithChild(child) { c ->
                    SchoolTestScreen(container, c, testId, onBack = dropUnlessResumed { nav.popBackStack() })
                }
            }
            composable<BookRoute> {
                WithChild(child) { c ->
                    val journey: JourneyViewModel = viewModel(
                        key = "journey-${c.id}",
                        factory = JourneyViewModel.factory(c.id, container.content, container.progress, container.parent, c.classGrade),
                    )
                    val journeyState by journey.uiState.collectAsStateWithLifecycle()
                    BookScreen(
                        journey = journeyState,
                        onBack = dropUnlessResumed { nav.popBackStack() },
                        onOpenChapter = { id -> nav.navigate(ChapterRoute(id)) },
                    )
                }
            }
            composable<ChapterRoute> { entry ->
                val chapterId = entry.toRoute<ChapterRoute>().chapterId
                WithChild(child) { c ->
                    val journey: JourneyViewModel = viewModel(
                        key = "journey-${c.id}",
                        factory = JourneyViewModel.factory(c.id, container.content, container.progress, container.parent, c.classGrade),
                    )
                    val journeyState by journey.uiState.collectAsStateWithLifecycle()
                    ChapterScreen(
                        journey = journeyState,
                        chapterId = chapterId,
                        onBack = dropUnlessResumed { nav.popBackStack() },
                        onOpenSection = { id -> openLesson(c, LessonRoute(id)) },
                    )
                }
            }
            composable<WordPowerRoute> {
                WithChild(child) { c ->
                    val words: WordPowerViewModel = viewModel(
                        key = "words-${c.id}-${c.classGrade}",
                        factory = WordPowerViewModel.factory(c.id, c.classGrade, container.content, container.progress),
                    )
                    val wordsState by words.uiState.collectAsStateWithLifecycle()
                    WordPowerScreen(
                        state = wordsState,
                        onBack = dropUnlessResumed { nav.popBackStack() },
                        onOpenLesson = { id -> openLesson(c, LessonRoute(id)) },
                    )
                }
            }
            composable<ExpressRoute> {
                WithChild(child) { c ->
                    val express: ExpressViewModel = viewModel(
                        key = "express-${c.id}",
                        factory = ExpressViewModel.factory(c.id, container.content, container.progress),
                    )
                    val expressState by express.uiState.collectAsStateWithLifecycle()
                    ExpressScreen(
                        state = expressState,
                        onBack = dropUnlessResumed { nav.popBackStack() },
                        onPlay = { id, challenge -> openTimed(c, ExpressRoundRoute(id, challenge)) },
                    )
                }
            }
            composable<ExpressRoundRoute> { entry ->
                val route = entry.toRoute<ExpressRoundRoute>()
                WithChild(child) { c ->
                    ExpressGameScreen(
                        container = container,
                        childId = c.id,
                        passageId = route.passageId,
                        challenge = route.challenge,
                        onExit = dropUnlessResumed { nav.popBackStack() },
                        onNext = { next -> openTimed(c, ExpressRoundRoute(next, route.challenge), replaceCurrent = true) },
                    )
                }
            }
            composable<ReadingRoute> {
                WithChild(child) { c ->
                    val journey: JourneyViewModel = viewModel(
                        key = "journey-${c.id}",
                        factory = JourneyViewModel.factory(c.id, container.content, container.progress, container.parent, c.classGrade),
                    )
                    val journeyState by journey.uiState.collectAsStateWithLifecycle()
                    ReadingScreen(
                        journey = journeyState,
                        onBack = dropUnlessResumed { nav.popBackStack() },
                        onOpenPassage = { id -> openLesson(c, LessonRoute(id)) },
                    )
                }
            }
            composable<ParentRoute> {
                ParentArea(
                    container = container,
                    children = ready.children,
                    currentChild = child,
                    onClose = dropUnlessResumed { nav.popBackStack() },
                    onKidModeStarted = { id ->
                        viewModel.setCurrentChild(id)
                        nav.startFresh(HomeRoute)
                    },
                    onChildDeleted = { deleted ->
                        if (deleted == child?.id) {
                            val others = ready.children.filter { it.id != deleted }
                            val only = others.singleOrNull()
                            when {
                                others.isEmpty() -> nav.startFresh(ProfileSetupRoute)
                                only == null -> nav.startFresh(ProfilePickerRoute)
                                else -> {
                                    viewModel.setCurrentChild(only.id)
                                    nav.startFresh(if (only.placementDone) HomeRoute else PlacementRoute(only.id))
                                }
                            }
                        }
                    },
                )
            }
            composable<TimeUpRoute> {
                WithChild(child) { c ->
                    TimeUpScreen(
                        settings = container.settings,
                        onHome = dropUnlessResumed { nav.popBackStack<HomeRoute>(inclusive = false) },
                        onExtraTime = {
                            scope.launch {
                                container.parent.addExtraTime(c.id, EXTRA_MINUTES)
                                nav.popBackStack<HomeRoute>(inclusive = false)
                            }
                        },
                    )
                }
            }
            composable<SettingsRoute> {
                SettingsScreen(
                    current = language,
                    onLanguageChange = viewModel::setLanguage,
                    englishVoice = englishVoice,
                    onEnglishVoiceChange = viewModel::setEnglishVoice,
                    chimesOn = chimesOn,
                    onChimesChange = viewModel::setChimesOn,
                    onBack = dropUnlessResumed { nav.popBackStack() },
                )
            }
            composable<LevelRoute> { entry ->
                val number = entry.toRoute<LevelRoute>().number
                WithChild(child) { c ->
                    val journey: JourneyViewModel = viewModel(
                        key = "journey-${c.id}",
                        factory = JourneyViewModel.factory(c.id, container.content, container.progress, container.parent, c.classGrade),
                    )
                    val journeyState by journey.uiState.collectAsStateWithLifecycle()
                    journeyState.levels.firstOrNull { it.number == number }?.let { level ->
                        LevelScreen(
                            level = level,
                            journey = journeyState,
                            onBack = dropUnlessResumed { nav.popBackStack() },
                            onOpenLesson = { id -> openLesson(c, LessonRoute(id)) },
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
                        onNextLesson = { next -> openLesson(c, LessonRoute(next), replaceCurrent = true) },
                    )
                }
            }
        }
    }
    }
    }
}

private const val EXTRA_MINUTES = 15

/** Screens that belong to a child only render once one is chosen. */
@Composable
private fun WithChild(child: Child?, content: @Composable (Child) -> Unit) {
    if (child != null) content(child)
}

/** Go to [route] and clear everything behind it (used after setup steps the child shouldn't go back to). */
private fun NavHostController.startFresh(route: Any) {
    navigate(route) { popUpTo(graph.id) { inclusive = true } }
}
