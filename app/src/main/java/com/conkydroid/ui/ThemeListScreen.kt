package com.conkydroid.ui

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.conkydroid.theme.Theme
import com.conkydroid.theme.ThemeRepository

@Composable
fun ThemeListScreen(
    themes: List<Theme>,
    activeTheme: Theme?,
    onSelect: (Theme) -> Unit,
    onEdit: (Theme) -> Unit,
    onSettings: () -> Unit,
    repo: ThemeRepository,
    onRefresh: () -> Unit,
) {
    var showNewDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf<Theme?>(null) }
    var showDuplicateDialog by remember { mutableStateOf<Theme?>(null) }

    val context = LocalContext.current

    val currentOnRefresh by rememberUpdatedState(onRefresh)
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                if (json != null) {
                    val theme = repo.importJson(json)
                    if (theme != null) {
                        repo.save(theme.name, json)
                        currentOnRefresh()
                        Toast.makeText(context, "Imported \"${theme.name}\"", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Invalid theme JSON", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (_: Exception) {
                Toast.makeText(context, "Import failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Themes", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = { showNewDialog = true },
                modifier = Modifier.weight(1f),
            ) { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("New") }
            Button(
                onClick = { importLauncher.launch("application/json") },
                modifier = Modifier.weight(1f),
            ) { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Import") }
        }

        Spacer(Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(themes) { theme ->
                ThemeCard(
                    theme = theme,
                    isActive = theme.name == activeTheme?.name,
                    isUser = repo.isUserTheme(theme.name),
                    onClick = { onSelect(theme) },
                    onEdit = { onEdit(theme) },
                    onDuplicate = { showDuplicateDialog = theme },
                    onExport = {
                        val json = repo.exportJson(theme)
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, json)
                            putExtra(Intent.EXTRA_SUBJECT, "Theme: ${theme.name}")
                            type = "application/json"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Export Theme"))
                    },
                    onDelete = {
                        if (repo.isUserTheme(theme.name)) showDeleteConfirm = theme
                    },
                )
            }
        }
    }

    if (showNewDialog) {
        NewThemeDialog(
            onDismiss = { showNewDialog = false },
            onConfirm = { name ->
                val t = repo.createNew(name)
                val json = repo.exportJson(t)
                val ok = repo.save(t.name, json)
                showNewDialog = false
                if (ok) {
                    onRefresh()
                    Toast.makeText(context, "Theme \"$name\" created", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to save theme!", Toast.LENGTH_LONG).show()
                }
            },
        )
    }

    showDeleteConfirm?.let { theme ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Delete Theme") },
            text = { Text("Delete \"${theme.name}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    repo.delete(theme.name)
                    onRefresh()
                    showDeleteConfirm = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text("Cancel") }
            },
        )
    }

    showDuplicateDialog?.let { theme ->
        DuplicateThemeDialog(
            originalName = theme.name,
            onDismiss = { showDuplicateDialog = null },
            onConfirm = { newName ->
                val copy = repo.duplicate(theme, newName)
                repo.save(copy.name, repo.exportJson(copy))
                onRefresh()
                showDuplicateDialog = null
            },
        )
    }
}

@Composable
private fun NewThemeDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Theme") },
        text = {
            Column {
                Text("Enter a name for the new theme:")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Theme name") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name.trim()) }) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun DuplicateThemeDialog(
    originalName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf("${originalName} (Copy)") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Duplicate Theme") },
        text = {
            Column {
                Text("Enter a name for the copy:")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("New name") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name.trim()) }) { Text("Duplicate") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun ThemeCard(
    theme: Theme,
    isActive: Boolean,
    isUser: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(theme.name, style = MaterialTheme.typography.titleMedium)
                    if (isActive) {
                        Text("ACTIVE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
                if (theme.author.isNotBlank()) {
                    Text(theme.author, style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    "${theme.widgets.size} widgets${if (isUser) "  •  user" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDuplicate, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Add, contentDescription = "Duplicate", modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onExport, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Share, contentDescription = "Export", modifier = Modifier.size(18.dp))
            }
            if (isUser) {
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
