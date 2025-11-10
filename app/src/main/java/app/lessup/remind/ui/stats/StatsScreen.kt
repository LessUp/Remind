package app.lessup.remind.ui.stats

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import app.lessup.remind.R
import java.util.Locale
import kotlin.text.Charsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun StatsScreen(padding: PaddingValues, vm: StatsViewModel = hiltViewModel()) {
    val stats by vm.stats.collectAsState()
    val monthly by vm.monthlyTrends.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var cachedCsv by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        val data = cachedCsv ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            scope.launch {
                runCatching {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { it.write(data.toByteArray(Charsets.UTF_8)) }
                            ?: throw IllegalStateException(context.getString(R.string.stats_export_error_write))
                    }
                }.onSuccess {
                    Toast.makeText(context, context.getString(R.string.stats_export_success), Toast.LENGTH_SHORT).show()
                }.onFailure {
                    Toast.makeText(
                        context,
                        context.getString(
                            R.string.stats_export_failure,
                            it.localizedMessage ?: it.message ?: it.javaClass.simpleName
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
        cachedCsv = null
    }

    Column(
        modifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = stringResource(R.string.stats_title), style = MaterialTheme.typography.titleLarge)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.stats_items_due, stats.threshold, stats.dueItems7))
                Text(stringResource(R.string.stats_items_expired, stats.expiredItems))
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.stats_subs_due, stats.threshold, stats.dueSubs7))
                Text(stringResource(R.string.stats_subs_expired, stats.expiredSubs))
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.stats_monthly_trend_title), style = MaterialTheme.typography.titleMedium)
                monthly.forEach { trend ->
                    val cost = trend.subscriptionCostCents / 100.0
                    Text(
                        stringResource(
                            R.string.stats_monthly_trend_entry,
                            trend.label,
                            trend.itemsExpiring,
                            trend.overdueItems,
                            trend.subscriptionsEnding,
                            cost
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                OutlinedButton(onClick = {
                    scope.launch {
                        runCatching { vm.buildMonthlyTrendsCsv() }
                            .onSuccess {
                                cachedCsv = it
                                val nowDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                                val fileName = String.format(
                                    Locale.getDefault(),
                                    "monthly-trends-%04d%02d.csv",
                                    nowDate.year,
                                    nowDate.monthNumber
                                )
                                exportLauncher.launch(fileName)
                            }
                            .onFailure {
                                Toast.makeText(
                                    context,
                                    context.getString(
                                        R.string.stats_export_failure,
                                        it.localizedMessage ?: it.message ?: it.javaClass.simpleName
                                    ),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                    }
                }) {
                    Text(stringResource(R.string.stats_export_button))
                }
            }
        }
    }
}
