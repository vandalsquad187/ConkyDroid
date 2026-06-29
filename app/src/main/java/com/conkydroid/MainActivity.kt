package com.conkydroid

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.conkydroid.overlay.OverlayService
import com.conkydroid.theme.Theme
import com.conkydroid.theme.ThemeHolder
import com.conkydroid.theme.ThemeLoader
import com.conkydroid.theme.ThemeRepository
import com.conkydroid.ui.SettingsScreen
import com.conkydroid.ui.ThemeEditorScreen
import com.conkydroid.ui.ThemeListScreen
import com.conkydroid.ui.ThemeMode

private fun parseHexColor(hex: String?): Int? {
    if (hex == null) return null
    return try { android.graphics.Color.parseColor(hex) } catch (_: Exception) { null }
}

class MainActivity : ComponentActivity() {

    private lateinit var repo: ThemeRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = ThemeRepository(this)
        val prefs = getSharedPreferences("conkydroid", MODE_PRIVATE)

        setContent {
            var screen by remember { mutableStateOf<Screen>(Screen.Main) }
            var themes by remember { mutableStateOf(repo.loadAll()) }
            var themeMode by remember { mutableStateOf(
                ThemeMode.valueOf(prefs.getString("theme", "System") ?: "System")
            ) }
            var overlayAlpha by remember { mutableStateOf(prefs.getFloat("alpha", 1f)) }
            var weatherLat by remember { mutableStateOf(prefs.getString("weather_lat", "51.5") ?: "51.5") }
            var weatherLon by remember { mutableStateOf(prefs.getString("weather_lon", "-0.12") ?: "-0.12") }
            var batterySaver by remember { mutableStateOf(prefs.getBoolean("battery_saver", false)) }

            remember {
                weatherLat.toDoubleOrNull()?.let { com.conkydroid.data.weather.WeatherProvider.latitude = it }
                weatherLon.toDoubleOrNull()?.let { com.conkydroid.data.weather.WeatherProvider.longitude = it }
                com.conkydroid.theme.ThemeHolder.batterySaver = batterySaver
            }

            fun refreshThemes() {
                themes = repo.loadAll()
            }

            val colorScheme = when (themeMode) {
                ThemeMode.Light -> lightColorScheme()
                ThemeMode.Dark -> darkColorScheme()
                ThemeMode.System -> if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
            }

            MaterialTheme(colorScheme = colorScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    when (val s = screen) {
                        Screen.Settings -> {
                            var blockedApps by remember { mutableStateOf(
                                prefs.getStringSet("blocked_apps", emptySet()) ?: emptySet()
                            ) }
                            fun saveBlocked(apps: Set<String>) {
                                blockedApps = apps
                                prefs.edit().putStringSet("blocked_apps", apps).apply()
                            }
                            var visibilityMode by remember { mutableStateOf(
                                prefs.getString("visibility_mode", "disabled") ?: "disabled"
                            ) }
                            fun saveVisibilityMode(mode: String) {
                                visibilityMode = mode
                                prefs.edit().putString("visibility_mode", mode).apply()
                            }
                            var allowedApps by remember { mutableStateOf(
                                prefs.getStringSet("allowed_apps", emptySet()) ?: emptySet()
                            ) }
                            fun saveAllowed(apps: Set<String>) {
                                allowedApps = apps
                                prefs.edit().putStringSet("allowed_apps", apps).apply()
                            }
                            SettingsScreen(
                                onBack = { screen = Screen.Main },
                                themeMode = themeMode,
                                onThemeModeChange = { mode ->
                                    themeMode = mode
                                    prefs.edit().putString("theme", mode.name).apply()
                                },
                                blockedApps = blockedApps,
                                onAddBlocked = { pkg -> saveBlocked(blockedApps + pkg) },
                                onRemoveBlocked = { pkg -> saveBlocked(blockedApps - pkg) },
                                visibilityMode = visibilityMode,
                                onVisibilityModeChange = { saveVisibilityMode(it) },
                                allowedApps = allowedApps,
                                onAddAllowed = { pkg -> saveAllowed(allowedApps + pkg) },
                                onRemoveAllowed = { pkg -> saveAllowed(allowedApps - pkg) },
                                alpha = overlayAlpha,
                                onAlphaChange = { a ->
                                    overlayAlpha = a
                                    prefs.edit().putFloat("alpha", a).apply()
                                    com.conkydroid.theme.ThemeHolder.globalAlpha = a
                                },
                                weatherLat = weatherLat,
                                weatherLon = weatherLon,
                                onWeatherLatChange = { lat ->
                                    weatherLat = lat
                                    prefs.edit().putString("weather_lat", lat).apply()
                                    lat.toDoubleOrNull()?.let { com.conkydroid.data.weather.WeatherProvider.latitude = it }
                                },
                                onWeatherLonChange = { lon ->
                                    weatherLon = lon
                                    prefs.edit().putString("weather_lon", lon).apply()
                                    lon.toDoubleOrNull()?.let { com.conkydroid.data.weather.WeatherProvider.longitude = it }
                                },
                                batterySaver = batterySaver,
                                onBatterySaverChange = { on ->
                                    batterySaver = on
                                    prefs.edit().putBoolean("battery_saver", on).apply()
                                    com.conkydroid.theme.ThemeHolder.batterySaver = on
                                },
                            )
                        }
                        is Screen.EditTheme -> {
                            ThemeEditorScreen(
                                theme = s.theme,
                                onBack = { screen = Screen.Main; refreshThemes() },
                                onSave = { theme ->
                                    repo.save(theme.name, ThemeLoader().toJson(theme))
                                    ThemeHolder.update(theme.widgets, theme.globalScale, parseHexColor(theme.globalColor), theme.globalFont)
                                    refreshThemes()
                                    screen = Screen.Main
                                },
                                onPreview = { theme ->
                                    ThemeHolder.update(theme.widgets, theme.globalScale, parseHexColor(theme.globalColor), theme.globalFont)
                                },
                            )
                        }
                        Screen.Main -> {
                            MainApp(
                                checkPermission = { hasOverlayPermission() },
                                onRequestPermission = { requestOverlayPermission() },
                                onStartOverlay = { startOverlay() },
                                onStopOverlay = { stopOverlay() },
                                themes = themes,
                                repo = repo,
                                onRefresh = { refreshThemes() },
                                onThemeSelected = { theme ->
                                    ThemeHolder.update(theme.widgets, theme.globalScale, parseHexColor(theme.globalColor), theme.globalFont)
                                },
                                onEditTheme = { theme ->
                                    ThemeHolder.update(theme.widgets, theme.globalScale, parseHexColor(theme.globalColor), theme.globalFont)
                                    screen = Screen.EditTheme(theme)
                                },
                                onSavePositions = { theme ->
                                    repo.save(theme.name, ThemeLoader().toJson(theme))
                                    refreshThemes()
                                },
                                onSettings = { screen = Screen.Settings },
                            )
                        }
                    }
                }
            }
        }
    }

    private fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else true
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    private fun startOverlay() {
        startForegroundService(Intent(this, OverlayService::class.java))
    }

    private fun stopOverlay() {
        stopService(Intent(this, OverlayService::class.java))
    }
}

