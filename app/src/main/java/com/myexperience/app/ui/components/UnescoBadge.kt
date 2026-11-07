package com.myexperience.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.myexperience.app.data.UnescoSite
import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Icona del bollino di un patrimonio UNESCO. */
fun unescoIcon(key: String): ImageVector = when (key) {
    "art" -> Icons.Rounded.Palette
    "arena" -> Icons.Rounded.Stadium
    "church" -> Icons.Rounded.Church
    "boat" -> Icons.Rounded.Sailing
    "tower" -> Icons.Rounded.Apartment
    "cave" -> Icons.Rounded.Landscape
    "villa" -> Icons.Rounded.Villa
    "factory" -> Icons.Rounded.Factory
    "castle" -> Icons.Rounded.Castle
    "trullo" -> Icons.Rounded.Cottage
    "mosaic" -> Icons.Rounded.AutoAwesomeMosaic
    "palace" -> Icons.Rounded.AccountBalance
    "garden" -> Icons.Rounded.LocalFlorist
    "coast" -> Icons.Rounded.Waves
    "temple" -> Icons.Rounded.AccountBalance
    "fort" -> Icons.Rounded.Fort
    "volcano" -> Icons.Rounded.Volcano
    "fountain" -> Icons.Rounded.Water
    "tomb" -> Icons.Rounded.HistoryEdu
    "hills" -> Icons.Rounded.Terrain
    "train" -> Icons.Rounded.Train
    "mountain" -> Icons.Rounded.Landscape
    "fossil" -> Icons.Rounded.Pets
    "lake" -> Icons.Rounded.Pool
    "forest" -> Icons.Rounded.Forest
    "wine" -> Icons.Rounded.WineBar
    "spa" -> Icons.Rounded.Spa
    "arch" -> Icons.Rounded.ViewColumn
    "road" -> Icons.Rounded.Route
    "museum" -> Icons.Rounded.Museum
    "statue" -> Icons.Rounded.Face
    else -> Icons.Rounded.Star
}

/**
 * Aspetto unico di ogni bollino, ricavato dalla sua posizione nella collezione:
 * tinta (angolo aureo: mai due uguali), forma del bordo, motivo del medaglione e numero di dentelli.
 */
private data class BadgeLook(val c1: Color, val c2: Color, val rim: Int, val pattern: Int, val teeth: Int)

private fun badgeLook(site: UnescoSite): BadgeLook {
    val i = site.order
    val hue = ((i * 137.508f) + if (site.natural) 95f else 12f) % 360f
    val sat = if (site.natural) 0.45f else 0.58f
    return BadgeLook(
        c1 = Color.hsl(hue, sat, 0.56f),
        c2 = Color.hsl((hue + 18f) % 360f, sat + 0.1f, 0.27f),
        rim = i % 4,
        pattern = (i / 4) % 4,
        teeth = 20 + (i * 7) % 17,
    )
}

/** Colori principali del bollino (per sfondi e accenti). */
fun unescoColors(site: UnescoSite): Pair<Color, Color> = badgeLook(site).let { it.c1 to it.c2 }

private val GoldLight = Color(0xFFF7DC8A)
private val Gold = Color(0xFFD9A63E)
private val GoldDark = Color(0xFF9C6F1E)
private val Grey1 = Color(0xFF9AA0A6)
private val Grey2 = Color(0xFF5F6368)

