package app.lessup.remind.ui.subs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import app.lessup.remind.ui.navigation.NavRoutes
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.FilterChip
import java.util.Locale
import app.lessup.remind.R

@Composable
fun SubsScreen(nav: NavController, padding: PaddingValues, vm: SubscriptionsViewModel = hiltViewModel()) {
    val list by vm.subs.collectAsState()
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(emptySet<SubscriptionsViewModel.UiSub.Status>()) }
    var sort by remember { mutableStateOf(SubSort.NEXT_EXPIRY) }
    Scaffold(
        modifier = Modifier.padding(padding),
        floatingActionButton = {
            FloatingActionButton(onClick = { nav.navigate(NavRoutes.SubEdit) }) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.subs_search_label)) }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val allSelected = selected.isEmpty()
                    FilterChip(
                        selected = allSelected,
                        onClick = { selected = emptySet() },
                        label = { Text(stringResource(R.string.subs_filter_all)) }
                    )
                    FilterChip(
                        selected = selected.contains(SubscriptionsViewModel.UiSub.Status.DUE),
                        onClick = {
                            selected = if (selected.contains(SubscriptionsViewModel.UiSub.Status.DUE)) selected - SubscriptionsViewModel.UiSub.Status.DUE else selected + SubscriptionsViewModel.UiSub.Status.DUE
                        },
                        label = { Text(stringResource(R.string.subs_filter_due)) }
                    )
                    FilterChip(
                        selected = selected.contains(SubscriptionsViewModel.UiSub.Status.EXPIRED),
                        onClick = {
                            selected = if (selected.contains(SubscriptionsViewModel.UiSub.Status.EXPIRED)) selected - SubscriptionsViewModel.UiSub.Status.EXPIRED else selected + SubscriptionsViewModel.UiSub.Status.EXPIRED
                        },
                        label = { Text(stringResource(R.string.subs_filter_expired)) }
                    )
                    FilterChip(
                        selected = selected.contains(SubscriptionsViewModel.UiSub.Status.NORMAL),
                        onClick = {
                            selected = if (selected.contains(SubscriptionsViewModel.UiSub.Status.NORMAL)) selected - SubscriptionsViewModel.UiSub.Status.NORMAL else selected + SubscriptionsViewModel.UiSub.Status.NORMAL
                        },
                        label = { Text(stringResource(R.string.subs_filter_normal)) }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.subs_sort_title),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    SubSort.entries.forEach { option ->
                        FilterChip(
                            selected = sort == option,
                            onClick = { sort = option },
                            label = { Text(stringResource(option.labelRes)) }
                        )
                    }
                }
            }
            val filtered = list.filter { ui ->
                (query.isBlank() || ui.entity.name.contains(query, ignoreCase = true)) &&
                (selected.isEmpty() || selected.contains(ui.status))
            }
            val sorted = when (sort) {
                SubSort.NEXT_EXPIRY -> filtered.sortedBy { it.daysLeft }
                SubSort.NAME -> filtered.sortedBy { it.entity.name.lowercase(Locale.getDefault()) }
                SubSort.PRICE -> filtered.sortedByDescending { it.entity.priceCents }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(sorted) { ui ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { nav.navigate(NavRoutes.SubEdit + "?id=${ui.entity.id}") }
                    ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(ui.entity.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            val text = stringResource(
                                R.string.subs_card_summary,
                                ui.entity.endAt,
                                ui.daysLeft,
                                ui.entity.priceCents / 100.0
                            )
                            Text(text, style = MaterialTheme.typography.bodyMedium)
                        }
                        val tag = when (ui.status) {
                            SubscriptionsViewModel.UiSub.Status.EXPIRED -> stringResource(R.string.subs_status_expired)
                            SubscriptionsViewModel.UiSub.Status.DUE -> stringResource(R.string.subs_status_due)
                            SubscriptionsViewModel.UiSub.Status.NORMAL -> stringResource(R.string.subs_status_normal)
                        }
                        Text(tag, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}

private enum class SubSort(val labelRes: Int) {
    NEXT_EXPIRY(R.string.subs_sort_expiry),
    NAME(R.string.subs_sort_name),
    PRICE(R.string.subs_sort_price)
}
}
