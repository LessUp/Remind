package app.lessup.remind.ui.settings

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.io.ByteArrayInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import kotlin.text.Charsets
import app.lessup.remind.R
import app.lessup.remind.data.settings.ThemeMode

@Composable
fun SettingsScreen(padding: PaddingValues, vm: SettingsViewModel = hiltViewModel()) {
    val state by vm.uiState.collectAsState()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var itemsCsv by remember { mutableStateOf<String?>(null) }
    var subsCsv by remember { mutableStateOf<String?>(null) }
    var backupPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var snoozeSlider by remember(state.snoozeMinutes) { mutableFloatStateOf(state.snoozeMinutes.toFloat()) }

    val itemsExporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        val content = itemsCsv ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            ctx.contentResolver.openOutputStream(uri)?.use { os ->
                os.write(content.toByteArray(Charsets.UTF_8))
            }
            Toast.makeText(ctx, ctx.getString(R.string.settings_export_items_success), Toast.LENGTH_SHORT).show()
        }
        itemsCsv = null
    }
    val subsExporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        val content = subsCsv ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            ctx.contentResolver.openOutputStream(uri)?.use { os ->
                os.write(content.toByteArray(Charsets.UTF_8))
            }
            Toast.makeText(ctx, ctx.getString(R.string.settings_export_subs_success), Toast.LENGTH_SHORT).show()
        }
        subsCsv = null
    }
    val itemsImporter = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = runCatching {
                val bytes = withContext(Dispatchers.IO) {
                    ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                } ?: throw IllegalArgumentException(ctx.getString(R.string.settings_error_read_file))
                vm.importItemsCsv(ByteArrayInputStream(bytes))
            }
            result.onSuccess {
                Toast.makeText(ctx, ctx.getString(R.string.settings_import_items_success, it), Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(
                    ctx,
                    ctx.getString(R.string.settings_import_items_failure, it.localizedMessage ?: it.message ?: it.javaClass.simpleName),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    val subsImporter = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = runCatching {
                val bytes = withContext(Dispatchers.IO) {
                    ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                } ?: throw IllegalArgumentException(ctx.getString(R.string.settings_error_read_file))
                vm.importSubsCsv(ByteArrayInputStream(bytes))
            }
            result.onSuccess {
                Toast.makeText(ctx, ctx.getString(R.string.settings_import_subs_success, it), Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(
                    ctx,
                    ctx.getString(R.string.settings_import_subs_failure, it.localizedMessage ?: it.message ?: it.javaClass.simpleName),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    val backupExporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = runCatching {
                val bytes = vm.exportBackup(backupPassword)
                withContext(Dispatchers.IO) {
                    ctx.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                        ?: throw IllegalArgumentException(ctx.getString(R.string.settings_error_write_file))
                }
            }
            result.onSuccess {
                Toast.makeText(ctx, ctx.getString(R.string.settings_backup_export_success), Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(
                    ctx,
                    ctx.getString(R.string.settings_backup_export_failure, it.localizedMessage ?: it.message ?: it.javaClass.simpleName),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    val backupImporter = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = runCatching {
                val bytes = withContext(Dispatchers.IO) {
                    ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                } ?: throw IllegalArgumentException(ctx.getString(R.string.settings_error_read_file))
                vm.importBackup(bytes, backupPassword)
            }
            result.onSuccess {
                Toast.makeText(ctx, ctx.getString(R.string.settings_backup_import_success), Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(
                    ctx,
                    ctx.getString(R.string.settings_backup_import_failure, it.localizedMessage ?: it.message ?: it.javaClass.simpleName),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = stringResource(R.string.settings_title), style = MaterialTheme.typography.titleLarge)

        Text(text = stringResource(R.string.settings_threshold_title), style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ThresholdButton(text = "3", selected = state.threshold == 3) { vm.setThreshold(3) }
            ThresholdButton(text = "7", selected = state.threshold == 7) { vm.setThreshold(7) }
            ThresholdButton(text = "14", selected = state.threshold == 14) { vm.setThreshold(14) }
        }

        Text(text = stringResource(R.string.settings_reminder_time_title), style = MaterialTheme.typography.titleMedium)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = {
                TimePickerDialog(ctx, { _, h, m -> vm.setReminderTime(h, m) }, state.reminderHour, state.reminderMinute, true).show()
            }) {
                Text(String.format("%02d:%02d", state.reminderHour, state.reminderMinute))
            }
            Text(text = stringResource(R.string.settings_reminder_time_hint), style = MaterialTheme.typography.bodySmall)
        }

        SettingSwitchRow(
            title = stringResource(R.string.settings_notifications_title),
            description = stringResource(R.string.settings_notifications_desc),
            checked = state.notificationsEnabled,
            onCheckedChange = vm::setNotificationsEnabled
        )
        SettingSwitchRow(
            title = stringResource(R.string.settings_item_notifications_title),
            description = stringResource(R.string.settings_item_notifications_desc),
            checked = state.itemRemindersEnabled,
            onCheckedChange = vm::setItemRemindersEnabled
        )
        SettingSwitchRow(
            title = stringResource(R.string.settings_sub_notifications_title),
            description = stringResource(R.string.settings_sub_notifications_desc),
            checked = state.subRemindersEnabled,
            onCheckedChange = vm::setSubRemindersEnabled
        )
        SettingSwitchRow(
            title = stringResource(R.string.settings_overview_notifications_title),
            description = stringResource(R.string.settings_overview_notifications_desc),
            checked = state.dailyOverviewEnabled,
            onCheckedChange = vm::setDailyOverviewEnabled
        )

        Text(text = stringResource(R.string.settings_snooze_title, state.snoozeMinutes), style = MaterialTheme.typography.titleMedium)
        Slider(
            value = snoozeSlider,
            onValueChange = { value ->
                snoozeSlider = value
            },
            onValueChangeFinished = {
                val minutes = snoozeSlider.roundToInt().coerceIn(5, 180)
                snoozeSlider = minutes.toFloat()
                vm.setSnoozeMinutes(minutes)
            },
            valueRange = 5f..180f,
            steps = 34
        )
        Text(text = stringResource(R.string.settings_snooze_hint), style = MaterialTheme.typography.bodySmall)

        Text(text = stringResource(R.string.settings_theme_title), style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ThemeModeChip(label = stringResource(R.string.settings_theme_system), selected = state.themeMode == ThemeMode.SYSTEM) {
                vm.setThemeMode(ThemeMode.SYSTEM)
            }
            ThemeModeChip(label = stringResource(R.string.settings_theme_light), selected = state.themeMode == ThemeMode.LIGHT) {
                vm.setThemeMode(ThemeMode.LIGHT)
            }
            ThemeModeChip(label = stringResource(R.string.settings_theme_dark), selected = state.themeMode == ThemeMode.DARK) {
                vm.setThemeMode(ThemeMode.DARK)
            }
        }

        Text(text = stringResource(R.string.settings_data_export_title), style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = {
                scope.launch {
                    itemsCsv = vm.buildItemsCsv()
                    itemsExporter.launch("items.csv")
                }
            }) { Text(stringResource(R.string.settings_export_items_csv)) }
            OutlinedButton(onClick = {
                scope.launch {
                    subsCsv = vm.buildSubsCsv()
                    subsExporter.launch("subscriptions.csv")
                }
            }) { Text(stringResource(R.string.settings_export_subs_csv)) }
        }

        Text(text = stringResource(R.string.settings_data_import_title), style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { itemsImporter.launch(arrayOf("text/csv")) }) {
                Text(stringResource(R.string.settings_import_items_csv))
            }
            OutlinedButton(onClick = { subsImporter.launch(arrayOf("text/csv")) }) {
                Text(stringResource(R.string.settings_import_subs_csv))
            }
        }

        Text(text = stringResource(R.string.settings_backup_title), style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = backupPassword,
            onValueChange = { backupPassword = it.trim() },
            label = { Text(stringResource(R.string.settings_backup_password_label)) },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = stringResource(
                            if (showPassword) R.string.settings_password_hide else R.string.settings_password_show
                        )
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(onClick = { backupImporter.launch(arrayOf("application/octet-stream")) }) {
                Text(stringResource(R.string.settings_backup_import_button))
            }
            OutlinedButton(onClick = { backupExporter.launch("lessup-remind.backup") }) {
                Text(stringResource(R.string.settings_backup_export_button))
            }
        }
        Text(text = stringResource(R.string.settings_backup_hint), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SettingSwitchRow(title: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(text = description, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ThemeModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) }
    )
}

@Composable
private fun ThresholdButton(text: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick) { Text(text) }
    } else {
        OutlinedButton(onClick = onClick) { Text(text) }
    }
}