/** Bordo: 0 dentellato, 1 smerlato, 2 stella, 3 sigillo a onde. */
private fun rimPath(look: BadgeLook, c: Offset, r: Float): Path {
    val path = Path()
    val steps = if (look.rim == 0 || look.rim == 2) look.teeth * 2 else 360
    for (k in 0..steps) {
        val t = k.toFloat() / steps
        val a = t * 2f * PI.toFloat() - PI.toFloat() / 2f
        val rr = when (look.rim) {
            0 -> if (k % 2 == 0) r else r * 0.9f
            1 -> r * (0.9f + 0.1f * abs(cos(a * look.teeth / 2f)))
            2 -> if (k % 2 == 0) r else r * 0.84f
            else -> r * (0.94f + 0.06f * sin(a * look.teeth))
        }
        val p = Offset(c.x + cos(a) * rr, c.y + sin(a) * rr)
        if (k == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
    }
    path.close()
    return path
}

/** Motivo inciso nel medaglione: 0 raggi, 1 corona di puntini, 2 cerchi concentrici, 3 rosa dei venti. */
private fun DrawScope.pattern(look: BadgeLook, c: Offset, r: Float) {
    val ink = Color.White.copy(alpha = 0.14f)
    when (look.pattern) {
        0 -> repeat(24) { k ->
            val a = k * (2f * PI.toFloat() / 24f)
            drawLine(ink, Offset(c.x + cos(a) * r * 0.25f, c.y + sin(a) * r * 0.25f), Offset(c.x + cos(a) * r * 0.74f, c.y + sin(a) * r * 0.74f), r * 0.02f)
        }
        1 -> repeat(look.teeth) { k ->
            val a = k * (2f * PI.toFloat() / look.teeth)
            drawCircle(ink.copy(alpha = 0.3f), r * 0.03f, Offset(c.x + cos(a) * r * 0.6f, c.y + sin(a) * r * 0.6f))
        }
        2 -> listOf(0.3f, 0.45f, 0.58f).forEach { f -> drawCircle(ink, r * f, c, style = Stroke(r * 0.018f)) }
        else -> repeat(8) { k ->
            val a = k * (PI.toFloat() / 4f)
            val len = if (k % 2 == 0) 0.74f else 0.5f
            val p = Path().apply {
                moveTo(c.x + cos(a) * r * len, c.y + sin(a) * r * len)
                lineTo(c.x + cos(a + 0.18f) * r * 0.2f, c.y + sin(a + 0.18f) * r * 0.2f)
                lineTo(c.x + cos(a - 0.18f) * r * 0.2f, c.y + sin(a - 0.18f) * r * 0.2f)
                close()
            }
            drawPath(p, ink)
        }
    }
}

/**
 * Bollino da collezione, diverso per ogni sito. Se raccolto è a colori con il bordo dorato;
 * se ci sei passato ([visited]) ma manca la foto è a colori spenti con le impronte;
 * altrimenti grigio e con il lucchetto.
 */
@Composable
fun UnescoBadge(site: UnescoSite, collected: Boolean, size: Dp, modifier: Modifier = Modifier, visited: Boolean = false) {
    val look = badgeLook(site)
    val (c1, c2) = when {
        collected -> look.c1 to look.c2
        visited -> lerp(look.c1, Grey1, 0.45f) to lerp(look.c2, Grey2, 0.45f)
        else -> Grey1 to Grey2
    }
    val rim = if (collected) listOf(GoldLight, Gold, GoldDark) else listOf(Color(0xFFD5D8DC), Color(0xFFAEB3B8), Color(0xFF80868B))
    val showText = size >= 96.dp
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = this.size.minDimension / 2f
            val c = center
            drawPath(rimPath(look, c, r), Brush.linearGradient(rim, start = Offset(0f, 0f), end = Offset(this.size.width, this.size.height)))
            // ombra interna del medaglione
            drawCircle(Color.Black.copy(alpha = 0.18f), r * 0.80f, c.copy(y = c.y + r * 0.02f))
            drawCircle(
                Brush.radialGradient(listOf(c1, c2), center = Offset(c.x - r * 0.3f, c.y - r * 0.35f), radius = r * 1.1f),
                r * 0.78f, c,
            )
            pattern(look, c, r)
            drawCircle(
                Color.White.copy(alpha = 0.55f), r * 0.68f, c,
                style = Stroke(width = r * 0.025f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(r * 0.06f, r * 0.045f))),
            )
            drawCircle(rim[1].copy(alpha = 0.9f), r * 0.78f, c, style = Stroke(width = r * 0.035f))
            // riflesso lucido
            drawCircle(
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.30f), Color.Transparent),
                    startY = c.y - r * 0.78f, endY = c.y,
                ),
                r * 0.74f, c,
            )
        }
        Icon(
            unescoIcon(site.icon), null,
            tint = Color.White.copy(alpha = if (collected) 1f else 0.75f),
            modifier = Modifier.size(size * 0.34f),
        )
        if (showText) {
            Text(
                site.label, color = Color.White.copy(alpha = 0.9f), fontWeight = FontWeight.Black,
                fontSize = (size.value * 0.075f).sp, letterSpacing = (size.value * 0.012f).sp,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = size * 0.2f),
            )
            Text(
                "${site.year}", color = Color.White.copy(alpha = 0.9f), fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.08f).sp,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = size * 0.19f),
            )
        }
        if (!collected) {
            Box(
                Modifier.align(Alignment.BottomEnd).offset(x = (-2).dp, y = (-2).dp).size(size * 0.3f),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.fillMaxSize()) { drawCircle(if (visited) look.c2 else Color(0xFF3C4043)) }
                Icon(
                    if (visited) Icons.Rounded.DirectionsWalk else Icons.Rounded.Lock, null,
                    tint = Color.White, modifier = Modifier.size(size * 0.17f),
                )
            }
        }
    }
}

/** La foto incollata alla collezione, con il bollino sopra come un timbro. */
@Composable
fun StampedPhoto(site: UnescoSite, path: String, modifier: Modifier = Modifier, stampSize: Dp = 88.dp) {
    Box(modifier.fillMaxWidth().aspectRatio(4f / 3f)) {
        AsyncImage(
            model = File(path), contentDescription = site.name, contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)),
        )
        UnescoBadge(
            site, true, stampSize,
            Modifier.align(Alignment.BottomEnd).offset(x = 10.dp, y = 14.dp)
                .graphicsLayer { rotationZ = -14f; alpha = 0.95f },
        )
    }
}
