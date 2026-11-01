package com.samjhoenglish.ui.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samjhoenglish.data.ProgressRepository
import com.samjhoenglish.data.db.Child
import com.samjhoenglish.i18n.L
import com.samjhoenglish.i18n.UiStrings
import com.samjhoenglish.i18n.text
import com.samjhoenglish.speech.rememberSay
import com.samjhoenglish.ui.components.BigButton
import com.samjhoenglish.ui.components.TinkuSays
import kotlinx.coroutines.launch

/** Buddies a child can pick. (No parrot: "tota ratna" is the rote learning we avoid.) */
private val avatars = listOf("🐯", "🦁", "🐼", "🐵", "🦊", "🐰", "🐘", "🐶")

/** First-time setup for a child: name, buddy, class. Big targets, every question spoken. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileSetupScreen(progress: ProgressRepository, onCreated: (Long) -> Unit) {
    val say = rememberSay()
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf("") }
    var avatar by rememberSaveable { mutableStateOf(avatars.first()) }
    var classGrade by rememberSaveable { mutableIntStateOf(0) }
    var saving by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) { say(UiStrings.setupHello) }

    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TinkuSays(message = UiStrings.setupHello.text(), onListen = { say(UiStrings.setupHello) }, tinkuSize = 110.dp)

        Spacer(Modifier.height(24.dp))
        Question(UiStrings.whatsYourName, onListen = { say(UiStrings.whatsYourName) })
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(20) },
            placeholder = { Text(UiStrings.typeName.text(), style = MaterialTheme.typography.titleLarge) },
            textStyle = MaterialTheme.typography.headlineSmall,
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(24.dp))
        Question(UiStrings.pickBuddy, onListen = { say(UiStrings.pickBuddy) })
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            avatars.forEach { option ->
                ChoiceBubble(selected = option == avatar, onClick = { avatar = option }) {
                    Text(option, fontSize = 34.sp)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Question(UiStrings.whichClass, onListen = { say(UiStrings.whichClass) })
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            (1..8).forEach { grade ->
                ChoiceBubble(selected = grade == classGrade, onClick = { classGrade = grade }) {
                    Text("$grade", style = MaterialTheme.typography.headlineMedium)
                }
            }
        }

        Spacer(Modifier.height(28.dp))
        BigButton(
            text = UiStrings.letsGo.text(),
            emoji = "🚀",
            enabled = name.isNotBlank() && classGrade > 0 && !saving,
            onClick = {
                saving = true
                scope.launch { onCreated(progress.addChild(name.trim(), avatar, classGrade)) }
            },
        )
    }
}

/** "Who is learning today?" — shown when several children share the phone. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfilePickerScreen(children: List<Child>, onPick: (Long) -> Unit, onAddChild: () -> Unit) {
    val say = rememberSay()
    LaunchedEffect(Unit) { say(UiStrings.whoIsLearning) }

    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TinkuSays(message = UiStrings.whoIsLearning.text(), onListen = { say(UiStrings.whoIsLearning) }, tinkuSize = 110.dp)
        Spacer(Modifier.height(24.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            children.forEach { child ->
                Surface(
                    onClick = { onPick(child.id) },
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
                    shadowElevation = 2.dp,
                    modifier = Modifier.size(width = 140.dp, height = 150.dp),
                ) {
                    Column(
                        Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(child.avatar, fontSize = 56.sp)
                        Text(child.name, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, maxLines = 1)
                    }
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        BigButton(text = UiStrings.addChild.text(), emoji = "➕", color = MaterialTheme.colorScheme.secondary, onClick = onAddChild)
    }
}

@Composable
private fun Question(text: L, onListen: () -> Unit) {
    Surface(onClick = onListen, color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth()) {
        Text(
            "🔊 ${text.text()}",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 10.dp),
        )
    }
}

@Composable
private fun ChoiceBubble(selected: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) colors.primaryContainer else colors.surface,
        border = BorderStroke(if (selected) 3.dp else 2.dp, if (selected) colors.primary else colors.outline),
        modifier = Modifier.size(64.dp),
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}
