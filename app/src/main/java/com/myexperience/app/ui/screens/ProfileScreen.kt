package com.myexperience.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PhotoAlbum
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myexperience.app.MainViewModel
import com.myexperience.app.data.PlaceWithMedia
import com.myexperience.app.data.BadgeCollection
import com.myexperience.app.data.BadgeCollections
import com.myexperience.app.ui.components.*
import com.myexperience.app.ui.journal.themeById
import com.myexperience.app.ui.theme.BrandBrush
import com.myexperience.app.ui.theme.Forest
import com.myexperience.app.ui.theme.Sand
import com.myexperience.app.ui.theme.Sea
import com.myexperience.app.ui.theme.Terracotta
import com.myexperience.app.update.AppUpdater
import com.myexperience.app.update.UpdateState
import kotlinx.coroutines.launch

/** Profilo: chi sei, statistiche e tutti i tuoi album di viaggio. */
@Composable
fun ProfileScreen(
    vm: MainViewModel,
    onOpenPlace: (Long) -> Unit,
    onAdd: () -> Unit,
    onOpenCollection: (String) -> Unit,
) {
    val places by vm.places.collectAsStateWithLifecycle()
    val badges by vm.badges.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val all = places.orEmpty()
    var editing by remember { mutableStateOf(false) }
    var filter by rememberSaveable { mutableStateOf<String?>(null) } // paese, "♥" = preferiti
    // Come su Instagram: sotto la bio due icone, gli album e le collezioni di bollini.
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var settings by remember { mutableStateOf(false) }

    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.setAvatar(uri)
    }
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.setProfileCover(uri)
    }

    val countries = remember(all) {
        all.flatMap { it.countries }.groupingBy { it }.eachCount()
            .entries.sortedByDescending { it.value }
    }
    val shown = remember(all, filter) {
        when (filter) {
            null -> all
            "♥" -> all.filter { it.place.favorite }
            else -> all.filter { filter in it.countries }
        }
    }
    val byYear = remember(shown) {
        shown.groupBy { it.place.startDate.toLocalDateUtc().year }.toSortedMap(compareByDescending { it })
    }
    val photoCount = all.sumOf { p -> p.media.count { !it.isVideo } }
    val videoCount = all.sumOf { p -> p.media.count { it.isVideo } }
    val days = all.sumOf { daysBetween(it.place.startDate, it.place.endDate).toInt() }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 130.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Box(Modifier.fillMaxWidth().padding(horizontal = 0.dp)) {
                val t = rememberInfiniteTransition(label = "banner")
                val shift by t.animateFloat(0f, 1f, infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Reverse), label = "s")
                Box(
                    Modifier
                        .padding(top = 0.dp)
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp, topStart = 0.dp, topEnd = 0.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Sea, Forest, Sand),
                                start = Offset(shift * 600f, 0f),
                                end = Offset(1200f - shift * 400f, 700f),
                            )
                        ),
                ) {
                    val cover = profile.coverPath
                    if (cover != null) {
                        AsyncImage(
                            model = java.io.File(cover), contentDescription = "Copertina",
                            contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                        )
                        // velo in basso: l'avatar resta leggibile sopra la foto
                        Box(
                            Modifier.fillMaxSize().background(
                                Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.25f), Color.Transparent, Color.Black.copy(alpha = 0.35f)))
                            ),
                        )
                    } else {
                        Icon(
                            Icons.Rounded.Public, null, tint = Color.White,
                            modifier = Modifier.align(Alignment.CenterEnd).offset(x = 30.dp).size(160.dp)
                                .graphicsLayer { alpha = 0.18f; rotationZ = shift * 30f },
                        )
                    }
                    Row(
                        Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            Modifier.clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.35f))
                                .bounceClick { coverPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.AddAPhoto, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Copertina", color = Color.White, style = MaterialTheme.typography.labelMedium)
                        }
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier.size(34.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.35f)).bounceClick { settings = true },
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Rounded.Settings, "Impostazioni", tint = Color.White, modifier = Modifier.size(19.dp)) }
                    }
                }
                Column(Modifier.fillMaxWidth().padding(top = 128.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box {
                        StoryRing(112.dp, Modifier.bounceClick {
                            avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }) { Avatar(profile.avatarPath, profile.name, 100.dp) }
                        Box(
                            Modifier.align(Alignment.BottomEnd).size(32.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Rounded.AddAPhoto, null, Modifier.size(17.dp)) }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(profile.name, style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Rounded.Edit, "Modifica profilo", tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp).bounceClick { editing = true },
                        )
                    }
                    Text(
                        profile.bio, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                    )
                }
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surface).padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                AnimatedCounter(all.size, "Album")
                AnimatedCounter(countries.size, "Paesi")
                AnimatedCounter(days, "Giorni")
                AnimatedCounter(photoCount, "Foto")
                AnimatedCounter(videoCount, "Video")
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }, key = "tabs") {
            ProfileTabs(tab) { tab = it }
        }

        if (tab == 1) {
            BadgeCollections.forEachIndexed { i, collection ->
                item(span = { GridItemSpan(maxLineSpan) }, key = "c${collection.id}") {
                    BadgeCollectionCard(collection, badges, Modifier.animateItem().appear(i)) { onOpenCollection(collection.id) }
                }
            }
        }

        if (tab == 0) item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                if (all.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { SelectChip("Tutti · ${all.size}", filter == null) { filter = null } }
                        if (all.any { it.place.favorite }) {
                            item { SelectChip("Preferiti", filter == "♥", Icons.Rounded.Favorite) { filter = "♥" } }
                        }
                        items(countries, key = { it.key }) { (c, n) ->
                            SelectChip("${flagPrefix(c)}$c · $n", filter == c) { filter = if (filter == c) null else c }
                        }
                    }
                }
            }
        }

        if (tab == 0 && all.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyState(
                    Icons.Rounded.PhotoAlbum, "Nessun album ancora",
                    "Ogni viaggio diventa un album-diario. Crealo dal globo tenendo premuto su un luogo, oppure con il pulsante +.",
                    actionText = "Crea il primo album", onAction = onAdd,
                )
            }
        }

        if (tab == 0) byYear.forEach { (year, items) ->
            item(span = { GridItemSpan(maxLineSpan) }, key = "y$year") {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
                    Text("$year", style = MaterialTheme.typography.titleLarge.copy(brush = BrandBrush), fontWeight = FontWeight.Black)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${items.size} ${if (items.size == 1) "album" else "album"}",
                        style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(items, key = { it.place.id }) { p ->
                AlbumCard(p, Modifier.animateItem().appear(items.indexOf(p))) { onOpenPlace(p.place.id) }
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                "Immagini della Terra: Solar System Scope (CC BY 4.0) · Mappe © OpenFreeMap, OpenStreetMap contributors",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            )
        }
    }

    if (settings) ProfileSettingsSheet { settings = false }

    if (editing) {
        var name by remember { mutableStateOf(profile.name) }
        var bio by remember { mutableStateOf(profile.bio) }
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text("Modifica profilo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Nome") }, singleLine = true, shape = RoundedCornerShape(16.dp))
                    OutlinedTextField(bio, { bio = it }, label = { Text("Bio") }, minLines = 2, shape = RoundedCornerShape(16.dp))
                }
            },
            confirmButton = { TextButton(onClick = { vm.updateProfile(name.trim(), bio.trim()); editing = false }) { Text("Salva") } },
            dismissButton = { TextButton(onClick = { editing = false }) { Text("Annulla") } },
        )
    }
}

