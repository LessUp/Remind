package app.lessup.remind.ui.items

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import app.lessup.remind.ui.navigation.NavRoutes
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.FilterChip

@Composable
fun ItemsScreen(nav: NavController, padding: PaddingValues, vm: ItemsViewModel = hiltViewModel()) {
    val list by vm.items.collectAsState()
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(emptySet<ItemsViewModel.UiItem.Status>()) }
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
                    label = { Text("搜索") }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val allSelected = selected.isEmpty()
                    FilterChip(
                        selected = allSelected,
                        onClick = { selected = emptySet() },
                        label = { Text("全部") }
                    )
                    FilterChip(
                        selected = selected.contains(ItemsViewModel.UiItem.Status.DUE),
                        onClick = {
                            selected = if (selected.contains(ItemsViewModel.UiItem.Status.DUE)) selected - ItemsViewModel.UiItem.Status.DUE else selected + ItemsViewModel.UiItem.Status.DUE
                        },
                        label = { Text("临期") }
                    )
                    FilterChip(
                        selected = selected.contains(ItemsViewModel.UiItem.Status.EXPIRED),
                        onClick = {
                            selected = if (selected.contains(ItemsViewModel.UiItem.Status.EXPIRED)) selected - ItemsViewModel.UiItem.Status.EXPIRED else selected + ItemsViewModel.UiItem.Status.EXPIRED
                        },
                        label = { Text("已过期") }
                    )
                    FilterChip(
                        selected = selected.contains(ItemsViewModel.UiItem.Status.NO_EXPIRY),
                        onClick = {
                            selected = if (selected.contains(ItemsViewModel.UiItem.Status.NO_EXPIRY)) selected - ItemsViewModel.UiItem.Status.NO_EXPIRY else selected + ItemsViewModel.UiItem.Status.NO_EXPIRY
                        },
                        label = { Text("无保质期") }
                    )
                    FilterChip(
                        selected = selected.contains(ItemsViewModel.UiItem.Status.NORMAL),
                        onClick = {
                            selected = if (selected.contains(ItemsViewModel.UiItem.Status.NORMAL)) selected - ItemsViewModel.UiItem.Status.NORMAL else selected + ItemsViewModel.UiItem.Status.NORMAL
                        },
                        label = { Text("正常") }
                    )
                }
            }
            val filtered = list.filter { ui ->
                (query.isBlank() || ui.entity.name.contains(query, ignoreCase = true)) &&
                (selected.isEmpty() || selected.contains(ui.status))
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtered) { ui ->
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
                                ItemsViewModel.UiItem.Status.NO_EXPIRY -> "购买 ${ui.entity.purchasedAt} · 已购 ${ui.daysSince} 天"
                                ItemsViewModel.UiItem.Status.EXPIRED -> "购买 ${ui.entity.purchasedAt} · 已过期 ${-(ui.daysToExpire ?: 0)} 天"
                                ItemsViewModel.UiItem.Status.DUE -> "购买 ${ui.entity.purchasedAt} · 距到期 ${ui.daysToExpire} 天"
                                ItemsViewModel.UiItem.Status.NORMAL -> "购买 ${ui.entity.purchasedAt} · 距到期 ${ui.daysToExpire} 天"
                            }
                            Text(sub, style = MaterialTheme.typography.bodyMedium)
                        }
                        val tag = when (ui.status) {
                            ItemsViewModel.UiItem.Status.NO_EXPIRY -> "无保质期"
                            ItemsViewModel.UiItem.Status.EXPIRED -> "已过期"
                            ItemsViewModel.UiItem.Status.DUE -> "临期"
                            ItemsViewModel.UiItem.Status.NORMAL -> "正常"
                        }
                        Text(tag, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                }
            }
        }
    }
}
