package app.lessup.remind.ui.subs

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import kotlinx.datetime.LocalDate
import kotlin.math.roundToLong
import app.lessup.remind.R

@Composable
fun SubEditScreen(nav: NavController, padding: PaddingValues, id: Long?, vm: SubEditViewModel = hiltViewModel()) {
    var form by remember { mutableStateOf(SubEditViewModel.Form()) }
    val ctx = LocalContext.current

    LaunchedEffect(id) {
        if (id != null) {
            vm.load(id)?.let { e ->
                form = SubEditViewModel.Form(
                    id = e.id,
                    name = e.name,
                    provider = e.provider,
                    purchasedAt = e.purchasedAt,
                    priceCents = e.priceCents,
                    endAt = e.endAt,
                    autoRenew = e.autoRenew,
                    notes = e.notes
                )
            }
        }
    }

    var priceText by remember(form.id) { mutableStateOf(if (form.priceCents > 0) (form.priceCents.toDouble() / 100.0).toString() else "") }

    Scaffold(modifier = Modifier.padding(padding)) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = form.name,
                onValueChange = { form = form.copy(name = it) },
                label = { Text(stringResource(R.string.name)) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = form.provider ?: "",
                onValueChange = { form = form.copy(provider = it) },
                label = { Text(stringResource(R.string.provider)) },
                modifier = Modifier.fillMaxWidth()
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.item_edit_purchase_date, form.purchasedAt),
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 16.dp)
                )
                Button(onClick = {
                    val date = form.purchasedAt
                    DatePickerDialog(
                        ctx,
                        { _, y, m, d -> form = form.copy(purchasedAt = LocalDate(y, m + 1, d)) },
                        date.year,
                        date.monthNumber - 1,
                        date.dayOfMonth
                    ).show()
                }) { Text(stringResource(R.string.item_edit_select_date)) }
            }

            OutlinedTextField(
                value = priceText,
                onValueChange = { t ->
                    val sanitized = t.filter { it.isDigit() || it == '.' }
                    priceText = sanitized
                    val cents = runCatching { (sanitized.toDouble() * 100).roundToLong() }.getOrDefault(0)
                    form = form.copy(priceCents = cents)
                },
                label = { Text(stringResource(R.string.subs_price_label)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions.Default
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.subs_end_date, form.endAt),
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 16.dp)
                )
                Button(onClick = {
                    val date = form.endAt
                    DatePickerDialog(
                        ctx,
                        { _, y, m, d -> form = form.copy(endAt = LocalDate(y, m + 1, d)) },
                        date.year,
                        date.monthNumber - 1,
                        date.dayOfMonth
                    ).show()
                }) { Text(stringResource(R.string.subs_set_end_date)) }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.subs_auto_renew))
                Switch(checked = form.autoRenew, onCheckedChange = { form = form.copy(autoRenew = it) })
            }

            OutlinedTextField(
                value = form.notes ?: "",
                onValueChange = { form = form.copy(notes = it) },
                label = { Text(stringResource(R.string.notes)) },
                modifier = Modifier.fillMaxWidth()
            )

            Button(onClick = {
                vm.save(form)
                nav.navigateUp()
            }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.save)) }
        }
    }
}
