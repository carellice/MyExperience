package com.myexperience.app.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.myexperience.app.MainViewModel
import com.myexperience.app.data.Place
import com.myexperience.app.data.PlaceStop
import com.myexperience.app.ui.components.*
import com.myexperience.app.ui.theme.BrandBrush
import com.myexperience.app.ui.theme.gradientFor
import java.io.File

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlaceEditScreen(
    vm: MainViewModel,
    placeId: Long,
    onBack: () -> Unit,
    onPickLocation: () -> Unit,
    onSaved: (Long) -> Unit,
) {
    val context = LocalContext.current
    var loaded by rememberSaveable { mutableStateOf(false) }
    var title by rememberSaveable { mutableStateOf("") }
    val stops = rememberSaveable(saver = StopsSaver) { mutableStateListOf<PlaceStop>() }
    var editStop by remember { mutableStateOf<Int?>(null) }
    var start by rememberSaveable { mutableLongStateOf(todayUtcMillis()) }
    var end by rememberSaveable { mutableLongStateOf(todayUtcMillis()) }
    var description by rememberSaveable { mutableStateOf("") }
    var highlights by rememberSaveable { mutableStateOf("") }
    var companions by rememberSaveable { mutableStateOf("") }
    var budget by rememberSaveable { mutableStateOf("") }
    var rating by rememberSaveable { mutableIntStateOf(4) }
    var mood by rememberSaveable { mutableStateOf("😍") }
    var category by rememberSaveable { mutableStateOf("Città") }
    var coverPath by rememberSaveable { mutableStateOf<String?>(null) }
    var newCover by rememberSaveable { mutableStateOf<Uri?>(null) }
    var favorite by rememberSaveable { mutableStateOf(false) }
    var createdAt by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    var showDates by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (loaded) return@LaunchedEffect
        loaded = true
        if (placeId != 0L) {
            vm.getPlace(placeId)?.let { p ->
                title = p.title; start = p.startDate; end = p.endDate
                description = p.description; highlights = p.highlights; companions = p.companions
                budget = p.budget; rating = p.rating; mood = p.mood; category = p.category
                coverPath = p.coverPath; favorite = p.favorite; createdAt = p.createdAt
                val saved = vm.getStops(placeId)
                stops.clear()
                stops += saved.ifEmpty {
                    listOf(PlaceStop(placeId = p.id, name = p.locationName, country = p.country,
                        latitude = p.latitude, longitude = p.longitude))
                }
            }
        }
    }

    val picked by vm.pickedLocation.collectAsStateWithLifecycle()
    LaunchedEffect(picked) {
        picked?.let {
            stops += PlaceStop(placeId = placeId, name = it.name, country = it.country,
                latitude = it.latitude, longitude = it.longitude)
            if (title.isBlank()) title = "Esperienza a ${it.name}"
            if (stops.size > 1 && category == "Città") category = "On the road"
            vm.consumePicked()
        }
    }

    val save: () -> Unit = {
        when {
            saving -> Unit
            title.isBlank() -> Toast.makeText(context, "Dai un titolo all'esperienza", Toast.LENGTH_SHORT).show()
            stops.isEmpty() -> Toast.makeText(context, "Aggiungi almeno un luogo dalla mappa", Toast.LENGTH_SHORT).show()
            else -> {
                saving = true
                val first = stops.first()
                val place = Place(
                    id = placeId, title = title.trim(), locationName = first.name,
                    country = first.country, latitude = first.latitude, longitude = first.longitude,
                    startDate = start, endDate = end, description = description.trim(),
                    highlights = highlights.trim(), companions = companions.trim(),
                    rating = rating, mood = mood, category = category, budget = budget.trim(),
                    coverPath = coverPath, favorite = favorite, createdAt = createdAt,
                )
                vm.savePlace(place, stops.toList(), newCover) { onSaved(it) }
            }
        }
    }

    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) newCover = uri
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (placeId == 0L) "Nuova esperienza" else "Modifica esperienza") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Rounded.Close, "Chiudi") }
                },
                actions = {
                    TextButton(
                        enabled = !saving,
                        onClick = { save() },
                    ) { Text("Salva", style = MaterialTheme.typography.titleMedium.copy(brush = BrandBrush)) }
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
            // Copertina
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(gradientFor(placeId + title.length))
                    .bounceClick {
                        coverPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                    .appear(0),
                contentAlignment = Alignment.Center,
            ) {
                val model: Any? = newCover ?: coverPath?.let { File(it) }
                if (model != null) {
                    AsyncImage(model, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    GlassChip("Cambia copertina", Modifier.align(Alignment.BottomEnd).padding(12.dp))
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.AddAPhoto, null, tint = Color.White, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(6.dp))
                        Text("Aggiungi una copertina", color = Color.White, style = MaterialTheme.typography.titleMedium)
                        Text("(opzionale: verrà usata la prima foto dell'album)", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                }
            }

            FormCard(Modifier.appear(1)) {
                FieldLabel("Titolo")
                OutlinedTextField(
                    title, { title = it },
                    placeholder = { Text("Es. Tramonto a Santorini") },
                    singleLine = true, shape = RoundedCornerShape(16.dp), colors = appTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(14.dp))
                FieldLabel(if (stops.size > 1) "Tappe · viaggio on the road" else "Luogo")
                stops.forEachIndexed { i, s ->
                    StopRow(
                        index = i,
                        stop = s,
                        multi = stops.size > 1,
                        canUp = i > 0,
                        canDown = i < stops.lastIndex,
                        onClick = { editStop = i },
                        onUp = { stops.add(i - 1, stops.removeAt(i)) },
                        onDown = { stops.add(i + 1, stops.removeAt(i)) },
                    )
                    Spacer(Modifier.height(8.dp))
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                        .bounceClick(onClick = onPickLocation)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(BrandBrush),
                        contentAlignment = Alignment.Center,
                    ) { Icon(if (stops.isEmpty()) Icons.Rounded.Place else Icons.Rounded.Add, null, tint = Color.White) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (stops.isEmpty()) "Scegli sulla mappa" else "Aggiungi una tappa",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            if (stops.isEmpty()) "Cerca una città o tocca la mappa"
                            else "Più tappe = un unico album on the road",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
                FieldLabel("Quando")
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
                    val days = daysBetween(start, end)
                    InfoChip(if (days == 1L) "1 giorno" else "$days giorni", container = MaterialTheme.colorScheme.surface)
                }
            }

            FormCard(Modifier.appear(2)) {
                FieldLabel("Tipo di esperienza")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Categories.forEach { name ->
                        SelectChip(name, category == name, categoryIcon(name)) { category = name }
                    }
                }
                Spacer(Modifier.height(16.dp))
                FieldLabel("Mood · ${moodFor(mood).label}")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Moods.forEach { m ->
                        val sel = m.key == mood
                        val s by animateFloatAsState(
                            if (sel) 1.2f else 1f,
                            spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "mood",
                        )
                        Box(
                            Modifier
                                .size(38.dp)
                                .graphicsLayer { scaleX = s; scaleY = s }
                                .clip(CircleShape)
                                .background(if (sel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .bounceClick { mood = m.key },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                m.icon, m.label,
                                tint = if (sel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                FieldLabel("Voto")
                RatingStars(rating, 34.dp) { rating = it }
            }

            FormCard(Modifier.appear(3)) {
                FieldLabel("Racconto breve (opzionale)")
                OutlinedTextField(
                    description, { description = it },
                    placeholder = { Text("Due righe per ricordarla… il diario lo personalizzi dopo nell'album") },
                    minLines = 3, shape = RoundedCornerShape(16.dp), colors = appTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Text(
                if (placeId == 0L) "Dopo il salvataggio troverai una pagina di diario già impaginata, pronta da personalizzare."
                else "Il diario si modifica dall'album con Personalizza.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp),
            )

            GradientButton(
                if (placeId == 0L) "Crea esperienza" else "Salva modifiche",
                Modifier.fillMaxWidth().appear(4),
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

    editStop?.let { i ->
        val s = stops.getOrNull(i) ?: return@let
        var name by remember(i, s) { mutableStateOf(s.name) }
        var country by remember(i, s) { mutableStateOf(s.country) }
        var nights by remember(i, s) { mutableIntStateOf(s.nights) }
        var notes by remember(i, s) { mutableStateOf(s.notes) }
        AlertDialog(
            onDismissRequest = { editStop = null },
            icon = { Icon(Icons.Rounded.Place, null) },
            title = { Text("Tappa ${i + 1}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Nome luogo") }, singleLine = true, shape = RoundedCornerShape(16.dp))
                    OutlinedTextField(country, { country = it }, label = { Text("Paese") }, singleLine = true, shape = RoundedCornerShape(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Notti", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                        FilledTonalIconButton(onClick = { if (nights > 0) nights-- }) { Text("−", fontSize = 20.sp) }
                        Text("$nights", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 14.dp))
                        FilledTonalIconButton(onClick = { nights++ }) { Text("+", fontSize = 20.sp) }
                    }
                    OutlinedTextField(notes, { notes = it }, label = { Text("Note") }, minLines = 2, shape = RoundedCornerShape(16.dp))
                    TextButton(onClick = { stops.removeAt(i); editStop = null }) {
                        Text("Rimuovi tappa", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    stops[i] = s.copy(name = name.trim().ifBlank { s.name }, country = country.trim(), nights = nights, notes = notes.trim())
                    editStop = null
                }) { Text("Salva") }
            },
            dismissButton = { TextButton(onClick = { editStop = null }) { Text("Annulla") } },
        )
    }
}

/** Le tappe sopravvivono a rotazioni e alla navigazione verso la mappa. */
private val StopsSaver = listSaver<SnapshotStateList<PlaceStop>, Any>(
    save = { list ->
        list.flatMap { listOf(it.id, it.name, it.country, it.latitude, it.longitude, it.nights, it.notes) }
    },
    restore = { flat ->
        flat.chunked(7).map { c ->
            PlaceStop(
                id = c[0] as Long, placeId = 0, name = c[1] as String, country = c[2] as String,
                latitude = c[3] as Double, longitude = c[4] as Double, nights = c[5] as Int, notes = c[6] as String,
            )
        }.toMutableStateList()
    },
)

@Composable
private fun StopRow(
    index: Int,
    stop: PlaceStop,
    multi: Boolean,
    canUp: Boolean,
    canDown: Boolean,
    onClick: () -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .bounceClick(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(34.dp).clip(CircleShape).background(BrandBrush),
            contentAlignment = Alignment.Center,
        ) {
            if (multi) Text("${index + 1}", color = Color.White, fontWeight = FontWeight.Black)
            else Icon(Icons.Rounded.Place, null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(stop.name, style = MaterialTheme.typography.titleSmall)
            val sub = listOfNotNull(
                stop.country.ifBlank { null },
                if (stop.nights > 0) "${stop.nights} ${if (stop.nights == 1) "notte" else "notti"}" else null,
            ).joinToString(" · ").ifBlank { "Tocca per modificare" }
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (multi) {
            IconButton(onClick = onUp, enabled = canUp, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Rounded.ArrowUpward, "Su", Modifier.size(18.dp))
            }
            IconButton(onClick = onDown, enabled = canDown, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Rounded.ArrowDownward, "Giù", Modifier.size(18.dp))
            }
        }
    }
}


@Composable
fun SelectChip(text: String, selected: Boolean, icon: ImageVector? = null, onClick: () -> Unit) {
    val bg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        label = "chip",
    )
    val fg = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) BrandBrush else Brush.linearGradient(listOf(bg, bg)))
            .then(if (!selected) Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(50)) else Modifier)
            .bounceClick(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(text, color = fg, style = MaterialTheme.typography.labelLarge)
        }
    }
}
