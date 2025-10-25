package app.lessup.remind.ui.subs

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
fun SubsScreen(nav: NavController, padding: PaddingValues, vm: SubscriptionsViewModel = hiltViewModel()) {
    val list by vm.subs.collectAsState()
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf(emptySet<SubscriptionsViewModel.UiSub.Status>()) }
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
                        selected = selected.contains(SubscriptionsViewModel.UiSub.Status.DUE),
                        onClick = {
                            selected = if (selected.contains(SubscriptionsViewModel.UiSub.Status.DUE)) selected - SubscriptionsViewModel.UiSub.Status.DUE else selected + SubscriptionsViewModel.UiSub.Status.DUE
                        },
                        label = { Text("临期") }
                    )
                    FilterChip(
                        selected = selected.contains(SubscriptionsViewModel.UiSub.Status.EXPIRED),
                        onClick = {
                            selected = if (selected.contains(SubscriptionsViewModel.UiSub.Status.EXPIRED)) selected - SubscriptionsViewModel.UiSub.Status.EXPIRED else selected + SubscriptionsViewModel.UiSub.Status.EXPIRED
                        },
                        label = { Text("已过期") }
                    )
                    FilterChip(
                        selected = selected.contains(SubscriptionsViewModel.UiSub.Status.NORMAL),
                        onClick = {
                            selected = if (selected.contains(SubscriptionsViewModel.UiSub.Status.NORMAL)) selected - SubscriptionsViewModel.UiSub.Status.NORMAL else selected + SubscriptionsViewModel.UiSub.Status.NORMAL
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
                            val sub = "到期 ${ui.entity.endAt} · 剩余 ${ui.daysLeft} 天 · CNY ${(ui.entity.priceCents.toDouble()/100.0)}"
                            Text(sub, style = MaterialTheme.typography.bodyMedium)
                        }
                        val tag = when (ui.status) {
                            SubscriptionsViewModel.UiSub.Status.EXPIRED -> "已过期"
                            SubscriptionsViewModel.UiSub.Status.DUE -> "临期"
                            SubscriptionsViewModel.UiSub.Status.NORMAL -> "正常"
                        }
                        Text(tag, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                }
            }
        }
    }
}