sealed class Screen {
    data object Main : Screen()
    data object Settings : Screen()
    data class EditTheme(val theme: Theme) : Screen()
}

@Composable
fun MainApp(
    checkPermission: () -> Boolean,
    onRequestPermission: () -> Unit,
    onStartOverlay: () -> Unit,
    onStopOverlay: () -> Unit,
    themes: List<Theme>,
    repo: ThemeRepository,
    onRefresh: () -> Unit,
    onThemeSelected: (Theme) -> Unit,
    onEditTheme: (Theme) -> Unit,
    onSavePositions: (Theme) -> Unit,
    onSettings: () -> Unit,
) {
    var activeTheme by remember { mutableStateOf<Theme?>(null) }
    var hasPermission by remember { mutableStateOf(checkPermission()) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = checkPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val isEditing by if (hasPermission && activeTheme != null) ThemeHolder.editMode.collectAsState() else remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (!hasPermission) {
                Spacer(Modifier.height(32.dp))
                Text("Overlay-Berechtigung erforderlich", color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
                Button(onClick = onRequestPermission) { Text("Berechtigung erteilen") }
            } else if (!isEditing) {
                ThemeListScreen(
                    themes = themes,
                    activeTheme = activeTheme,
                    onSelect = { theme ->
                        activeTheme = theme
                        ThemeHolder.setEditMode(false)
                        onThemeSelected(theme)
                    },
                    onEdit = onEditTheme,
                    onSettings = onSettings,
                    repo = repo,
                    onRefresh = onRefresh,
                )
            }
        }

        if (hasPermission) {
            Row(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = onStartOverlay, modifier = Modifier.weight(1f)) { Text("Start") }
                Button(onClick = onStopOverlay, modifier = Modifier.weight(1f)) { Text("Stop") }
                if (activeTheme != null) {
                    Button(
                        onClick = {
                            if (isEditing) {
                                ThemeHolder.setEditMode(false)
                                ThemeHolder.selectWidget(null)
                                activeTheme?.let { onSavePositions(it.copy(widgets = ThemeHolder.widgets.value)) }
                            } else {
                                ThemeHolder.setEditMode(true)
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text(if (isEditing) "\u2713 Save" else "\u270E Edit") }
                }
                Button(onClick = onSettings) { Text("\u2699") }
            }
            if (isEditing) {
                Row(
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(onClick = { ThemeHolder.undo() }, enabled = ThemeHolder.canUndo(), modifier = Modifier.weight(1f)) { Text("\u21A9") }
                    Button(onClick = { ThemeHolder.resetToOriginal() }, modifier = Modifier.weight(1f)) { Text("\u21BA") }
                    Button(onClick = { ThemeHolder.setEditMode(false); ThemeHolder.selectWidget(null) }, modifier = Modifier.weight(1f)) { Text("Cancel") }
                }
            }
        }
    }
}
