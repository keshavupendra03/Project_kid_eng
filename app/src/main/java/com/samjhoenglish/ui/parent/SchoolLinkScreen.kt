package com.samjhoenglish.ui.parent

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samjhoenglish.AppContainer
import com.samjhoenglish.data.LinkResult
import com.samjhoenglish.data.db.Child
import com.samjhoenglish.data.db.SchoolLink
import com.samjhoenglish.i18n.L
import com.samjhoenglish.i18n.UiStrings
import com.samjhoenglish.i18n.text
import com.samjhoenglish.ui.components.BigButton
import com.samjhoenglish.ui.components.EmojiButton
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/** Where a parent gives consent and connects a child to their school, or disconnects. */
@Composable
fun SchoolLinkScreen(container: AppContainer, child: Child, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val link by remember(child.id) { container.school.link(child.id) }.collectAsStateWithLifecycle(initialValue = null)

    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            EmojiButton(emoji = "⬅️", label = UiStrings.back.text(), onClick = onBack)
            Text(UiStrings.school.text(), style = MaterialTheme.typography.headlineSmall)
        }
        Text("${child.avatar} ${child.name}", style = MaterialTheme.typography.titleLarge)

        val current = link
        if (current != null) Connected(container, child, current) else Connect(container, child)
    }
}

@Composable
private fun Connect(container: AppContainer, child: Child) {
    var code by remember { mutableStateOf("") }
    var agreed by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var problem by remember { mutableStateOf<L?>(null) }

    OutlinedTextField(
        value = code,
        onValueChange = { code = it.uppercase().take(12) },
        label = { Text(UiStrings.schoolCodeLabel.text()) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
        textStyle = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.fillMaxWidth(),
    )
    Text(UiStrings.schoolConsentTitle.text(), style = MaterialTheme.typography.titleLarge)
    Text(UiStrings.schoolShared.text(), style = MaterialTheme.typography.bodyLarge)
    Text(UiStrings.schoolNotShared.text(), style = MaterialTheme.typography.bodyLarge)
    Text(UiStrings.schoolStopAnytime.text(), style = MaterialTheme.typography.bodyLarge)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = agreed, onCheckedChange = { agreed = it })
        Text(UiStrings.schoolConsentCheck.text(), style = MaterialTheme.typography.bodyLarge)
    }
    problem?.let { Text(it.text(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }
    BigButton(
        text = UiStrings.schoolConnect.text(),
        emoji = "🏫",
        enabled = agreed && code.length >= 8 && !busy,
        onClick = {
            busy = true
            problem = null
            // Not this screen's scope: it closes as soon as the link is saved, which would cancel the first send.
            container.appScope.launch {
                problem = when (container.school.connect(child.id, code.trim())) {
                    is LinkResult.Linked -> null
                    LinkResult.WrongCode -> UiStrings.schoolWrongCode
                    LinkResult.TooManyTries -> UiStrings.schoolTooManyTries
                    LinkResult.NoConnection -> UiStrings.schoolNoConnection
                }
                busy = false
            }
        },
    )
}

@Composable
private fun Connected(container: AppContainer, child: Child, link: SchoolLink) {
    val scope = rememberCoroutineScope()
    var confirming by remember { mutableStateOf(false) }
    var problem by remember { mutableStateOf<L?>(null) }

    Text(
        UiStrings.schoolConnected.text().format(link.studentName, link.schoolName, "${link.grade} ${link.section}"),
        style = MaterialTheme.typography.titleLarge,
    )
    val sent = if (link.lastSyncAt == 0L) UiStrings.schoolNever.text() else DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(link.lastSyncAt))
    Text("${UiStrings.schoolLastSent.text()}: $sent", style = MaterialTheme.typography.bodyLarge)
    Text(UiStrings.schoolShared.text(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(UiStrings.schoolNotShared.text(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    problem?.let { Text(it.text(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }
    BigButton(
        text = UiStrings.schoolSendNow.text(),
        emoji = "📤",
        onClick = {
            problem = null
            scope.launch { if (!container.school.sync(child.id, force = true)) problem = UiStrings.schoolNoConnection }
        },
    )
    TextButton(onClick = { confirming = true }) {
        Text(UiStrings.schoolDisconnect.text(), style = MaterialTheme.typography.titleMedium)
    }

    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            text = { Text(UiStrings.schoolDisconnectConfirm.text(), style = MaterialTheme.typography.titleMedium) },
            confirmButton = {
                TextButton(onClick = { confirming = false }) { Text(UiStrings.cancel.text(), style = MaterialTheme.typography.titleMedium) }
            },
            dismissButton = {
                TextButton(onClick = {
                    confirming = false
                    scope.launch { if (!container.school.disconnect(child.id)) problem = UiStrings.schoolNoConnection }
                }) { Text(UiStrings.schoolDisconnect.text(), style = MaterialTheme.typography.titleMedium) }
            },
        )
    }
}
