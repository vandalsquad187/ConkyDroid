package com.conkydroid.ui

import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

enum class ThemeMode { System, Light, Dark }

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    blockedApps: Set<String>,
    onAddBlocked: (String) -> Unit,
    onRemoveBlocked: (String) -> Unit,
    visibilityMode: String = "disabled",
    onVisibilityModeChange: (String) -> Unit = {},
    allowedApps: Set<String> = emptySet(),
    onAddAllowed: (String) -> Unit = {},
    onRemoveAllowed: (String) -> Unit = {},
    alpha: Float,
    onAlphaChange: (Float) -> Unit,
    weatherLat: String,
    weatherLon: String,
    onWeatherLatChange: (String) -> Unit,
    onWeatherLonChange: (String) -> Unit,
    batterySaver: Boolean = false,
    onBatterySaverChange: (Boolean) -> Unit = {},
) {
    val ctx = LocalContext.current
    val pm = ctx.packageManager

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Settings", style = MaterialTheme.typography.headlineMedium)
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            item {
                Spacer(Modifier.height(8.dp))
                Text("Theme", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        Button(
                            onClick = { onThemeModeChange(mode) },
                            enabled = mode != themeMode,
                            modifier = Modifier.weight(1f),
                        ) { Text(mode.name, maxLines = 1) }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text("Overlay Opacity", style = MaterialTheme.typography.titleMedium)
                Slider(value = alpha, onValueChange = onAlphaChange, valueRange = 0.1f..1f)
                Text("${(alpha * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)

                Spacer(Modifier.height(16.dp))
                Text("Battery Saver", style = MaterialTheme.typography.titleMedium)
                Text("Reduce refresh rate to 5s (saves battery)", style = MaterialTheme.typography.bodySmall)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(if (batterySaver) "On" else "Off")
                    Switch(checked = batterySaver, onCheckedChange = onBatterySaverChange)
                }

                Spacer(Modifier.height(16.dp))
                Text("Permissions", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { ctx.startActivity(android.content.Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }, modifier = Modifier.weight(1f)) { Text("Usage Access") }
                    Button(onClick = { ctx.startActivity(android.content.Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply { data = android.net.Uri.parse("package:${ctx.packageName}") }) }, modifier = Modifier.weight(1f)) { Text("Overlay") }
                }
                Button(onClick = { ctx.startActivity(android.content.Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")) }, modifier = Modifier.fillMaxWidth()) { Text("Notification Access (for Now Playing + Notifications)") }

                Spacer(Modifier.height(16.dp))
                Text("Weather Location", style = MaterialTheme.typography.titleMedium)
                Text("Set lat/lon for weather data ({weather_temp} etc.)", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = weatherLat, onValueChange = onWeatherLatChange, label = { Text("Latitude") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = weatherLon, onValueChange = onWeatherLonChange, label = { Text("Longitude") }, modifier = Modifier.weight(1f))
                }

                Spacer(Modifier.height(16.dp))
                Text("App-Filter", style = MaterialTheme.typography.titleMedium)
                Text("Lege fest, in welchen Apps das Overlay angezeigt wird.", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val modes = listOf("disabled" to "Aus", "blacklist" to "Blacklist", "whitelist" to "Whitelist")
                    for ((value, label) in modes) {
                        Button(
                            onClick = { onVisibilityModeChange(value) },
                            enabled = value != visibilityMode,
                            modifier = Modifier.weight(1f),
                        ) { Text(label, maxLines = 1) }
                    }
                }

                Spacer(Modifier.height(8.dp))

                when (visibilityMode) {
                    "disabled" -> {
                        Text("Overlay wird in allen Apps angezeigt.", style = MaterialTheme.typography.bodyMedium)
                    }
                    "blacklist" -> {
                        Text("Overlay in diesen Apps ausblenden:", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(4.dp))
                        AddAppButton(ctx, pm, "Blacklist") { onAddBlocked(it) }
                        Spacer(Modifier.height(8.dp))
                        AppListDisplay(blockedApps, ctx, pm, "Keine Apps auf der Blacklist.") { onRemoveBlocked(it) }
                    }
                    "whitelist" -> {
                        Text("Overlay nur in diesen Apps anzeigen:", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(4.dp))
                        AddAppButton(ctx, pm, "Whitelist") { onAddAllowed(it) }
                        Spacer(Modifier.height(8.dp))
                        AppListDisplay(allowedApps, ctx, pm, "Keine Apps auf der Whitelist.") { onRemoveAllowed(it) }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text("Theme files location:", style = MaterialTheme.typography.bodySmall)
                Text("Internal Storage/Android/data/com.conkydroid/files/themes/", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun AddAppButton(ctx: Context, pm: PackageManager, listName: String, onAdd: (String) -> Unit) {
    Button(
        onClick = {
            val allApps: List<android.content.pm.ApplicationInfo> = if (android.os.Build.VERSION.SDK_INT >= 33) {
                pm.getInstalledApplications(android.content.pm.PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION") pm.getInstalledApplications(0)
            }
            val apps = allApps
                .map { it.packageName to (pm.getApplicationLabel(it)?.toString() ?: it.packageName) }
                .filterNot { (pkg, _) -> pkg == ctx.packageName }
                .sortedBy { (_, label) -> label }
            val names = apps.map { (pkg, label) -> "$label ($pkg)" }.toTypedArray()
            android.app.AlertDialog.Builder(ctx)
                .setTitle("App zu $listName hinzufügen")
                .setItems(names) { _, which -> onAdd(apps[which].first) }
                .setNegativeButton("Abbrechen", null)
                .create()
                .show()
        },
        modifier = Modifier.fillMaxWidth(),
    ) { Text("+ App hinzufügen") }
}

@Composable
private fun AppListDisplay(
    apps: Set<String>,
    ctx: Context,
    pm: PackageManager,
    emptyText: String,
    onRemove: (String) -> Unit,
) {
    if (apps.isEmpty()) {
        Text(emptyText, style = MaterialTheme.typography.bodyMedium)
    } else {
        val allApps: List<android.content.pm.ApplicationInfo> = if (android.os.Build.VERSION.SDK_INT >= 33) {
            pm.getInstalledApplications(android.content.pm.PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION") pm.getInstalledApplications(0)
        }
        val appLabels = allApps.associate { it.packageName to (pm.getApplicationLabel(it)?.toString() ?: it.packageName) }
        for (pkg in apps.sorted()) {
            val label = appLabels[pkg] ?: pkg
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, modifier = Modifier.weight(1f))
                TextButton(onClick = { onRemove(pkg) }) { Text("Entfernen") }
            }
        }
    }
}
