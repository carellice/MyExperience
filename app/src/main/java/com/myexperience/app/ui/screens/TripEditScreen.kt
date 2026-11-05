package com.myexperience.app.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.myexperience.app.MainViewModel
import com.myexperience.app.data.PlannedTrip
import com.myexperience.app.ui.components.*
import com.myexperience.app.ui.theme.BrandBrush
import com.myexperience.app.ui.theme.TripGradients

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TripEditScreen(vm: MainViewModel, tripId: Long, onBack: () -> Unit, onSaved: (Long) -> Unit) {
    val context = LocalContext.current
    var loaded by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var start by rememberSaveable { mutableStateOf<Long?>(null) }
    var end by rememberSaveable { mutableStateOf<Long?>(null) }
    var budget by rememberSaveable { mutableStateOf("") }
    var transport by rememberSaveable { mutableStateOf("") }
    var accommodation by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var colorIndex by rememberSaveable { mutableIntStateOf((0 until TripGradients.size).random()) }
    var createdAt by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    var showDates by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (loaded) return@LaunchedEffect
        loaded = true
        if (tripId != 0L) vm.getTrip(tripId)?.let { t ->
            name = t.name; start = t.startDate; end = t.endDate; budget = t.budget
            transport = t.transport; accommodation = t.accommodation; notes = t.notes
            colorIndex = t.colorIndex; createdAt = t.createdAt
        }
    }

    val save: () -> Unit = {
        when {
            saving -> Unit
            name.isBlank() -> Toast.makeText(context, "Dai un nome al viaggio", Toast.LENGTH_SHORT).show()
            else -> {
                saving = true
                vm.saveTrip(
                    PlannedTrip(
                        id = tripId, name = name.trim(), startDate = start, endDate = end,
                        budget = budget.trim(), transport = transport, accommodation = accommodation.trim(),
                        notes = notes.trim(), colorIndex = colorIndex, createdAt = createdAt,
                    )
                ) { onSaved(it) }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (tripId == 0L) "Nuovo viaggio" else "Modifica viaggio") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.Close, "Chiudi") } },
                actions = {
                    TextButton(onClick = save, enabled = !saving) {
                        Text("Salva", style = MaterialTheme.typography.titleMedium.copy(brush = BrandBrush))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Anteprima
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Brush.linearGradient(TripGradients[colorIndex.mod(TripGradients.size)]))
                    .padding(20.dp)
                    .appear(0),
            ) {
                Column(Modifier.align(Alignment.BottomStart)) {
                    GlassChip(countdownLabel(start, end), icon = Icons.Rounded.HourglassTop)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        name.ifBlank { "Il mio prossimo viaggio" },
                        color = Color.White, style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                FloatingIcon(transportIcon(transport), 48.dp, Color.White, Modifier.align(Alignment.TopEnd))
            }

            FormCard(Modifier.appear(1)) {
                FieldLabel("Nome del viaggio")
                OutlinedTextField(
                    name, { name = it },
                    placeholder = { Text("Es. Giappone in primavera") },
                    singleLine = true, shape = RoundedCornerShape(16.dp), colors = appTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(14.dp))
                FieldLabel("Date")
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .bounceClick { showDates = true }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.CalendarMonth, null)
                    Spacer(Modifier.width(12.dp))
                    Text(formatRange(start, end), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    if (start != null) {
                        Icon(
                            Icons.Rounded.Close, "Rimuovi date",
                            modifier = Modifier.size(20.dp).bounceClick { start = null; end = null },
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                FieldLabel("Colore della scheda")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TripGradients.forEachIndexed { i, g ->
                        val sel = i == colorIndex
                        val s by animateFloatAsState(
                            if (sel) 1.2f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "c",
                        )
                        Box(
                            Modifier
                                .size(30.dp)
                                .graphicsLayer { scaleX = s; scaleY = s }
                                .clip(CircleShape)
                                .background(Brush.linearGradient(g))
                                .then(if (sel) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                                .bounceClick { colorIndex = i },
                        )
                    }
                }
            }

            FormCard(Modifier.appear(2)) {
                FieldLabel("Come ci arrivi")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Transports.forEach { t ->
                        val sel = transportLabel(transport) == t
                        SelectChip(t, sel, transportIcon(t)) { transport = if (sel) "" else t }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        budget, { budget = it }, label = { Text("Budget €") },
                        singleLine = true, shape = RoundedCornerShape(16.dp), colors = appTextFieldColors(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        accommodation, { accommodation = it }, label = { Text("Alloggio") },
                        singleLine = true, shape = RoundedCornerShape(16.dp), colors = appTextFieldColors(),
                        modifier = Modifier.weight(1.4f),
                    )
                }
                Spacer(Modifier.height(14.dp))
                FieldLabel("Note")
                OutlinedTextField(
                    notes, { notes = it },
                    placeholder = { Text("Idee, prenotazioni, documenti, cose da non dimenticare…") },
                    minLines = 4, shape = RoundedCornerShape(16.dp), colors = appTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Text(
                "Dopo il salvataggio potrai aggiungere le mete sulla mappa e la checklist.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp),
            )

            GradientButton(
                if (tripId == 0L) "Crea viaggio" else "Salva modifiche",
                Modifier.fillMaxWidth().appear(3),
                icon = Icons.Rounded.Check,
                enabled = !saving,
                onClick = save,
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDates) {
        DateRangeDialog(
            start = start, end = end,
            onDismiss = { showDates = false },
            onConfirm = { s, e -> start = s; end = e; showDates = false },
        )
    }
}
