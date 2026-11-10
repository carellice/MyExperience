package com.myexperience.app.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.myexperience.app.MainViewModel
import com.myexperience.app.data.BadgeCollections
import com.myexperience.app.data.UnescoSite
import com.myexperience.app.data.distanceFrom
import com.myexperience.app.ui.components.*
import com.myexperience.app.ui.theme.BrandBrush
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Una collezione di bollini (patrimoni UNESCO, musei…): raccolti, da completare con la foto, da scoprire. */
@Composable
fun UnescoScreen(vm: MainViewModel, collectionId: String, onBack: () -> Unit) {
    val collection = BadgeCollections.firstOrNull { it.id == collectionId } ?: BadgeCollections.first()
    val sites = collection.items
    val badges by vm.badges.collectAsStateWithLifecycle()
    val photos by vm.badgePhotos.collectAsStateWithLifecycle()
    val seen by vm.seenBadges.collectAsStateWithLifecycle()
    val fromAlbums by vm.albumBadges.collectAsStateWithLifecycle()
    val places by vm.places.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val location by rememberDeviceLocation(hasLocationPermission(context))
    var filter by rememberSaveable { mutableIntStateOf(0) } // 0 tutti, 1 raccolti, 2 da completare, 3 da scoprire
    var query by rememberSaveable { mutableStateOf("") }
    var detail by remember { mutableStateOf<UnescoSite?>(null) }

    fun visited(id: String) = id !in badges && (id in seen || id in fromAlbums)

    val shown = remember(badges, seen, fromAlbums, filter, query, location) {
        sites
            .filter {
                when (filter) {
                    1 -> it.id in badges
                    2 -> visited(it.id)
                    3 -> it.id !in badges && !visited(it.id)
                    else -> true
                }
            }
            .filter { query.isBlank() || it.name.contains(query, true) || it.region.contains(query, true) }
            .let { list ->
                val loc = location
                if (loc != null) list.sortedBy { it.distanceFrom(loc.latitude, loc.longitude) } else list.sortedBy { it.name }
            }
    }
    val total = sites.size
    val got = sites.count { it.id in badges }
    val toPaste = sites.count { visited(it.id) }

    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 40.dp),
    ) {
        item {
            Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Indietro") }
                Text(collection.title, style = MaterialTheme.typography.headlineMedium.copy(brush = BrandBrush), fontWeight = FontWeight.Black)
                Text(collection.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$got", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text(" / $total bollini", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(6.dp))
                Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                    Box(
                        Modifier.fillMaxWidth(if (total == 0) 0f else got.toFloat() / total).fillMaxHeight()
                            .clip(RoundedCornerShape(50)).background(BrandBrush),
                    )
                }
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    query, { query = it },
                    placeholder = { Text("Cerca per nome o luogo") },
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                    singleLine = true, shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { SelectChip("Tutti · $total", filter == 0) { filter = 0 } }
                    item { SelectChip("Raccolti · $got", filter == 1) { filter = 1 } }
                    if (toPaste > 0) item { SelectChip("Manca la foto · $toPaste", filter == 2) { filter = 2 } }
                    item { SelectChip("Da scoprire · ${total - got - toPaste}", filter == 3) { filter = 3 } }
                }
            }
        }
        if (shown.isEmpty()) {
            item {
                Text(
                    if (filter == 1) "Nessun bollino ancora: visita un luogo della collezione e incolla una tua foto per iniziare."
                    else "Nessun elemento trovato.",
                    textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                )
            }
        }
        items(shown, key = { it.id }) { site ->
            val at = badges[site.id]
            val wasHere = visited(site.id)
            Row(
                Modifier
                    .animateItem()
                    .padding(horizontal = 16.dp, vertical = 5.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .bounceClick { detail = site }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                UnescoBadge(site, at != null, 64.dp, visited = wasHere)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(site.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${site.region} · ${site.year}" + (if (site.natural) " · naturale" else ""),
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    val sub = when {
                        at != null -> "Raccolto il ${formatDay(at)}"
                        wasHere -> "Ci sei passato · incolla la foto"
                        location != null -> "A " + formatDistance(site.distanceFrom(location!!.latitude, location!!.longitude))
                        else -> null
                    }
                    if (sub != null) {
                        Text(
                            sub, style = MaterialTheme.typography.labelMedium,
                            color = if (at != null || wasHere) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (at != null || wasHere) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
                if (at != null) Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }

    detail?.let { site ->
        val albumId = fromAlbums[site.id]
        val album = places.orEmpty().firstOrNull { it.place.id == albumId }
        BadgeDetailDialog(
            site = site,
            collectedAt = badges[site.id],
            photo = photos[site.id],
            seenAt = seen[site.id],
            albumTitle = album?.place?.title,
            albumPhotos = album?.media?.filter { !it.isVideo }?.map { it.path }.orEmpty(),
            onPaste = { uri, at -> vm.collectBadge(site.id, uri, at) },
            onUnstick = { vm.unstickBadge(site.id) },
            onForget = { vm.forgetVisit(site.id) },
            onDismiss = { detail = null },
        )
    }
}

/**
 * Scheda di un bollino. Raccolto: la foto con il bollino come timbro, e si può scollare.
 * Visitato (posizione, album o bollino scollato): si incolla la foto, anche da quelle dell'album.
 * Mai visto: si può raccogliere lo stesso se ci sei stato in passato, incollando una tua foto.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BadgeDetailDialog(
    site: UnescoSite,
    collectedAt: Long?,
    photo: String?,
    seenAt: Long?,
    albumTitle: String?,
    albumPhotos: List<String>,
    onPaste: (Uri, Long?) -> Unit,
    onUnstick: () -> Unit,
    onForget: () -> Unit,
    onDismiss: () -> Unit,
) {
    var confirmUnstick by remember { mutableStateOf(false) }
    var confirmForget by remember { mutableStateOf(false) }
    // Visita passata segnata a mano: prima il giorno, poi la foto.
    var askDate by remember { mutableStateOf(false) }
    var pastDate by remember { mutableStateOf<Long?>(null) }
    val pick = rememberBadgePhotoPicker { onPaste(it, pastDate); onDismiss() }
    val collected = collectedAt != null && photo != null
    val visited = !collected && (seenAt != null || albumTitle != null)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Chiudi") } },
        dismissButton = when {
            collected -> {
                { TextButton(onClick = { confirmUnstick = true }) { Text("Scolla il bollino", color = MaterialTheme.colorScheme.error) } }
            }
            seenAt != null -> {
                { TextButton(onClick = { confirmForget = true }) { Text("Annulla il passaggio", color = MaterialTheme.colorScheme.error) } }
            }
            else -> null
        },
        text = {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (collected) {
                    StampedPhoto(site, photo!!, Modifier.padding(bottom = 12.dp))
                } else {
                    UnescoBadge(site, false, 180.dp, visited = visited)
                }
                Spacer(Modifier.height(16.dp))
                Text(site.name, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                Text(
                    "${site.region} · " + if (site.label == "UNESCO") "patrimonio ${if (site.natural) "naturale" else "culturale"} dal ${site.year}" else "aperto dal ${site.year}",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    when {
                        collected -> "Bollino raccolto il ${formatDay(collectedAt!!)}"
                        seenAt != null -> "Ci sei passato il ${formatDay(seenAt)}: incolla una tua foto per raccogliere il bollino"
                        albumTitle != null -> "Ci sei stato nell'album «$albumTitle»: incolla una foto per raccogliere il bollino"
                        else -> "Bollino da scoprire: ${site.points.size} ${if (site.points.size == 1) "punto" else "punti"} di raccolta"
                    },
                    fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                    color = if (collected || visited) MaterialTheme.colorScheme.primary else Color.Gray,
                )
                if (!collected) {
                    if (albumPhotos.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text("Scegli una foto dell'album", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(albumPhotos) { path ->
                                AsyncImage(
                                    model = File(path), contentDescription = null, contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(72.dp).clip(RoundedCornerShape(14.dp))
                                        .bounceClick { onPaste(Uri.fromFile(File(path)), null); onDismiss() },
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(BrandBrush)
                            .bounceClick(onClick = { if (visited) pick() else askDate = true }).padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (visited) "Incolla una foto dalla galleria" else "Ci sono stato in passato: scegli il giorno e incolla una foto",
                            color = Color.White, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        },
    )

    if (confirmUnstick) {
        AlertDialog(
            onDismissRequest = { confirmUnstick = false },
            title = { Text("Scollare il bollino?") },
            text = {
                Column {
                    Text("La foto viene tolta dalla collezione. Il passaggio resta segnato: potrai riattaccare il bollino con un'altra foto quando vuoi.")
                    Spacer(Modifier.height(8.dp))
                    Text("Se il passaggio era sbagliato, annullalo: il bollino torna da scoprire.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Column(horizontalAlignment = Alignment.End) {
                    TextButton(onClick = { onUnstick(); confirmUnstick = false; onDismiss() }) {
                        Text("Scolla", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { onForget(); confirmUnstick = false; onDismiss() }) {
                        Text("Scolla e annulla il passaggio", color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(onClick = { confirmUnstick = false }) { Text("Annulla") }
                }
            },
        )
    }

    if (confirmForget) {
        AlertDialog(
            onDismissRequest = { confirmForget = false },
            title = { Text("Annullare il passaggio?") },
            text = { Text("Il passaggio del ${formatDay(seenAt ?: 0)} viene tolto: il bollino torna da scoprire.") },
            confirmButton = {
                TextButton(onClick = { onForget(); confirmForget = false; onDismiss() }) {
                    Text("Annulla il passaggio", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmForget = false }) { Text("Indietro") } },
        )
    }

    if (askDate) {
        val today = System.currentTimeMillis()
        val state = rememberDatePickerState(
            initialSelectedDateMillis = today,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= today
            },
        )
        DatePickerDialog(
            onDismissRequest = { askDate = false },
            confirmButton = {
                TextButton(onClick = {
                    // A mezzogiorno del giorno scelto: il fuso orario non lo sposta al giorno prima.
                    pastDate = state.selectedDateMillis?.plus(12 * 60 * 60 * 1000L)
                    askDate = false
                    pick()
                }) { Text("Scegli la foto") }
            },
            dismissButton = { TextButton(onClick = { askDate = false }) { Text("Annulla") } },
        ) {
            DatePicker(state, title = { Text("Quando ci sei stato?", Modifier.padding(start = 24.dp, top = 16.dp)) })
        }
    }
}

private fun formatDay(ms: Long): String = SimpleDateFormat("d MMM yyyy", Locale.ITALIAN).format(Date(ms))

private fun formatDistance(m: Double): String =
    if (m < 1000) "${m.toInt()} m" else if (m < 100_000) String.format(Locale.ITALIAN, "%.1f km", m / 1000) else "${(m / 1000).toInt()} km"
