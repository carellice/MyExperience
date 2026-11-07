package com.myexperience.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import android.widget.Toast
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddLocationAlt
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Hotel
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PhotoAlbum
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myexperience.app.MainViewModel
import com.myexperience.app.data.TripDestination
import com.myexperience.app.ui.components.*
import com.myexperience.app.ui.theme.BrandBrush
import com.myexperience.app.ui.theme.Moss
import com.myexperience.app.ui.theme.TripGradients

@Composable
fun TripDetailScreen(
    vm: MainViewModel,
    tripId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onAddDestination: () -> Unit,
    onCompleted: (Long) -> Unit,
) {
    val context = LocalContext.current
    val flow = remember(tripId) { vm.trip(tripId) }
    val data by flow.collectAsStateWithLifecycle(initialValue = null)
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmComplete by remember { mutableStateOf(false) }
    var completing by remember { mutableStateOf(false) }
    var editDest by remember { mutableStateOf<TripDestination?>(null) }
    var newTask by remember { mutableStateOf("") }

    // il viaggio può sparire mentre è aperto (diventato album in automatico)
    var seen by remember { mutableStateOf(false) }
    LaunchedEffect(data) {
        if (data != null) seen = true else if (seen && !completing) onBack()
    }

    val d = data
    if (d == null) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        return
    }
    val trip = d.trip
    val colors = TripGradients[trip.colorIndex.mod(TripGradients.size)]

    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 48.dp),
    ) {
        // ---------- HERO ----------
        item {
            val inf = rememberInfiniteTransition(label = "hero")
            val fly by inf.animateFloat(
                0f, 1f, infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "fly",
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
                    .background(Brush.linearGradient(colors)),
            ) {
                Icon(
                    transportIcon(trip.transport), null, tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 24.dp)
                        .size(72.dp)
                        .graphicsLayer {
                            translationX = -40f + fly * 80f
                            translationY = 20f - fly * 40f
                            rotationZ = -10f + fly * 10f
                        },
                )
                Column(Modifier.statusBarsPadding().padding(16.dp)) {
                    Row {
                        CircleIconButton(Icons.AutoMirrored.Rounded.ArrowBack, onClick = onBack)
                        Spacer(Modifier.weight(1f))
                        CircleIconButton(Icons.Rounded.Edit, onClick = onEdit)
                        Spacer(Modifier.width(8.dp))
                        CircleIconButton(Icons.Rounded.Delete, onClick = { confirmDelete = true })
                    }
                    Spacer(Modifier.height(40.dp))
                    Column(Modifier.appear()) {
                        Text(trip.name, color = Color.White, style = MaterialTheme.typography.headlineLarge)
                        Text(formatRange(trip.startDate, trip.endDate), color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GlassChip(countdownLabel(trip.startDate, trip.endDate), icon = Icons.Rounded.HourglassTop)
                            if (trip.startDate != null) {
                                val days = daysBetween(trip.startDate, trip.endDate ?: trip.startDate)
                                GlassChip("$days giorni", icon = Icons.Rounded.CalendarMonth)
                            }
                            val flags = d.destinations.map { countryFlag(it.country) }.filter { it.isNotEmpty() }.distinct().take(4)
                            if (flags.isNotEmpty()) FlagsChip(flags)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        // ---------- MAPPA ----------
        if (d.destinations.isNotEmpty()) {
            item {
                Box(
                    Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .appear(1),
                ) {
                    OsmMap(
                        pins = d.destinations.map { MapPin(it.id, it.latitude, it.longitude, it.name) },
                        modifier = Modifier.fillMaxSize(),
                        drawRoute = true,
                        interactive = false,
                    )
                    GlassChip("Itinerario", Modifier.padding(12.dp), icon = Icons.Rounded.Explore)
                }
            }
        }

        // ---------- ITINERARIO ----------
        item {
            SectionTitle("Mete e tappe") {
                FilledTonalButton(onClick = onAddDestination, shape = RoundedCornerShape(16.dp)) {
                    Icon(Icons.Rounded.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Meta")
                }
            }
        }
        if (d.destinations.isEmpty()) {
            item {
                Column(
                    Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .bounceClick(onClick = onAddDestination)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    FloatingIcon(Icons.Rounded.AddLocationAlt, 40.dp, MaterialTheme.colorScheme.primary)
                    Text("Aggiungi la prima meta", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Cercala o scegli un punto sulla mappa",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        itemsIndexed(d.destinations, key = { _, x -> x.id }) { i, dest ->
            TimelineItem(
                index = i,
                dest = dest,
                isLast = i == d.destinations.lastIndex,
                modifier = Modifier.animateItem().appear(i),
                onClick = { editDest = dest },
                onVisited = { vm.toggleVisited(dest) },
            )
        }

        // ---------- CHIUSURA VIAGGIO ----------
        if (d.destinations.isNotEmpty()) {
            item {
                Column(
                    Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (d.destinations.size > 1) Icons.Rounded.Route else Icons.Rounded.PhotoAlbum, null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (d.destinations.size > 1) "Diventerà un album on the road" else "Diventerà un album",
                            style = MaterialTheme.typography.titleSmall,
                        )
                    }
                    Text(
                        "Quando il viaggio finisce, tutte le tappe confluiscono in automatico in un unico album-diario. " +
                            "Puoi anche chiuderlo subito.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    GradientButton(
                        "Viaggio concluso · crea l'album",
                        Modifier.fillMaxWidth(),
                        icon = Icons.Rounded.Check,
                        enabled = !completing,
                    ) { confirmComplete = true }
                }
            }
        }

        // ---------- CHECKLIST ----------
        item {
            val total = d.tasks.size
            val done = d.tasks.count { it.done }
            SectionTitle("Checklist", Modifier.padding(top = 12.dp)) {
                if (total > 0) InfoChip("$done/$total")
            }
            if (total > 0) {
                val progress by animateFloatAsState(done / total.toFloat(), tween(700), label = "p")
                Box(
                    Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(progress).clip(RoundedCornerShape(4.dp)).background(BrandBrush))
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        itemsIndexed(d.tasks, key = { _, t -> "task${t.id}" }) { _, task ->
            Row(
                Modifier
                    .animateItem()
                    .padding(horizontal = 16.dp, vertical = 3.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .bounceClick { vm.toggleTask(task) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AnimatedContent(task.done, label = "check", transitionSpec = {
                    (scaleIn(spring(dampingRatio = 0.4f)) + fadeIn()) togetherWith (scaleOut() + fadeOut())
                }) { checked ->
                    Icon(
                        if (checked) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                        null,
                        tint = if (checked) Moss else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    task.text,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (task.done) TextDecoration.LineThrough else null,
                    color = if (task.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
                Icon(
                    Icons.Rounded.Close, "Rimuovi",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp).bounceClick { vm.deleteTask(task) },
                )
            }
        }
        item {
            val submit = {
                if (newTask.isNotBlank()) {
                    vm.addTask(tripId, newTask.trim())
                    newTask = ""
                }
            }
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    newTask, { newTask = it },
                    placeholder = { Text("Es. Prenotare il volo, passaporto…") },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = appTextFieldColors(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier.size(52.dp).clip(CircleShape).background(BrandBrush).bounceClick(onClick = submit),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Add, "Aggiungi", tint = Color.White) }
            }
        }

        // ---------- DETTAGLI ----------
        item {
            SectionTitle("Dettagli", Modifier.padding(top = 12.dp))
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    InfoTile(Icons.Rounded.Payments, "Budget", trip.budget.ifBlank { "—" }.let { if (it != "—" && !it.contains("€")) "$it €" else it }, Modifier.weight(1f))
                    InfoTile(transportIcon(trip.transport), "Trasporto", transportLabel(trip.transport).ifBlank { "—" }, Modifier.weight(1f))
                }
                InfoTile(Icons.Rounded.Hotel, "Alloggio", trip.accommodation.ifBlank { "—" }, Modifier.fillMaxWidth())
                if (trip.notes.isNotBlank()) {
                    InfoTile(Icons.Rounded.EditNote, "Note", trip.notes, Modifier.fillMaxWidth())
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Eliminare il viaggio?") },
            text = { Text("Verranno eliminate anche le mete e la checklist.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onBack()
                    vm.deleteTrip(trip)
                }) { Text("Elimina", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Annulla") } },
        )
    }

    if (confirmComplete) {
        AlertDialog(
            onDismissRequest = { confirmComplete = false },
            title = { Text("Chiudere il viaggio?") },
            text = {
                Text(
                    "Le ${d.destinations.size} tappe diventano un unico album con diario e rullino. " +
                        "Il viaggio lascerà la lista dei viaggi da organizzare."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmComplete = false
                    completing = true
                    vm.completeTrip(tripId) { albumId ->
                        if (albumId != null) onCompleted(albumId)
                        else {
                            completing = false
                            Toast.makeText(context, "Aggiungi almeno una meta", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) { Text("Crea album") }
            },
            dismissButton = { TextButton(onClick = { confirmComplete = false }) { Text("Annulla") } },
        )
    }

    editDest?.let { dest ->
        var notes by remember(dest.id) { mutableStateOf(dest.notes) }
        var nights by remember(dest.id) { mutableIntStateOf(dest.nights) }
        var name by remember(dest.id) { mutableStateOf(dest.name) }
        AlertDialog(
            onDismissRequest = { editDest = null },
            icon = { Icon(Icons.Rounded.Place, null) },
            title = { Text(dest.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Nome") }, singleLine = true, shape = RoundedCornerShape(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Notti", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                        FilledTonalIconButton(onClick = { if (nights > 0) nights-- }) { Text("−", fontSize = 20.sp) }
                        AnimatedContent(nights, label = "n", transitionSpec = {
                            if (targetState > initialState) (slideInVertically { -it } + fadeIn()) togetherWith (slideOutVertically { it } + fadeOut())
                            else (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut())
                        }) { n ->
                            Text("$n", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 14.dp))
                        }
                        FilledTonalIconButton(onClick = { nights++ }) { Text("+", fontSize = 20.sp) }
                    }
                    OutlinedTextField(
                        notes, { notes = it }, label = { Text("Note, cose da vedere…") },
                        minLines = 3, shape = RoundedCornerShape(16.dp),
                    )
                    TextButton(onClick = {
                        vm.deleteDestination(dest); editDest = null
                    }) { Text("Rimuovi meta", color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.updateDestination(dest.copy(notes = notes.trim(), nights = nights, name = name.ifBlank { dest.name }))
                    editDest = null
                }) { Text("Salva") }
            },
            dismissButton = { TextButton(onClick = { editDest = null }) { Text("Annulla") } },
        )
    }
}

@Composable
private fun TimelineItem(
    index: Int,
    dest: TripDestination,
    isLast: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onVisited: () -> Unit,
) {
    Row(modifier.padding(horizontal = 16.dp).height(IntrinsicSize.Min)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(40.dp)) {
            Box(
                Modifier.size(34.dp).clip(CircleShape).background(BrandBrush),
                contentAlignment = Alignment.Center,
            ) { Text("${index + 1}", color = Color.White, fontWeight = FontWeight.Black) }
            if (!isLast) {
                Box(
                    Modifier
                        .width(3.dp)
                        .weight(1f)
                        .background(
                            Brush.verticalGradient(
                                listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            )
                        ),
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(
            Modifier
                .weight(1f)
                .padding(bottom = 12.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.surface)
                .bounceClick(onClick = onClick)
                .padding(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(flagPrefix(dest.country) + dest.name, style = MaterialTheme.typography.titleMedium)
                    if (dest.country.isNotBlank()) {
                        Text(dest.country, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                InfoChip(if (dest.nights == 1) "1 notte" else "${dest.nights} notti", icon = Icons.Rounded.Bedtime)
            }
            if (dest.notes.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(dest.notes, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(10.dp))
            if (dest.visited) {
                InfoChip("Visitata", container = Moss.copy(alpha = 0.2f), modifier = Modifier.bounceClick(onClick = onVisited), icon = Icons.Rounded.CheckCircle)
            } else {
                Row(
                    Modifier.bounceClick(onClick = onVisited).padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Segna come visitata", style = MaterialTheme.typography.labelLarge.copy(brush = BrandBrush))
                }
            }
        }
    }
}

@Composable
private fun InfoTile(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        IconBadge(icon, container = MaterialTheme.colorScheme.primaryContainer, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall)
        }
    }
}
