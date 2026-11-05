package com.myexperience.app.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Luggage
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myexperience.app.MainViewModel
import com.myexperience.app.data.TripWithDetails
import com.myexperience.app.ui.components.*
import com.myexperience.app.ui.theme.BrandBrush
import com.myexperience.app.ui.theme.TripGradients

@Composable
fun TripsScreen(vm: MainViewModel, onOpenTrip: (Long) -> Unit, onNewTrip: () -> Unit) {
    val trips by vm.trips.collectAsStateWithLifecycle()
    val list = trips

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 200.dp),
        ) {
            item {
                Column(
                    Modifier
                        .statusBarsPadding()
                        .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
                ) {
                    Text("Viaggi da organizzare", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "Pianifica mete, tappe e checklist della prossima avventura",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!list.isNullOrEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            InfoChip("${list.size} viaggi", icon = Icons.Rounded.Luggage)
                            InfoChip("${list.sumOf { it.destinations.size }} mete", icon = Icons.Rounded.Place)
                            val todo = list.sumOf { t -> t.tasks.count { !it.done } }
                            if (todo > 0) InfoChip("$todo da fare", icon = Icons.Rounded.Checklist)
                        }
                    }
                }
            }
            when {
                list == null -> Unit
                list.isEmpty() -> item {
                    EmptyState(
                        Icons.Rounded.Map, "Nessun viaggio in programma",
                        "Crea un viaggio, aggiungi le mete sulla mappa, organizza date, budget e checklist.",
                        actionText = "Organizza un viaggio", onAction = onNewTrip,
                    )
                }
                else -> itemsIndexed(list, key = { _, t -> t.trip.id }) { i, t ->
                    TripCard(
                        t,
                        Modifier
                            .animateItem()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .appear(i),
                    ) { onOpenTrip(t.trip.id) }
                }
            }
        }

        if (!list.isNullOrEmpty()) {
            GradientButton(
                "Nuovo viaggio",
                Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 20.dp, bottom = 100.dp),
                icon = Icons.Rounded.Add,
                onClick = onNewTrip,
            )
        }
    }
}

@Composable
fun TripCard(t: TripWithDetails, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = TripGradients[t.trip.colorIndex.mod(TripGradients.size)]
    val total = t.tasks.size
    val done = t.tasks.count { it.done }
    val progress by animateFloatAsState(
        if (total == 0) 0f else done / total.toFloat(),
        tween(900, easing = FastOutSlowInEasing),
        label = "progress",
    )
    val inf = rememberInfiniteTransition(label = "card")
    val rot by inf.animateFloat(
        -8f, 8f, infiniteRepeatable(tween(3000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "rot",
    )

    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(Brush.linearGradient(colors))
            .bounceClick(onClick = onClick),
    ) {
        Icon(
            transportIcon(t.trip.transport), null, tint = Color.White,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 24.dp)
                .size(120.dp)
                .graphicsLayer { alpha = 0.22f; rotationZ = rot },
        )
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassChip(countdownLabel(t.trip.startDate, t.trip.endDate), icon = Icons.Rounded.HourglassTop)
                Spacer(Modifier.width(8.dp))
                if (t.destinations.isNotEmpty()) GlassChip("${t.destinations.size}", icon = Icons.Rounded.Place)
            }
            Spacer(Modifier.height(14.dp))
            Text(
                t.trip.name, color = Color.White, style = MaterialTheme.typography.headlineSmall,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            Text(
                formatRange(t.trip.startDate, t.trip.endDate),
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (t.destinations.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    t.destinations.joinToString("  →  ") { it.name },
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (total > 0) {
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.3f)),
                    ) {
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("$done/$total pronti", color = Color.White, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

/** Piccolo indicatore a gradiente usato nelle sezioni. */
@Composable
fun GradientDot(modifier: Modifier = Modifier) {
    Box(modifier.size(10.dp).clip(RoundedCornerShape(5.dp)).background(BrandBrush))
}
