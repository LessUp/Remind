package app.lessup.remind.ui.items

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
fun ItemsScreen(nav: NavController, padding: PaddingValues, vm: ItemsViewModel = hiltViewModel()) {
    val list by vm.items.collectAsState()
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(emptySet<ItemsViewModel.UiItem.Status>()) }
    var sort by remember { mutableStateOf(ItemSort.NEXT_EXPIRY) }
    Scaffold(
        modifier = Modifier.padding(padding),
        floatingActionButton = {
            FloatingActionButton(onClick = { nav.navigate(NavRoutes.ItemEdit) }) {
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
                    label = { Text(stringResource(R.string.items_search_label)) }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val allSelected = selected.isEmpty()
                    FilterChip(
                        selected = allSelected,
                        onClick = { selected = emptySet() },
                        label = { Text(stringResource(R.string.items_filter_all)) }
                    )
                    FilterChip(
                        selected = selected.contains(ItemsViewModel.UiItem.Status.DUE),
                        onClick = {
                            selected = if (selected.contains(ItemsViewModel.UiItem.Status.DUE)) selected - ItemsViewModel.UiItem.Status.DUE else selected + ItemsViewModel.UiItem.Status.DUE
                        },
                        label = { Text(stringResource(R.string.items_filter_due)) }
                    )
                    FilterChip(
                        selected = selected.contains(ItemsViewModel.UiItem.Status.EXPIRED),
                        onClick = {
                            selected = if (selected.contains(ItemsViewModel.UiItem.Status.EXPIRED)) selected - ItemsViewModel.UiItem.Status.EXPIRED else selected + ItemsViewModel.UiItem.Status.EXPIRED
                        },
                        label = { Text(stringResource(R.string.items_filter_expired)) }
                    )
                    FilterChip(
                        selected = selected.contains(ItemsViewModel.UiItem.Status.NO_EXPIRY),
                        onClick = {
                            selected = if (selected.contains(ItemsViewModel.UiItem.Status.NO_EXPIRY)) selected - ItemsViewModel.UiItem.Status.NO_EXPIRY else selected + ItemsViewModel.UiItem.Status.NO_EXPIRY
                        },
                        label = { Text(stringResource(R.string.items_filter_no_expiry)) }
                    )
                    FilterChip(
                        selected = selected.contains(ItemsViewModel.UiItem.Status.NORMAL),
                        onClick = {
                            selected = if (selected.contains(ItemsViewModel.UiItem.Status.NORMAL)) selected - ItemsViewModel.UiItem.Status.NORMAL else selected + ItemsViewModel.UiItem.Status.NORMAL
                        },
                        label = { Text(stringResource(R.string.items_filter_normal)) }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.items_sort_title),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    ItemSort.entries.forEach { option ->
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
                ItemSort.NEXT_EXPIRY -> filtered.sortedWith(
                    compareBy<ItemsViewModel.UiItem> { it.daysToExpire ?: Long.MAX_VALUE }
                        .thenBy { it.entity.name.lowercase(Locale.getDefault()) }
                )
                ItemSort.NAME -> filtered.sortedBy { it.entity.name.lowercase(Locale.getDefault()) }
                ItemSort.RECENT -> filtered.sortedByDescending { it.entity.purchasedAt.toEpochDays() }
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
                            .clickable { nav.navigate(NavRoutes.ItemEdit + "?id=${ui.entity.id}") }
                    ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(ui.entity.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            val sub = when (ui.status) {
                                ItemsViewModel.UiItem.Status.NO_EXPIRY -> stringResource(
                                    R.string.items_card_no_expiry,
                                    ui.entity.purchasedAt,
                                    ui.daysSince
                                )
                                ItemsViewModel.UiItem.Status.EXPIRED -> stringResource(
                                    R.string.items_card_expired,
                                    ui.entity.purchasedAt,
                                    -(ui.daysToExpire ?: 0)
                                )
                                ItemsViewModel.UiItem.Status.DUE -> stringResource(
                                    R.string.items_card_due,
                                    ui.entity.purchasedAt,
                                    ui.daysToExpire ?: 0
                                )
                                ItemsViewModel.UiItem.Status.NORMAL -> stringResource(
                                    R.string.items_card_normal,
                                    ui.entity.purchasedAt,
                                    ui.daysToExpire ?: 0
                                )
                            }
                            Text(sub, style = MaterialTheme.typography.bodyMedium)
                        }
                        val tag = when (ui.status) {
                            ItemsViewModel.UiItem.Status.NO_EXPIRY -> stringResource(R.string.items_status_no_expiry)
                            ItemsViewModel.UiItem.Status.EXPIRED -> stringResource(R.string.items_status_expired)
                            ItemsViewModel.UiItem.Status.DUE -> stringResource(R.string.items_status_due)
                            ItemsViewModel.UiItem.Status.NORMAL -> stringResource(R.string.items_status_normal)
                        }
                        Text(tag, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}

private enum class ItemSort(val labelRes: Int) {
    NEXT_EXPIRY(R.string.items_sort_expiry),
    NAME(R.string.items_sort_name),
    RECENT(R.string.items_sort_recent)
}
}
