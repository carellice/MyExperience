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
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.myexperience.app.MainViewModel
import com.myexperience.app.data.Place
import com.myexperience.app.ui.components.*
import com.myexperience.app.ui.theme.BrandBrush
import com.myexperience.app.ui.theme.gradientFor
import java.io.File

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlaceEditScreen(
    vm: MainViewModel,
    placeId: Long,
    fromDestinationId: Long,
    onBack: () -> Unit,
    onPickLocation: () -> Unit,
    onSaved: (Long) -> Unit,
) {
    val context = LocalContext.current
    var loaded by rememberSaveable { mutableStateOf(false) }
    var title by rememberSaveable { mutableStateOf("") }
    var locationName by rememberSaveable { mutableStateOf("") }
    var country by rememberSaveable { mutableStateOf("") }
    var lat by rememberSaveable { mutableStateOf<Double?>(null) }
    var lon by rememberSaveable { mutableStateOf<Double?>(null) }
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
    var destToMark by rememberSaveable { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        if (loaded) return@LaunchedEffect
        loaded = true
        if (placeId != 0L) {
            vm.getPlace(placeId)?.let { p ->
                title = p.title; locationName = p.locationName; country = p.country
                lat = p.latitude; lon = p.longitude; start = p.startDate; end = p.endDate
                description = p.description; highlights = p.highlights; companions = p.companions
                budget = p.budget; rating = p.rating; mood = p.mood; category = p.category
                coverPath = p.coverPath; favorite = p.favorite; createdAt = p.createdAt
            }
        } else if (fromDestinationId != 0L) {
            vm.getDestination(fromDestinationId)?.let { d ->
                destToMark = d.id
                locationName = d.name; country = d.country; lat = d.latitude; lon = d.longitude
                title = "Esperienza a ${d.name}"
                description = d.notes
                vm.getTrip(d.tripId)?.let { t ->
                    t.startDate?.let { start = it; end = t.endDate ?: it }
                    if (t.budget.isNotBlank()) budget = t.budget
                }
            }
        }
    }

    val picked by vm.pickedLocation.collectAsStateWithLifecycle()
    LaunchedEffect(picked) {
        picked?.let {
            lat = it.latitude; lon = it.longitude
            locationName = it.name; country = it.country
            if (title.isBlank()) title = "Esperienza a ${it.name}"
            vm.consumePicked()
        }
    }

    val save: () -> Unit = {
        val la = lat
        val lo = lon
        when {
            saving -> Unit
            title.isBlank() -> Toast.makeText(context, "Dai un titolo all'esperienza", Toast.LENGTH_SHORT).show()
            la == null || lo == null || locationName.isBlank() ->
                Toast.makeText(context, "Scegli il luogo sulla mappa", Toast.LENGTH_SHORT).show()
            else -> {
                saving = true
                val place = Place(
                    id = placeId, title = title.trim(), locationName = locationName.trim(),
                    country = country.trim(), latitude = la, longitude = lo,
                    startDate = start, endDate = end, description = description.trim(),
                    highlights = highlights.trim(), companions = companions.trim(),
                    rating = rating, mood = mood, category = category, budget = budget.trim(),
                    coverPath = coverPath, favorite = favorite, createdAt = createdAt,
                )
                if (destToMark != 0L) vm.markDestinationVisited(destToMark)
                vm.savePlace(place, newCover) { onSaved(it) }
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
                FieldLabel("Luogo")
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .bounceClick(onClick = onPickLocation)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(BrandBrush),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.Place, null, tint = Color.White) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        if (lat != null) {
                            Text(locationName.ifBlank { "Luogo selezionato" }, style = MaterialTheme.typography.titleSmall)
                            Text(
                                country.ifBlank { "Tocca per cambiare" },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            Text("Scegli sulla mappa", style = MaterialTheme.typography.titleSmall)
                            Text("Cerca una città o tocca la mappa", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (lat != null) {
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            locationName, { locationName = it }, label = { Text("Nome luogo") },
                            singleLine = true, shape = RoundedCornerShape(16.dp), colors = appTextFieldColors(),
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            country, { country = it }, label = { Text("Paese") },
                            singleLine = true, shape = RoundedCornerShape(16.dp), colors = appTextFieldColors(),
                            modifier = Modifier.weight(1f),
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
                    Categories.forEach { (name, emoji) ->
                        SelectChip("$emoji $name", category == name) { category = name }
                    }
                }
                Spacer(Modifier.height(16.dp))
                FieldLabel("Mood")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Moods.forEach { m ->
                        val sel = m == mood
                        val s by animateFloatAsState(
                            if (sel) 1.25f else 1f,
                            spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "mood",
                        )
                        Box(
                            Modifier
                                .size(38.dp)
                                .graphicsLayer { scaleX = s; scaleY = s }
                                .clip(CircleShape)
                                .background(if (sel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .bounceClick { mood = m },
                            contentAlignment = Alignment.Center,
                        ) { Text(m, fontSize = 20.sp) }
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
                if (placeId == 0L) "✨ Dopo il salvataggio troverai una pagina di diario già impaginata, pronta da personalizzare."
                else "Il diario si modifica dall'album con ✏️ Personalizza.",
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
}


@Composable
fun SelectChip(text: String, selected: Boolean, onClick: () -> Unit) {
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
    ) { Text(text, color = fg, style = MaterialTheme.typography.labelLarge) }
}
