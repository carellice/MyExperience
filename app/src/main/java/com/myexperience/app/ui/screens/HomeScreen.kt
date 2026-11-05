package com.myexperience.app.ui.screens

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myexperience.app.MainViewModel
import com.myexperience.app.data.PickedLocation
import com.myexperience.app.ui.components.*
import com.myexperience.app.ui.globe.GlobeMarker
import com.myexperience.app.ui.globe.GlobeView
import com.myexperience.app.ui.theme.BrandBrush
import com.myexperience.app.ui.theme.Pink
import kotlinx.coroutines.launch

private sealed interface GlobeSelection {
    data class Album(val placeId: Long) : GlobeSelection
    data class Trip(val tripId: Long, val destId: Long) : GlobeSelection
    data class NewSpot(val loc: PickedLocation, val resolving: Boolean) : GlobeSelection
}

/** Home: solo il globo 3D, a tutto schermo, senza scorrimento. */
@Composable
fun HomeScreen(
    vm: MainViewModel,
    onOpenPlace: (Long) -> Unit,
    onOpenTrip: (Long) -> Unit,
    onAddAt: (PickedLocation) -> Unit,
) {
    val places by vm.places.collectAsStateWithLifecycle()
    val trips by vm.trips.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var selection by remember { mutableStateOf<GlobeSelection?>(null) }
    var ready by remember { mutableStateOf(false) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val context = LocalContext.current

    // Icone di sistema chiare sopra lo spazio
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        val ctrl = window?.let { WindowCompat.getInsetsController(it, view) }
        val prevStatus = ctrl?.isAppearanceLightStatusBars
        val prevNav = ctrl?.isAppearanceLightNavigationBars
        ctrl?.isAppearanceLightStatusBars = false
        ctrl?.isAppearanceLightNavigationBars = false
        onDispose {
            if (prevStatus != null) ctrl.isAppearanceLightStatusBars = prevStatus
            if (prevNav != null) ctrl.isAppearanceLightNavigationBars = prevNav
        }
    }

    val placeList = places.orEmpty()
    val markers = remember(places, trips, selection) {
        val list = mutableListOf<GlobeMarker>()
        placeList.forEach { list += GlobeMarker(it.place.id, it.place.latitude, it.place.longitude, 0) }
        trips.orEmpty().forEach { t ->
            t.destinations.filter { !it.visited }.forEach { d ->
                list += GlobeMarker(-d.id, d.latitude, d.longitude, 1)
            }
        }
        (selection as? GlobeSelection.NewSpot)?.let {
            list += GlobeMarker(Long.MIN_VALUE, it.loc.latitude, it.loc.longitude, 2)
        }
        list
    }
    val selectedMarkerId = when (val s = selection) {
        is GlobeSelection.Album -> s.placeId
        is GlobeSelection.Trip -> -s.destId
        else -> null
    }

    var globeRef by remember { mutableStateOf<GlobeView?>(null) }

    Box(Modifier.fillMaxSize().background(Color(0xFF02030A))) {
        AndroidView(
            factory = { ctx ->
                GlobeView(ctx, vm.globeCamera).also { g ->
                    g.onReady = { ready = true }
                    globeRef = g
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { g ->
                g.setMarkers(markers, selectedMarkerId)
                g.setAutoRotate(selection == null)
                g.onMarkerTap = { m ->
                    selection = when (m.kind) {
                        0 -> GlobeSelection.Album(m.id)
                        1 -> {
                            val destId = -m.id
                            val trip = trips.orEmpty().firstOrNull { t -> t.destinations.any { it.id == destId } }
                            trip?.let { GlobeSelection.Trip(it.trip.id, destId) }
                        }
                        else -> selection
                    }
                    g.flyTo(m.lat, m.lon, true)
                }
                g.onEmptyTap = { selection = null }
                g.onLongPress = { lat, lon ->
                    val temp = PickedLocation(lat, lon, "…", "")
                    selection = GlobeSelection.NewSpot(temp, true)
                    scope.launch {
                        val loc = vm.reverse(lat, lon)
                        if ((selection as? GlobeSelection.NewSpot)?.loc == temp) {
                            selection = GlobeSelection.NewSpot(loc, false)
                        }
                    }
                }
            },
        )

        DisposableEffect(lifecycle, globeRef) {
            val g = globeRef
            val obs = LifecycleEventObserver { _, e ->
                when (e) {
                    Lifecycle.Event.ON_RESUME -> g?.onResume()
                    Lifecycle.Event.ON_PAUSE -> g?.onPause()
                    else -> Unit
                }
            }
            lifecycle.addObserver(obs)
            onDispose { lifecycle.removeObserver(obs) }
        }

        // velo di caricamento
        AnimatedVisibility(!ready, enter = fadeIn(), exit = fadeOut(tween(900))) {
            Box(Modifier.fillMaxSize().background(Color(0xFF02030A)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FloatingEmoji("🌍", 56)
                    Spacer(Modifier.height(12.dp))
                    Text("Preparo il tuo mondo…", color = Color.White.copy(alpha = 0.7f))
                }
            }
        }

        // intestazione
        val countries = placeList.map { it.place.country }.filter { it.isNotBlank() }.distinct().size
        Column(
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent)))
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 24.dp),
        ) {
            Text("MyExperience", style = MaterialTheme.typography.headlineMedium.copy(brush = BrandBrush))
            Text(
                when {
                    placeList.isEmpty() -> "Il tuo mondo ti aspetta"
                    else -> "${placeList.size} ${if (placeList.size == 1) "ricordo" else "ricordi"} · " +
                        "$countries ${if (countries == 1) "paese" else "paesi"}"
                },
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LegendDot(Color(0xFFFF4D85), "Visitati")
                if (trips.orEmpty().any { t -> t.destinations.any { !it.visited } }) {
                    LegendDot(Color(0xFF33D9F2), "Da visitare")
                }
            }
        }

        // pannello inferiore
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 100.dp),
        ) {
            AnimatedContent(
                targetState = selection,
                transitionSpec = {
                    (slideInVertically(spring(dampingRatio = 0.8f)) { it / 2 } + fadeIn()) togetherWith
                        (slideOutVertically { it / 2 } + fadeOut())
                },
                contentKey = { s ->
                    when (s) {
                        is GlobeSelection.Album -> "a${s.placeId}"
                        is GlobeSelection.Trip -> "t${s.destId}"
                        is GlobeSelection.NewSpot -> "n"
                        null -> "none"
                    }
                },
                label = "globeCard",
            ) { s ->
                when (s) {
                    null -> {
                        if (ready) {
                            HintCard(
                                if (placeList.isEmpty())
                                    "Tieni premuto su un punto del globo per creare il tuo primo album di viaggio"
                                else "Tocca un punto luminoso per aprire l'album · tieni premuto per aggiungere un ricordo"
                            )
                        } else Spacer(Modifier.height(1.dp))
                    }
                    is GlobeSelection.Album -> {
                        val p = placeList.firstOrNull { it.place.id == s.placeId }
                        if (p != null) {
                            AlbumPreviewCard(
                                p = p,
                                onOpen = { onOpenPlace(p.place.id) },
                                onClose = { selection = null },
                                onLike = { vm.toggleFavorite(p.place) },
                            )
                        }
                    }
                    is GlobeSelection.Trip -> {
                        val t = trips.orEmpty().firstOrNull { it.trip.id == s.tripId }
                        val d = t?.destinations?.firstOrNull { it.id == s.destId }
                        if (t != null && d != null) {
                            GlassPanel(onClick = { onOpenTrip(t.trip.id) }) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        Modifier.size(56.dp).clip(RoundedCornerShape(18.dp))
                                            .background(Brush.linearGradient(listOf(Color(0xFF22C1C3), Color(0xFF7B5CFF)))),
                                        contentAlignment = Alignment.Center,
                                    ) { Text("🧳", fontSize = 26.sp) }
                                    Spacer(Modifier.width(14.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text("DA VISITARE", color = Color(0xFF33D9F2), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                                        Text("${countryFlag(d.country)} ${d.name}", color = Color.White, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "${t.trip.name} · ${countdownLabel(t.trip.startDate, t.trip.endDate)}",
                                            color = Color.White.copy(alpha = 0.75f),
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    Text("Apri →", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    is GlobeSelection.NewSpot -> {
                        GlassPanel {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(52.dp).clip(CircleShape).background(BrandBrush),
                                    contentAlignment = Alignment.Center,
                                ) { Text("📍", fontSize = 24.sp) }
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("NUOVO RICORDO", color = Pink, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                                    if (s.resolving) {
                                        Text("Cerco il luogo…", color = Color.White, style = MaterialTheme.typography.titleMedium)
                                    } else {
                                        Text(
                                            "${countryFlag(s.loc.country)} ${s.loc.name}",
                                            color = Color.White, style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                                        )
                                        if (s.loc.country.isNotBlank()) {
                                            Text(s.loc.country, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                                Icon(
                                    Icons.Rounded.Close, "Annulla", tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(end = 8.dp).size(22.dp).bounceClick { selection = null },
                                )
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (s.resolving) Brush.linearGradient(listOf(Color.Gray, Color.Gray)) else BrandBrush)
                                        .bounceClick(enabled = !s.resolving) {
                                            onAddAt(s.loc)
                                            selection = null
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Add, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Crea", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.1f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
    }
}

@Composable
private fun HintCard(text: String) {
    val t = rememberInfiniteTransition(label = "hint")
    val a by t.animateFloat(0.55f, 0.95f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "a")
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text,
            color = Color.White.copy(alpha = a),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 16.dp, vertical = 9.dp),
        )
    }
}

@Composable
private fun GlassPanel(onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .shadow(24.dp, RoundedCornerShape(28.dp), ambientColor = Color.Black, spotColor = Color.Black)
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xE6151726))
            .then(if (onClick != null) Modifier.bounceClick(onClick = onClick) else Modifier)
            .padding(14.dp),
    ) { content() }
}

@Composable
private fun AlbumPreviewCard(
    p: com.myexperience.app.data.PlaceWithMedia,
    onOpen: () -> Unit,
    onClose: () -> Unit,
    onLike: () -> Unit,
) {
    val place = p.place
    GlassPanel(onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CoverImage(
                place.coverPath, place.id,
                Modifier.size(78.dp).clip(RoundedCornerShape(20.dp)),
                emoji = categoryEmoji(place.category), emojiSize = 30,
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "${countryFlag(place.country)} ${place.locationName}".uppercase(),
                    color = Color(0xFFFF4D85), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    place.title, color = Color.White, style = MaterialTheme.typography.titleMedium,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    formatRange(place.startDate, place.endDate) + " · ${p.media.size} media",
                    color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(6.dp))
                Text("Apri l'album →", style = MaterialTheme.typography.labelLarge.copy(brush = BrandBrush))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Rounded.Close, "Chiudi", tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(22.dp).bounceClick(onClick = onClose),
                )
                Spacer(Modifier.height(18.dp))
                LikeButton(place.favorite, onLike, tintOff = Color.White)
            }
        }
    }
}

@Composable
fun LikeButton(liked: Boolean, onClick: () -> Unit, tintOff: Color = MaterialTheme.colorScheme.onSurface) {
    val scale = remember { Animatable(1f) }
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(liked) {
        if (first) {
            first = false
            return@LaunchedEffect
        }
        if (liked) {
            scale.animateTo(1.35f, tween(120))
            scale.animateTo(1f, spring(dampingRatio = 0.3f, stiffness = Spring.StiffnessMedium))
        }
    }
    Icon(
        if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
        contentDescription = "Preferito",
        tint = if (liked) Pink else tintOff,
        modifier = Modifier
            .size(26.dp)
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            .bounceClick(onClick = onClick),
    )
}
