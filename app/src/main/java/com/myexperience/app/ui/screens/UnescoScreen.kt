package com.myexperience.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myexperience.app.MainViewModel
import com.myexperience.app.data.UnescoSite
import com.myexperience.app.data.UnescoSites
import com.myexperience.app.data.distanceFrom
import com.myexperience.app.ui.components.*
import com.myexperience.app.ui.theme.BrandBrush
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Elenco dei patrimoni UNESCO mappati (per ora solo in Italia) con i bollini raccolti. */
@Composable
fun UnescoScreen(vm: MainViewModel, onBack: () -> Unit) {
    val badges by vm.badges.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val location by rememberDeviceLocation(hasLocationPermission(context))
    var filter by rememberSaveable { mutableIntStateOf(0) } // 0 tutti, 1 raccolti, 2 da scoprire
    var query by rememberSaveable { mutableStateOf("") }
    var detail by remember { mutableStateOf<UnescoSite?>(null) }

    val shown = remember(badges, filter, query, location) {
        UnescoSites
            .filter {
                when (filter) {
                    1 -> it.id in badges
                    2 -> it.id !in badges
                    else -> true
                }
            }
            .filter { query.isBlank() || it.name.contains(query, true) || it.region.contains(query, true) }
            .let { list ->
                val loc = location
                if (loc != null) list.sortedBy { it.distanceFrom(loc.latitude, loc.longitude) } else list.sortedBy { it.name }
            }
    }
    val total = UnescoSites.size
    val got = UnescoSites.count { it.id in badges }

    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 40.dp),
    ) {
        item {
            Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Indietro") }
                Text("Patrimoni UNESCO", style = MaterialTheme.typography.headlineMedium.copy(brush = BrandBrush), fontWeight = FontWeight.Black)
                Text(
                    "Italia · avvicinati a meno di 200 m da un patrimonio per raccoglierne il bollino",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
                    placeholder = { Text("Cerca per nome o regione") },
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                    singleLine = true, shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { SelectChip("Tutti · $total", filter == 0) { filter = 0 } }
                    item { SelectChip("Raccolti · $got", filter == 1) { filter = 1 } }
                    item { SelectChip("Da scoprire · ${total - got}", filter == 2) { filter = 2 } }
                }
            }
        }
        if (shown.isEmpty()) {
            item {
                Text(
                    if (filter == 1) "Nessun bollino ancora: visita un patrimonio UNESCO per iniziare la collezione."
                    else "Nessun patrimonio trovato.",
                    textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                )
            }
        }
        items(shown, key = { it.id }) { site ->
            val at = badges[site.id]
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
                UnescoBadge(site, at != null, 64.dp)
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
                        location != null -> "A " + formatDistance(site.distanceFrom(location!!.latitude, location!!.longitude))
                        else -> null
                    }
                    if (sub != null) {
                        Text(
                            sub, style = MaterialTheme.typography.labelMedium,
                            color = if (at != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (at != null) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
                if (at != null) Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }

    detail?.let { site ->
        val at = badges[site.id]
        AlertDialog(
            onDismissRequest = { detail = null },
            confirmButton = { TextButton(onClick = { detail = null }) { Text("Chiudi") } },
            text = {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    UnescoBadge(site, at != null, 180.dp)
                    Spacer(Modifier.height(16.dp))
                    Text(site.name, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                    Text(
                        "${site.region} · patrimonio ${if (site.natural) "naturale" else "culturale"} dal ${site.year}",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (at != null) "Bollino raccolto il ${formatDay(at)}"
                        else "Bollino da scoprire: ${site.points.size} ${if (site.points.size == 1) "punto" else "punti"} di raccolta",
                        fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        color = if (at != null) MaterialTheme.colorScheme.primary else Color.Gray,
                    )
                }
            },
        )
    }
}

private fun formatDay(ms: Long): String = SimpleDateFormat("d MMM yyyy", Locale.ITALIAN).format(Date(ms))

private fun formatDistance(m: Double): String =
    if (m < 1000) "${m.toInt()} m" else if (m < 100_000) String.format(Locale.ITALIAN, "%.1f km", m / 1000) else "${(m / 1000).toInt()} km"
