package com.samjhoenglish.ui.test

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samjhoenglish.AppContainer
import com.samjhoenglish.content.Step
import com.samjhoenglish.content.TestQuestions
import com.samjhoenglish.data.db.Child
import com.samjhoenglish.data.db.SchoolTest
import com.samjhoenglish.data.db.TestAttempt
import com.samjhoenglish.i18n.UiStrings
import com.samjhoenglish.i18n.text
import com.samjhoenglish.ui.components.EmojiButton
import com.samjhoenglish.ui.theme.BowRed
import com.samjhoenglish.ui.theme.Leaf
import com.samjhoenglish.ui.theme.Sunny

/** Home: one card per test still to take. Tapping opens the test. */
@Composable
fun ActiveTestCards(tests: List<SchoolTest>, onOpen: (Long) -> Unit) {
    tests.forEach { test ->
        val questions = remember(test.questions) { test.questions.count { it == '[' } - 1 }
        Surface(
            onClick = { onOpen(test.testId) },
            shape = RoundedCornerShape(28.dp),
            color = Sunny,
            shadowElevation = 3.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(UiStrings.activeTests.text(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(test.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "${test.kindLabel().text()} · ${UiStrings.testCardInfo.text().format(questions, test.minutes, shortDate(test.closesAt))}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

/**
 * The parent's list of handed-in school tests, and each test's questions with the child's answer
 * beside the right one, so they can help with the weak parts.
 */
@Composable
fun ParentTestResultsScreen(container: AppContainer, child: Child, onBack: () -> Unit) {
    val results by container.school.results(child.id).collectAsStateWithLifecycle(emptyList())
    var open by remember { mutableStateOf<TestAttempt?>(null) }
    val showing = open
    if (showing != null) {
        ParentTestDetail(container, showing, onBack = { open = null })
        return
    }
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            EmojiButton(emoji = "⬅️", label = UiStrings.back.text(), onClick = onBack)
            Text(UiStrings.parentTestResults.text(), style = MaterialTheme.typography.headlineSmall)
        }
        Text("${child.avatar} ${child.name}", style = MaterialTheme.typography.titleLarge)
        Text(UiStrings.parentTestResultsText.text(), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (results.isEmpty()) Text(UiStrings.parentNoTests.text(), style = MaterialTheme.typography.titleMedium)
        results.forEach { a ->
            val percent = if (a.total > 0) a.score * 100 / a.total else 0
            Surface(
                onClick = { open = a },
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(a.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${(if (a.kind == "book") UiStrings.bookTestKind else UiStrings.courseTestKind).text()} · ${shortDate(a.submittedAt ?: a.startedAt)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        "${a.score}/${a.total}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (percent >= 60) Leaf else BowRed,
                    )
                }
            }
        }
    }
}

@Composable
private fun ParentTestDetail(container: AppContainer, attempt: TestAttempt, onBack: () -> Unit) {
    val answers = remember(attempt) { container.school.answersOf(attempt) }
    var steps by remember { mutableStateOf<List<Pair<String, Step>>>(emptyList()) }
    LaunchedEffect(attempt) { steps = container.school.stepsOf(answers) }
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            EmojiButton(emoji = "⬅️", label = UiStrings.back.text(), onClick = onBack)
            Text(attempt.title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
        }
        Text(
            UiStrings.testScore.text().format(attempt.score, attempt.total),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        // The wrong ones first in a short list, so the parent sees at a glance what to work on.
        val byId = steps.associate { (_, s) -> s.id to s }
        val wrong = answers.filterNot { it.ok }.mapNotNull { byId[it.s]?.let(TestQuestions::describe) }
        if (wrong.isNotEmpty()) {
            Surface(shape = RoundedCornerShape(16.dp), color = BowRed.copy(alpha = 0.08f), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("🎯 ${UiStrings.testsToFocus.text()}", style = MaterialTheme.typography.titleMedium)
                    wrong.forEach { Text("• ${it.question} → ${it.answer}", style = MaterialTheme.typography.bodyLarge) }
                }
            }
        }
        TestReview(steps, answers)
    }
}