/** Una collezione di bollini: avanzamento e ultimi raccolti. */
@Composable
private fun BadgeCollectionCard(collection: BadgeCollection, badges: Map<String, Long>, modifier: Modifier = Modifier, onOpen: () -> Unit) {
    val sites = collection.items
    val total = sites.size
    val collected = sites.filter { it.id in badges }.sortedByDescending { badges[it.id] }
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surface)
            .bounceClick(onClick = onOpen).padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.WorkspacePremium, null, tint = Color(0xFFD9A63E))
            Spacer(Modifier.width(8.dp))
            Text(collection.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text("${collected.size}/$total", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(4.dp))
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.surfaceVariant)) {
            Box(
                Modifier.fillMaxWidth(collected.size.toFloat() / total).fillMaxHeight()
                    .clip(RoundedCornerShape(50)).background(BrandBrush),
            )
        }
        Spacer(Modifier.height(12.dp))
        if (collected.isEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                sites.take(3).forEach {
                    UnescoBadge(it, false, 52.dp)
                    Spacer(Modifier.width(6.dp))
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    "Visita un luogo della collezione e incolla una tua foto per il primo bollino",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(collected, key = { it.id }) { site ->
                    Column(Modifier.width(84.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        UnescoBadge(site, true, 76.dp)
                        Text(
                            site.name, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center,
                            maxLines = 2, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumCard(p: PlaceWithMedia, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val place = p.place
    val theme = themeById(place.theme)
    Column(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .bounceClick(onClick = onClick),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(0.82f)) {
            CoverImage(place.coverPath, place.id, Modifier.fillMaxSize(), icon = categoryIcon(place.category), iconSize = 44.dp)
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.7f)))
                ),
            )
            Row(Modifier.align(Alignment.TopStart).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                val flags = p.countries.ifEmpty { listOf(place.country) }.map { countryFlag(it) }.filter { it.isNotEmpty() }.take(4)
                if (flags.isNotEmpty()) FlagsChip(flags)
                if (place.favorite) {
                    Box(
                        Modifier.size(30.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Rounded.Favorite, null, tint = Terracotta, modifier = Modifier.size(16.dp)) }
                }
            }
            // "linguetta" con il colore del tema del diario
            Box(
                Modifier.align(Alignment.TopEnd).padding(top = 14.dp).size(width = 14.dp, height = 34.dp)
                    .clip(RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp)).background(theme.accent),
            )
            Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                Text(
                    place.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                    fontFamily = theme.font, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 19.sp,
                )
                val route = p.route
                Text(
                    if (route.size > 1) route.joinToString(" → ") { it.name } else place.locationName,
                    color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                formatRange(place.startDate, place.endDate), style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Icon(Icons.Rounded.PhotoLibrary, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text("${p.media.size}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Le due icone sotto la bio, come su Instagram: gli album e le collezioni, con la riga che scorre sotto quella scelta. */
@Composable
private fun ProfileTabs(selected: Int, onSelect: (Int) -> Unit) {
    val icons = listOf(Icons.Rounded.GridView to "I miei album", Icons.Rounded.MilitaryTech to "Collezioni")
    BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        val each = maxWidth / icons.size
        val offset by animateDpAsState(each * selected, spring(dampingRatio = 0.7f, stiffness = 500f), label = "tab")
        Row(Modifier.fillMaxWidth()) {
            icons.forEachIndexed { i, (icon, label) ->
                val tint by animateColorAsState(
                    if (i == selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    label = "tint",
                )
                Box(
                    Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(14.dp)).bounceClick { onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) { Icon(icon, label, tint = tint, modifier = Modifier.size(26.dp)) }
            }
        }
        Box(
            Modifier.align(Alignment.BottomStart).offset(x = offset + each / 4).width(each / 2).height(3.dp)
                .clip(RoundedCornerShape(50)).background(BrandBrush),
        )
    }
}

/** Le impostazioni dell'app, dall'icona in alto a destra del profilo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileSettingsSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
            Text("Impostazioni", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = 4.dp, bottom = 12.dp))
            UpdatesRow()
        }
    }
}

/** Controlla se su GitHub c'è una versione nuova dell'app. */
@Composable
private fun UpdatesRow() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val update by AppUpdater.state.collectAsStateWithLifecycle()
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .bounceClick { scope.launch { AppUpdater.check(ctx.applicationContext, manual = true) } }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.SystemUpdate, null, tint = Sea)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Controlla aggiornamenti", style = MaterialTheme.typography.titleSmall)
            Text(
                "Versione ${AppUpdater.currentVersion}", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (update is UpdateState.Checking) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
    }
}
