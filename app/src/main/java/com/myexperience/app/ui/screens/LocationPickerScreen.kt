package com.myexperience.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.myexperience.app.MainViewModel
import com.myexperience.app.data.PickedLocation
import com.myexperience.app.ui.components.*
import com.myexperience.app.ui.theme.BrandBrush
import kotlinx.coroutines.launch

@Composable
fun LocationPickerScreen(vm: MainViewModel, target: String, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<PickedLocation>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var resolving by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<PickedLocation?>(null) }
    var focus by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var noResults by remember { mutableStateOf(false) }

    fun search() {
        if (query.isBlank()) return
        keyboard?.hide()
        searching = true
        noResults = false
        scope.launch {
            results = vm.searchLocation(query.trim())
            noResults = results.isEmpty()
            searching = false
        }
    }

    Box(Modifier.fillMaxSize()) {
        OsmMap(
            pins = selected?.let { listOf(MapPin(0, it.latitude, it.longitude, it.name)) } ?: emptyList(),
            modifier = Modifier.fillMaxSize(),
            autoFit = false,
            focus = focus,
            onMapTap = { la, lo ->
                results = emptyList()
                selected = PickedLocation(la, lo, "…", "")
                resolving = true
                scope.launch {
                    selected = vm.reverse(la, lo)
                    resolving = false
                }
            },
        )

        Column(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircleIconButton(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    container = MaterialTheme.colorScheme.surface,
                    tint = MaterialTheme.colorScheme.onSurface,
                    onClick = onDone,
                )
                Spacer(Modifier.width(10.dp))
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Cerca città, luogo, paese…") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                    trailingIcon = {
                        if (searching) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { search() }),
                    shape = RoundedCornerShape(22.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                    modifier = Modifier.weight(1f).shadow(8.dp, RoundedCornerShape(22.dp)),
                )
            }
            AnimatedVisibility(
                visible = results.isNotEmpty() || noResults,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(
                    Modifier
                        .padding(top = 10.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(MaterialTheme.colorScheme.surface),
                ) {
                    if (noResults) {
                        Text(
                            "Nessun risultato. Prova a toccare direttamente la mappa.",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    LazyColumn(Modifier.heightIn(max = 300.dp)) {
                        items(results) { r ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .bounceClick {
                                        selected = r
                                        focus = r.latitude to r.longitude
                                        results = emptyList()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Rounded.Place, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(r.name, style = MaterialTheme.typography.titleSmall)
                                    if (r.country.isNotBlank()) {
                                        Text(r.country, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Pannello di conferma
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp)
                .fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
        ) {
            AnimatedContent(targetState = selected, label = "sel") { sel ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).background(BrandBrush),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            if (sel == null) Icons.Rounded.Explore else Icons.Rounded.Place, null,
                            tint = Color.White, modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        if (sel == null) {
                            Text("Dove sei stato?", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Cerca un luogo o tocca un punto sulla mappa",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            Text(sel.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                sel.country.ifBlank { "%.4f, %.4f".format(sel.latitude, sel.longitude) },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            GradientButton(
                if (target == "place") "Conferma luogo" else "Aggiungi tappa",
                Modifier.fillMaxWidth(),
                icon = Icons.Rounded.Check,
                enabled = selected != null && !resolving,
            ) {
                val sel = selected ?: return@GradientButton
                if (target == "place") {
                    vm.setPicked(sel)
                } else {
                    target.toLongOrNull()?.let { vm.addDestination(it, sel) }
                }
                onDone()
            }
        }
    }
}
