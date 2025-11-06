package app.lessup.remind.ui.settings

import android.app.TimePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import java.io.ByteArrayInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.text.Charsets

@Composable
fun SettingsScreen(padding: PaddingValues, vm: SettingsViewModel = hiltViewModel()) {
    val threshold by vm.dueThreshold.collectAsState()
    val hour by vm.reminderHour.collectAsState()
    val minute by vm.reminderMinute.collectAsState()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var itemsCsv by remember { mutableStateOf<String?>(null) }
    var subsCsv by remember { mutableStateOf<String?>(null) }

    val itemsExporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        val content = itemsCsv ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            ctx.contentResolver.openOutputStream(uri)?.use { os ->
                os.write(content.toByteArray(Charsets.UTF_8))
            }
        }
        itemsCsv = null
    }
    val subsExporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        val content = subsCsv ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            ctx.contentResolver.openOutputStream(uri)?.use { os ->
                os.write(content.toByteArray(Charsets.UTF_8))
            }
        }
        subsCsv = null
    }
    val itemsImporter = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) {
                    ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                } ?: throw IllegalArgumentException("无法读取文件")
                val importedCount = vm.importItemsCsv(ByteArrayInputStream(bytes))
                Toast.makeText(ctx, "导入物品成功（$importedCount 条）", Toast.LENGTH_SHORT).show()
            } catch (t: Throwable) {
                Toast.makeText(
                    ctx,
                    "导入物品失败：${t.localizedMessage ?: t.message ?: t.javaClass.simpleName}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    val subsImporter = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) {
                    ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                } ?: throw IllegalArgumentException("无法读取文件")
                val importedCount = vm.importSubsCsv(ByteArrayInputStream(bytes))
                Toast.makeText(ctx, "导入会员成功（$importedCount 条）", Toast.LENGTH_SHORT).show()
            } catch (t: Throwable) {
                Toast.makeText(
                    ctx,
                    "导入会员失败：${t.localizedMessage ?: t.message ?: t.javaClass.simpleName}",
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
        Text(text = "设置", style = MaterialTheme.typography.titleLarge)

        Text(text = "临期阈值（天）", style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ThresholdButton(text = "3", selected = threshold == 3) { vm.setThreshold(3) }
            ThresholdButton(text = "7", selected = threshold == 7) { vm.setThreshold(7) }
            ThresholdButton(text = "14", selected = threshold == 14) { vm.setThreshold(14) }
        }

        Text(text = "提醒时间", style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = {
                TimePickerDialog(ctx, { _, h, m -> vm.setReminderTime(h, m) }, hour, minute, true).show()
            }) {
                Text(String.format("%02d:%02d", hour, minute))
            }
            Text(text = "每日概览通知将在此时间触发（启用提醒后生效）", style = MaterialTheme.typography.bodySmall)
        }

        Text(text = "数据导出", style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = {
                scope.launch {
                    itemsCsv = vm.buildItemsCsv()
                    itemsExporter.launch("items.csv")
                }
            }) { Text("导出物品 CSV") }
            OutlinedButton(onClick = {
                scope.launch {
                    subsCsv = vm.buildSubsCsv()
                    subsExporter.launch("subscriptions.csv")
                }
            }) { Text("导出会员 CSV") }
        }

        Text(text = "数据导入", style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { itemsImporter.launch(arrayOf("text/csv")) }) {
                Text("导入物品 CSV")
            }
            OutlinedButton(onClick = { subsImporter.launch(arrayOf("text/csv")) }) {
                Text("导入会员 CSV")
            }
        }
    }
}

@Composable
private fun ThresholdButton(text: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick) { Text(text) }
    } else {
        OutlinedButton(onClick = onClick) { Text(text) }
    }
}
