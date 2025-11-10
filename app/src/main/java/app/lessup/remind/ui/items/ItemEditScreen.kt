package app.lessup.remind.ui.items

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import app.lessup.remind.R

@Composable
fun ItemEditScreen(nav: NavController, padding: PaddingValues, id: Long?, vm: ItemEditViewModel = hiltViewModel()) {
    var form by remember { mutableStateOf(ItemEditViewModel.Form()) }
    val ctx = LocalContext.current

    LaunchedEffect(id) {
        if (id != null) {
            vm.load(id)?.let { e ->
                form = ItemEditViewModel.Form(
                    id = e.id,
                    name = e.name,
                    purchasedAt = e.purchasedAt,
                    shelfLifeDays = e.shelfLifeDays,
                    expiryAt = e.expiryAt,
                    notes = e.notes
                )
            }
        }
    }

    var shelfDaysText by remember(form.id) { mutableStateOf(form.shelfLifeDays?.toString() ?: "") }

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
                value = shelfDaysText,
                onValueChange = { txt ->
                    shelfDaysText = txt.filter { it.isDigit() }
                    form = form.copy(shelfLifeDays = shelfDaysText.toIntOrNull())
                },
                label = { Text(stringResource(R.string.shelf_life_days)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions.Default
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                val fallbackExpiry = form.shelfLifeDays?.let { form.purchasedAt.plus(kotlinx.datetime.DatePeriod(days = it)) }
                Text(
                    text = stringResource(
                        R.string.item_edit_expiry_date,
                        (form.expiryAt ?: fallbackExpiry)?.toString() ?: stringResource(R.string.item_edit_expiry_unset)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 16.dp)
                )
                Button(onClick = {
                    val date = form.expiryAt ?: form.purchasedAt
                    DatePickerDialog(
                        ctx,
                        { _, y, m, d -> form = form.copy(expiryAt = LocalDate(y, m + 1, d)) },
                        date.year,
                        date.monthNumber - 1,
                        date.dayOfMonth
                    ).show()
                }) { Text(stringResource(R.string.item_edit_set_expiry)) }
            }

            OutlinedTextField(
                value = form.notes ?: "",
                onValueChange = { form = form.copy(notes = it) },
                label = { Text(stringResource(R.string.notes)) },
                modifier = Modifier.fillMaxWidth()
            )

            Button(onClick = {
                vm.save(form.copy(expiryAt = form.expiryAt))
                nav.navigateUp()
            }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.save)) }
        }
    }
}
