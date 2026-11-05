package com.myexperience.app.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.myexperience.app.data.UnescoSite
import com.myexperience.app.ui.theme.BrandBrush
import kotlinx.coroutines.delay

fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

/** Posizione del telefono aggiornata finché [enabled] è vero (GPS e rete, senza Play Services). */
@SuppressLint("MissingPermission")
@Composable
fun rememberDeviceLocation(enabled: Boolean): State<Location?> {
    val context = LocalContext.current
    val state = remember { mutableStateOf<Location?>(null) }
    DisposableEffect(enabled) {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (!enabled || lm == null || !hasLocationPermission(context)) return@DisposableEffect onDispose { }
        val listener = LocationListener { loc ->
            val cur = state.value
            // tiene la posizione più precisa tra quelle recenti
            if (cur == null || loc.time - cur.time > 20_000 || loc.accuracy <= cur.accuracy) state.value = loc
        }
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }
        providers.forEach { p ->
            runCatching { lm.getLastKnownLocation(p) }.getOrNull()?.let { last ->
                if (System.currentTimeMillis() - last.time < 2 * 60_000) listener.onLocationChanged(last)
            }
            runCatching { lm.requestLocationUpdates(p, 10_000L, 15f, listener, Looper.getMainLooper()) }
        }
        onDispose { runCatching { lm.removeUpdates(listener) } }
    }
    return state
}

/** Pop-up che propone di raccogliere il bollino di un patrimonio UNESCO vicino. */
@Composable
fun CollectBadgeDialog(site: UnescoSite, onCollect: () -> Unit, onLater: () -> Unit) {
    var collected by remember { mutableStateOf(false) }
    val appear = remember { Animatable(0f) }
    val flip = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow)) }
    LaunchedEffect(collected) {
        if (collected) {
            flip.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
            delay(1400)
            onCollect()
        }
    }
    val glow = rememberInfiniteTransition(label = "glow")
    val pulse by glow.animateFloat(0.92f, 1.06f, infiniteRepeatable(tween(1300), RepeatMode.Reverse), label = "p")

    Dialog(onDismissRequest = { if (!collected) onLater() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier
                .padding(28.dp)
                .fillMaxWidth()
                .shadow(30.dp, RoundedCornerShape(32.dp))
                .clip(RoundedCornerShape(32.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF1E2A2E), Color(0xFF0F1517))))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                if (collected) "BOLLINO RACCOLTO" else "PATRIMONIO UNESCO QUI VICINO",
                color = Color(0xFFF7DC8A), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp,
            )
            Spacer(Modifier.height(18.dp))
            Box(
                Modifier.graphicsLayer {
                    val s = appear.value * (if (collected) 1f else pulse)
                    scaleX = s; scaleY = s
                    rotationY = flip.value * 360f
                    cameraDistance = 14f * density
                },
            ) {
                // durante la rotazione il bollino si "colora"
                UnescoBadge(site, collected = flip.value > 0.5f, size = 170.dp)
            }
            Spacer(Modifier.height(18.dp))
            Text(
                site.name, color = Color.White, style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Text(
                "${site.region} · patrimonio dal ${site.year}",
                color = Color.White.copy(alpha = 0.65f), style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            if (collected) {
                Text("Aggiunto alla tua collezione nel profilo", color = Color.White.copy(alpha = 0.85f), textAlign = TextAlign.Center)
            } else {
                Text(
                    "Sei a meno di 200 m da questo patrimonio dell'umanità. Raccogli il suo bollino!",
                    color = Color.White.copy(alpha = 0.85f), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(20.dp))
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(BrandBrush)
                        .bounceClick { collected = true }.padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) { Text("Raccogli il bollino", color = Color.White, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Più tardi", color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).bounceClick(onClick = onLater).padding(10.dp),
                )
            }
        }
    }
}
