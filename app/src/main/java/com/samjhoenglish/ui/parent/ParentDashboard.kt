package com.samjhoenglish.ui.parent

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samjhoenglish.AppContainer
import com.samjhoenglish.data.db.Child
import com.samjhoenglish.i18n.AppLanguage
import com.samjhoenglish.i18n.LocalAppLanguage
import com.samjhoenglish.i18n.UiStrings
import com.samjhoenglish.i18n.text
import com.samjhoenglish.ui.components.EmojiButton
import com.samjhoenglish.ui.theme.ChartBar
import com.samjhoenglish.ui.theme.Leaf
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val limitChoices = listOf(0, 15, 30, 45, 60)
private const val DAY_MILLIS = 24 * 60 * 60 * 1000L

/** The whole parent section: asks for the PIN, then shows the dashboard. Locks again if the app is left. */
@Composable
fun ParentArea(
    container: AppContainer,
    children: List<Child>,
    currentChild: Child?,
    onClose: () -> Unit,
    onChildDeleted: (Long) -> Unit,
    onKidModeStarted: (Long) -> Unit,
) {
    var unlocked by remember { mutableStateOf(false) }
    var changingPin by remember { mutableStateOf(false) }
    var showingKidMode by remember { mutableStateOf(false) }
    var openingSettings by remember { mutableStateOf(false) }
    var schoolChild by remember { mutableStateOf<Child?>(null) }

    DisposableEffect(Unit) {
        container.stopAudio()
        container.studyTimer.enterParentArea()
        onDispose { container.studyTimer.leaveParentArea() }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        // Stepping out to the phone's Settings during Kid Mode setup doesn't count as leaving.
        if (openingSettings) {
            openingSettings = false
        } else {
            unlocked = false
            changingPin = false
            showingKidMode = false
            schoolChild = null
        }
    }

    val linking = schoolChild

    when {
        !unlocked -> ParentGate(container.settings, onDone = { unlocked = true }, onClose = onClose)
        changingPin -> ParentGate(
            container.settings,
            createNew = true,
            onDone = { changingPin = false },
            onClose = { changingPin = false },
        )
        linking != null -> SchoolLinkScreen(container, linking, onBack = { schoolChild = null })
        showingKidMode -> KidModeScreen(
            container = container,
            children = children,
            currentChild = currentChild,
            onOpeningSettings = { openingSettings = true },
            onStarted = onKidModeStarted,
            onBack = { showingKidMode = false },
        )
        else -> Dashboard(
            container,
            children,
            currentChild,
            onClose,
            onChangePin = { changingPin = true },
            onOpenKidMode = { showingKidMode = true },
            onOpenSchool = { schoolChild = it },
            onChildDeleted,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Dashboard(
    container: AppContainer,
    children: List<Child>,
    currentChild: Child?,
    onClose: () -> Unit,
    onChangePin: () -> Unit,
    onOpenKidMode: () -> Unit,
    onOpenSchool: (Child) -> Unit,
    onChildDeleted: (Long) -> Unit,
) {
    var selectedId by remember { mutableStateOf(currentChild?.id) }
    val child = children.firstOrNull { it.id == selectedId } ?: currentChild ?: children.firstOrNull() ?: return
    val scope = rememberCoroutineScope()
    var confirmDelete by remember { mutableStateOf(false) }

    val viewModel: ParentViewModel = viewModel(
        key = "parent-${child.id}",
        factory = ParentViewModel.factory(child.id, container.content, container.progress, container.parent),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            EmojiButton(emoji = "⬅️", label = UiStrings.back.text(), onClick = onClose)
            Text(UiStrings.parentTitle.text(), style = MaterialTheme.typography.headlineSmall)
        }

        Surface(
            onClick = onOpenKidMode,
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("📱 ${UiStrings.kidMode.text()}", style = MaterialTheme.typography.titleLarge)
                Text(UiStrings.kidModeCardText.text(), style = MaterialTheme.typography.bodyMedium)
            }
        }

        if (children.size > 1) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                children.forEach { c ->
                    Choice("${c.avatar} ${c.name}", selected = c.id == child.id) { selectedId = c.id }
                }
            }
        } else {
            Text("${child.avatar} ${child.name}", style = MaterialTheme.typography.titleLarge)
        }

        Surface(
            onClick = { onOpenSchool(child) },
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("🏫 ${UiStrings.school.text()}", style = MaterialTheme.typography.titleLarge)
                Text(UiStrings.schoolCardText.text(), style = MaterialTheme.typography.bodyMedium)
            }
        }

        if (!state.loaded) return@Column

        Section(UiStrings.timeSpent.text()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stat("${state.todayMinutes}", "${UiStrings.today.text()} (${UiStrings.minutes.text()})", Modifier.weight(1f))
                Stat("${state.weekMinutes}", "${UiStrings.thisWeek.text()} (${UiStrings.minutes.text()})", Modifier.weight(1f))
                Stat("🔥 ${state.streak}", UiStrings.streak.text(), Modifier.weight(1f))
            }
            Spacer(Modifier.height(14.dp))
            WeekChart(state.days)
        }

        Section(UiStrings.progressTitle.text()) {
            state.levels.forEach { row ->
                LevelProgressRow(row)
                Spacer(Modifier.height(10.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stat("${state.wordsLearned}", UiStrings.wordsLearned.text(), Modifier.weight(1f))
                Stat("${state.sentencesSpoken}", UiStrings.sentencesSpoken.text(), Modifier.weight(1f))
            }
        }

        Section("📚 ${UiStrings.wordPower.text()}") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stat("${state.powerWordsLearned}", UiStrings.wordsLearnedShort.text(), Modifier.weight(1f))
                Stat("${state.powerWordsMastered}", UiStrings.wordsMastered.text(), Modifier.weight(1f))
                Stat(state.lastMonthlyStars?.let { "⭐".repeat(it) } ?: "–", UiStrings.lastMonthlyScore.text(), Modifier.weight(1f))
            }
        }

        Section("📖 ${UiStrings.readingSection.text()}") {
            state.expressBest?.let {
                Note("🚂 ${UiStrings.expressBest.text()}: $it ${UiStrings.wordsPerMinute.text()}")
                Spacer(Modifier.height(8.dp))
            }
            if (state.passagesRead == 0) Note(UiStrings.readingNone.text()) else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Stat("${state.passagesRead}", UiStrings.passagesRead.text(), Modifier.weight(1f))
                    Stat(state.readingAccuracy?.let { "$it%" } ?: "–", UiStrings.readingAccuracy.text(), Modifier.weight(1f))
                    Stat(state.wordsPerMinute?.toString() ?: "–", UiStrings.wordsPerMinute.text(), Modifier.weight(1f))
                }
                state.readingLevel?.let {
                    Spacer(Modifier.height(8.dp))
                    Note("${UiStrings.readingLevel.text()}: $it")
                }
            }
        }

        Section(UiStrings.weakTitle.text()) {
            when {
                !state.hasPractised -> Note(UiStrings.weakTooEarly.text())
                state.weak.isEmpty() -> Note(UiStrings.weakNone.text())
                else -> state.weak.forEachIndexed { i, skill ->
                    if (i > 0) Spacer(Modifier.height(12.dp))
                    Text(skill.name.text(), style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${UiStrings.homeTip.text()} ${skill.tip.text()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Section(UiStrings.missionsTitle.text()) {
            if (state.pendingMissions.isEmpty()) Note(UiStrings.missionsNone.text())
            state.pendingMissions.forEach { mission ->
                Text("${mission.emoji} ${mission.text.text()}", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { viewModel.approveMission(mission.id) }) {
                        Text(UiStrings.missionApprove.text(), style = MaterialTheme.typography.titleMedium)
                    }
                    TextButton(onClick = { viewModel.returnMission(mission.id) }) {
                        Text(UiStrings.missionNotYet.text(), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            Note("${UiStrings.missionsApprovedCount.text()}: ${state.approvedMissions}")
        }

        Section(UiStrings.limitTitle.text()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                limitChoices.forEach { minutes ->
                    Choice(
                        if (minutes == 0) UiStrings.limitOff.text() else "$minutes ${UiStrings.minutes.text()}",
                        selected = minutes == child.dailyLimitMinutes,
                    ) { viewModel.setDailyLimit(minutes) }
                }
            }
            Spacer(Modifier.height(8.dp))
            Note(UiStrings.limitNote.text())
        }

        TextButton(onClick = onChangePin) {
            Text("🔑 ${UiStrings.pinChange.text()}", style = MaterialTheme.typography.titleMedium)
        }
        TextButton(onClick = { confirmDelete = true }) {
            Text("🗑️ ${UiStrings.deleteChild.text()}", style = MaterialTheme.typography.titleMedium)
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            text = { Text(UiStrings.deleteConfirm.text().format(child.name), style = MaterialTheme.typography.titleMedium) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(UiStrings.cancel.text(), style = MaterialTheme.typography.titleMedium)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    scope.launch {
                        // Best effort: if offline, the school can still remove the student itself.
                        container.school.disconnect(child.id)
                        container.parent.deleteChild(child.id)
                        onChildDeleted(child.id)
                    }
                }) {
                    Text(UiStrings.deleteYes.text(), style = MaterialTheme.typography.titleMedium)
                }
            },
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
    }
}

@Composable
private fun Note(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun Choice(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) colors.primaryContainer else colors.surface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) colors.primary else colors.outline),
        modifier = Modifier.heightIn(min = 48.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
        }
    }
}

/** Minutes per day for the last 7 days. One hue, bars on a shared baseline; tap a day to read its value. */
@Composable
private fun WeekChart(days: List<DayBar>) {
    var selected by remember(days.size) { mutableIntStateOf(days.lastIndex) }
    val locale = appLocale()
    val minutesWord = UiStrings.minutes.text()
    val most = days.maxOfOrNull { it.minutes } ?: 0
    val scale = most.coerceAtLeast(1).toFloat()
    val chosen = days.getOrNull(selected) ?: return

    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            "${dayLabel(chosen.day, "EEEE", locale)}: ${chosen.minutes} $minutesWord",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            "${UiStrings.mostInDay.text()}: $most",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth().height(120.dp)) {
        days.forEachIndexed { i, day ->
            val fraction = day.minutes / scale
            val label = dayLabel(day.day, "EEE", locale)
            Column(
                // The whole column is the tap target, much bigger than the bar.
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { selected = i }
                    .semantics { contentDescription = "$label: ${day.minutes} $minutesWord" },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                if (day.minutes > 0) {
                    Box(
                        Modifier
                            .width(24.dp)
                            .fillMaxHeight(fraction.coerceIn(0.03f, 1f))
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(ChartBar),
                    )
                }
            }
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline))
    Spacer(Modifier.height(4.dp))
    Row(Modifier.fillMaxWidth()) {
        days.forEachIndexed { i, day ->
            Text(
                dayLabel(day.day, "EEE", locale),
                fontSize = 12.sp,
                fontWeight = if (i == selected) FontWeight.Bold else FontWeight.Normal,
                color = if (i == selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun LevelProgressRow(row: LevelRow) {
    val total = row.level.lessons.size
    Text(
        "${row.level.emoji} ${UiStrings.level.text()} ${row.level.number}: ${row.level.title.text()}",
        style = MaterialTheme.typography.titleSmall,
    )
    Spacer(Modifier.height(4.dp))
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier
                .weight(1f)
                .height(10.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.outline),
        ) {
            if (row.passed > 0) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(row.passed.toFloat() / total)
                        .clip(RoundedCornerShape(50))
                        .background(Leaf),
                )
            }
        }
        Text(
            "${row.passed}/$total ${UiStrings.lessonsWord.text()}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun appLocale(): Locale =
    if (LocalAppLanguage.current == AppLanguage.HINDI) Locale.forLanguageTag("hi-IN") else Locale.forLanguageTag("en-IN")

private fun dayLabel(day: Long, pattern: String, locale: Locale): String =
    SimpleDateFormat(pattern, locale).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date(day * DAY_MILLIS))
