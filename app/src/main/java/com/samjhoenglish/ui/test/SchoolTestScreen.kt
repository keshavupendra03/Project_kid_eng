package com.samjhoenglish.ui.test

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.samjhoenglish.AppContainer
import com.samjhoenglish.content.Step
import com.samjhoenglish.content.TestQuestions
import com.samjhoenglish.data.TestAnswer
import com.samjhoenglish.data.db.Child
import com.samjhoenglish.data.db.SchoolTest
import com.samjhoenglish.data.db.TestAttempt
import com.samjhoenglish.i18n.UiStrings
import com.samjhoenglish.i18n.text
import com.samjhoenglish.ui.components.BigButton
import com.samjhoenglish.ui.components.EmojiButton
import com.samjhoenglish.ui.components.Tinku
import com.samjhoenglish.ui.components.TinkuMood
import com.samjhoenglish.ui.lesson.Feedback
import com.samjhoenglish.ui.lesson.LocalAnswerLog
import com.samjhoenglish.ui.lesson.StepContent
import com.samjhoenglish.ui.theme.BowRed
import com.samjhoenglish.ui.theme.Leaf
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

private enum class Phase { LOADING, INTRO, RUNNING, RESULT, CLOSED, BROKEN }

/** Pause after an answer so the child sees their tap register before the next question. */
private const val NEXT_DELAY_MS = 600L

fun SchoolTest.kindLabel() = if (kind == "book") UiStrings.bookTestKind else UiStrings.courseTestKind

fun shortDate(millis: Long): String = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(millis))

/**
 * A school test: an intro, then the questions one by one against the clock (no hints, one try
 * each), then the score with every answer. Saved after each answer, so a closed app carries on
 * where it left off; when the time is up the test is handed in by itself.
 */
@Composable
fun SchoolTestScreen(container: AppContainer, child: Child, testId: Long, onBack: () -> Unit) {
    val school = container.school
    val scope = rememberCoroutineScope()
    var phase by remember { mutableStateOf(Phase.LOADING) }
    var test by remember { mutableStateOf<SchoolTest?>(null) }
    var steps by remember { mutableStateOf<List<Pair<String, Step>>>(emptyList()) }
    var attempt by remember { mutableStateOf<TestAttempt?>(null) }
    var answers by remember { mutableStateOf<List<TestAnswer>>(emptyList()) }
    var timeUp by remember { mutableStateOf(false) }

    fun submit(current: TestAttempt, given: List<TestAnswer>, ranOut: Boolean) {
        // Questions not reached count as not answered.
        val all = given + steps.drop(given.size).map { (lesson, step) -> TestAnswer(lesson, step.id, ok = false) }
        val now = System.currentTimeMillis()
        val done = current.copy(
            submittedAt = now,
            score = all.count { it.ok },
            total = all.size,
            seconds = ((now - current.startedAt) / 1000).coerceIn(0, current.minutes * 60L).toInt(),
            answers = school.encodeAnswers(all),
        )
        attempt = done
        answers = all
        timeUp = ranOut
        phase = Phase.RESULT
        container.appScope.launch {
            school.saveAttempt(done)
            runCatching { school.uploadResults() }
            attempt = school.attempt(child.id, testId) ?: done
        }
    }

    LaunchedEffect(testId) {
        val t = school.test(child.id, testId)
        val saved = school.attempt(child.id, testId)
        val loaded = t?.let { school.testSteps(it) }
        test = t
        attempt = saved
        when {
            saved?.submittedAt != null -> {
                steps = loaded.orEmpty()
                answers = school.answersOf(saved)
                phase = Phase.RESULT
            }
            t == null || loaded == null -> phase = Phase.BROKEN
            saved == null && t.closesAt < System.currentTimeMillis() -> phase = Phase.CLOSED
            else -> {
                steps = loaded
                if (saved == null) {
                    phase = Phase.INTRO
                } else {
                    answers = school.answersOf(saved)
                    phase = Phase.RUNNING
                    // The clock kept running while the app was closed.
                    if (System.currentTimeMillis() >= saved.startedAt + saved.minutes * 60_000L) submit(saved, answers, ranOut = true)
                }
            }
        }
    }

    Box(Modifier.fillMaxSize().safeDrawingPadding()) {
        when (phase) {
            Phase.LOADING -> Unit
            Phase.BROKEN -> Message(UiStrings.testNeedsUpdate.text(), onBack)
            Phase.CLOSED -> Message(UiStrings.testClosed.text(), onBack)
            Phase.INTRO -> Intro(test!!, steps.size, onBack) {
                val started = TestAttempt(
                    childId = child.id, testId = testId, kind = test!!.kind, title = test!!.title, minutes = test!!.minutes,
                    startedAt = System.currentTimeMillis(), total = steps.size,
                )
                attempt = started
                answers = emptyList()
                phase = Phase.RUNNING
                scope.launch { school.saveAttempt(started) }
            }
            Phase.RUNNING -> Running(
                attempt = attempt!!,
                steps = steps,
                answered = answers,
                onAnswer = { answer ->
                    val next = answers + answer
                    answers = next
                    val saved = attempt!!.copy(answers = school.encodeAnswers(next))
                    attempt = saved
                    if (next.size >= steps.size) submit(saved, next, ranOut = false) else scope.launch { school.saveAttempt(saved) }
                },
                onTimeUp = { submit(attempt!!, answers, ranOut = true) },
                onLeave = onBack,
            )
            Phase.RESULT -> {
                val a = attempt
                if (a != null) {
                    Result(a, steps, answers, timeUp, onBack)
                }
            }
        }
    }
}

