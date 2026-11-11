package com.samjhoenglish.ui.parent

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samjhoenglish.AppContainer
import com.samjhoenglish.data.db.Child
import com.samjhoenglish.i18n.L
import com.samjhoenglish.i18n.UiStrings
import com.samjhoenglish.i18n.text
import com.samjhoenglish.kidmode.InstalledApp
import com.samjhoenglish.ui.components.BigButton
import com.samjhoenglish.ui.components.EmojiButton
import com.samjhoenglish.ui.theme.Leaf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Where a parent sets up Kid Mode, chooses the apps to lock, and turns it on or off. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KidModeScreen(
    container: AppContainer,
    children: List<Child>,
    currentChild: Child?,
    onOpeningSettings: () -> Unit,
    onStarted: (Long) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val kidMode = container.kidMode
    val scope = rememberCoroutineScope()
    BackHandler(onBack = onBack)

    val activeChild by container.settings.kidModeChild.collectAsStateWithLifecycle(initialValue = null)
    val locked by container.settings.lockedApps.collectAsStateWithLifecycle(initialValue = emptySet())
    val apps by produceState(emptyList<InstalledApp>()) { value = withContext(Dispatchers.IO) { kidMode.lockableApps() } }
    var chosenChild by remember { mutableStateOf(currentChild?.id ?: children.firstOrNull()?.id) }

    // Re-read permissions whenever the parent comes back from the phone's Settings.
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh++ }
    val usage = remember(refresh) { kidMode.hasUsageAccess() }
    val overlay = remember(refresh) { kidMode.canShowOverApps() }
    val battery = remember(refresh) { kidMode.ignoresBatteryLimits() }
    val notifications = remember(refresh) {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }
    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }

    fun openSettings(intent: Intent) {
        onOpeningSettings()
        runCatching { context.startActivity(intent) }
    }

    val packageUri = Uri.parse("package:${context.packageName}")

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
            Text(UiStrings.kidMode.text(), style = MaterialTheme.typography.headlineSmall)
        }

        val running = activeChild
        if (running != null) {
            val name = children.firstOrNull { it.id == running }?.name.orEmpty()
            Text(UiStrings.kidModeOnFor.text().format(name), style = MaterialTheme.typography.titleLarge, color = Leaf)
            BigButton(text = UiStrings.kidModeStop.text(), emoji = "🔓", onClick = { scope.launch { kidMode.stop() } })
        }

        Text(UiStrings.kidModeSetup.text(), style = MaterialTheme.typography.titleLarge)
        SetupRow(UiStrings.setupUsage, usage) { openSettings(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
        SetupRow(UiStrings.setupOverlay, overlay) { openSettings(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, packageUri)) }
        SetupRow(UiStrings.setupBattery, battery) { openSettings(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, packageUri)) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            SetupRow(UiStrings.setupNotifications, notifications) { askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS) }
        }
        Text(UiStrings.setupAutostart.text(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(Modifier.height(4.dp))
        Text(UiStrings.chooseAppsToLock.text(), style = MaterialTheme.typography.titleLarge)
        Text(UiStrings.alwaysLockedNote.text(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        apps.forEach { app ->
            val isLocked = app.packageName in locked
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(app.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Switch(
                    checked = isLocked,
                    onCheckedChange = { on ->
                        scope.launch { container.settings.setLockedApps(if (on) locked + app.packageName else locked - app.packageName) }
                    },
                )
            }
        }

        if (running == null) {
            Spacer(Modifier.height(4.dp))
            if (children.size > 1) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    children.forEach { c ->
                        val selected = c.id == chosenChild
                        Surface(
                            onClick = { chosenChild = c.id },
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                        ) {
                            Text("${c.avatar} ${c.name}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
                        }
                    }
                }
            }
            val child = chosenChild
            BigButton(
                text = UiStrings.kidModeStart.text(),
                emoji = "📱",
                enabled = usage && overlay && child != null,
                onClick = {
                    if (child != null) {
                        scope.launch {
                            kidMode.start(child)
                            onStarted(child)
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun SetupRow(label: L, done: Boolean, onFix: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("${if (done) "✅" else "⬜"} ${label.text()}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        if (!done) {
            TextButton(onClick = onFix) {
                Text(UiStrings.setupTurnOn.text(), style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}