@Composable
private fun Message(text: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Tinku(mood = TinkuMood.CONFUSED, size = 120.dp)
        Spacer(Modifier.height(16.dp))
        Text(text, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        BigButton(text = UiStrings.back.text(), emoji = "⬅️", onClick = onBack)
    }
}

@Composable
private fun Intro(test: SchoolTest, questions: Int, onBack: () -> Unit, onStart: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth()) { EmojiButton(emoji = "⬅️", label = UiStrings.back.text(), onClick = onBack) }
        Tinku(mood = TinkuMood.HAPPY, size = 120.dp)
        Text(test.kindLabel().text(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Text(test.title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(UiStrings.testIntro.text().format(questions, test.minutes), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        BigButton(text = UiStrings.testStart.text(), emoji = "🚀", onClick = onStart, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun Running(
    attempt: TestAttempt,
    steps: List<Pair<String, Step>>,
    answered: List<TestAnswer>,
    onAnswer: (TestAnswer) -> Unit,
    onTimeUp: () -> Unit,
    onLeave: () -> Unit,
) {
    val index = answered.size
    val deadline = attempt.startedAt + attempt.minutes * 60_000L
    var left by remember { mutableLongStateOf(deadline - System.currentTimeMillis()) }
    var leaving by remember { mutableStateOf(false) }
    var waiting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(deadline) {
        while (true) {
            left = deadline - System.currentTimeMillis()
            if (left <= 0) {
                onTimeUp()
                return@LaunchedEffect
            }
            delay(1000)
        }
    }
    BackHandler { leaving = true }

    val (lessonId, step) = steps.getOrNull(index) ?: return
    // For this question: what the child last picked, and how many wrong tries (match, sort, crossword).
    var given by remember(index) { mutableStateOf("") }
    var wrongTries by remember(index) { mutableIntStateOf(0) }

    fun record(ok: Boolean, answer: String) {
        if (waiting) return
        waiting = true
        scope.launch {
            delay(NEXT_DELAY_MS)
            waiting = false
            onAnswer(TestAnswer(lessonId, step.id, ok, answer))
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            EmojiButton(emoji = "✖️", label = UiStrings.testLeave.text(), onClick = { leaving = true })
            Text(
                UiStrings.testQuestionOf.text().format(index + 1, steps.size),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            val seconds = (left / 1000).coerceAtLeast(0)
            Surface(shape = RoundedCornerShape(50), color = if (seconds < 60) BowRed else MaterialTheme.colorScheme.primaryContainer) {
                Text(
                    "⏱️ %d:%02d".format(seconds / 60, seconds % 60),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (seconds < 60) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }
        }
        LinearProgressIndicator(progress = { index.toFloat() / steps.size }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))

        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            key(index) {
                CompositionLocalProvider(LocalAnswerLog provides { given = it }) {
                    StepContent(
                        step = step,
                        feedback = Feedback.NONE,
                        onAnswer = { correct, _ ->
                            when {
                                correct -> record(ok = wrongTries == 0, answer = if (wrongTries == 0) given else "$wrongTries ✗")
                                // Match, sort and crossword: a slip counts, but the child finishes the activity.
                                TestQuestions.hasParts(step) -> wrongTries++
                                else -> record(ok = false, answer = given)
                            }
                        },
                        onSkip = { record(ok = false, answer = "") },
                    )
                }
            }
        }
        TextButton(onClick = { record(ok = false, answer = "") }, modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp)) {
            Text("⏭️ ${UiStrings.testSkip.text()}", style = MaterialTheme.typography.titleMedium)
        }
    }

    if (leaving) {
        AlertDialog(
            onDismissRequest = { leaving = false },
            text = { Text(UiStrings.testExitConfirm.text(), style = MaterialTheme.typography.titleMedium) },
            confirmButton = { TextButton(onClick = { leaving = false }) { Text(UiStrings.testStay.text(), style = MaterialTheme.typography.titleMedium) } },
            dismissButton = { TextButton(onClick = { leaving = false; onLeave() }) { Text(UiStrings.testLeave.text(), style = MaterialTheme.typography.titleMedium) } },
        )
    }
}

@Composable
private fun Result(attempt: TestAttempt, steps: List<Pair<String, Step>>, answers: List<TestAnswer>, timeUp: Boolean, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        val good = attempt.total > 0 && attempt.score * 100 / attempt.total >= 60
        Tinku(mood = if (good) TinkuMood.CHEER else TinkuMood.HAPPY, size = 110.dp)
        Text(
            if (timeUp) UiStrings.testTimeUp.text() else UiStrings.testHandedIn.text(),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(attempt.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            UiStrings.testScore.text().format(attempt.score, attempt.total),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = if (good) Leaf else MaterialTheme.colorScheme.primary,
        )
        Text(
            if (attempt.uploaded) UiStrings.testSent.text() else UiStrings.testWillSend.text(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        BigButton(text = UiStrings.back.text(), emoji = "🏠", onClick = onBack, modifier = Modifier.fillMaxWidth())
        TestReview(steps, answers)
    }
}

/** Every question: ✅/❌, the question, the right answer and (if wrong) what the child answered. */
@Composable
fun TestReview(steps: List<Pair<String, Step>>, answers: List<TestAnswer>) {
    val byId = steps.associate { (_, step) -> step.id to step }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        answers.forEachIndexed { i, answer ->
            val shown = byId[answer.s]?.let(TestQuestions::describe)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (answer.ok) Leaf.copy(alpha = 0.10f) else BowRed.copy(alpha = 0.08f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${i + 1}. ${if (answer.ok) "✅" else "❌"} ${shown?.question ?: answer.s}", style = MaterialTheme.typography.titleMedium)
                    shown?.let { Text("${UiStrings.testRightAnswer.text()}: ${it.answer}", style = MaterialTheme.typography.bodyLarge, color = Leaf) }
                    if (!answer.ok) {
                        Text(
                            "${UiStrings.testYourAnswer.text()}: ${answer.given.ifBlank { UiStrings.testNoAnswer.text() }}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = BowRed,
                        )
                    }
                }
            }
        }
    }
}
