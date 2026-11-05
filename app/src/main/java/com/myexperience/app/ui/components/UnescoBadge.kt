package com.myexperience.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myexperience.app.data.UnescoSite
import kotlin.math.PI
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
    else -> Icons.Rounded.Star
}

private val CulturalPalettes = listOf(
    Color(0xFFE07A5F) to Color(0xFF9C3D2E),
    Color(0xFF3D7EA6) to Color(0xFF1F4466),
    Color(0xFF8E6BBF) to Color(0xFF4B3275),
    Color(0xFFD9822B) to Color(0xFF8A4B12),
    Color(0xFFC0566E) to Color(0xFF6E2438),
    Color(0xFF2A8197) to Color(0xFF134552),
    Color(0xFFB07D48) to Color(0xFF5E3E1E),
    Color(0xFF5B6CC9) to Color(0xFF2C3577),
)
private val NaturalPalettes = listOf(
    Color(0xFF5B8A4E) to Color(0xFF2B4A24),
    Color(0xFF3A9D8F) to Color(0xFF175048),
    Color(0xFF7FA34A) to Color(0xFF3F5A1C),
)

fun unescoColors(site: UnescoSite): Pair<Color, Color> {
    val list = if (site.natural) NaturalPalettes else CulturalPalettes
    return list[Math.floorMod(site.id.hashCode(), list.size)]
}

private val GoldLight = Color(0xFFF7DC8A)
private val Gold = Color(0xFFD9A63E)
private val GoldDark = Color(0xFF9C6F1E)

/**
 * Bollino da collezione: bordo dentellato dorato, medaglione colorato con l'icona del sito,
 * anello tratteggiato e anno di iscrizione. Se non raccolto è grigio e con il lucchetto.
 */
@Composable
fun UnescoBadge(site: UnescoSite, collected: Boolean, size: Dp, modifier: Modifier = Modifier) {
    val (c1, c2) = if (collected) unescoColors(site) else Color(0xFF9AA0A6) to Color(0xFF5F6368)
    val rim = if (collected) listOf(GoldLight, Gold, GoldDark) else listOf(Color(0xFFD5D8DC), Color(0xFFAEB3B8), Color(0xFF80868B))
    val showText = size >= 96.dp
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = this.size.minDimension / 2f
            val c = center
            // bordo dentellato
            val teeth = 28
            val path = Path()
            for (i in 0..teeth * 2) {
                val a = (i.toFloat() / (teeth * 2)) * 2f * PI.toFloat() - PI.toFloat() / 2f
                val rr = if (i % 2 == 0) r else r * 0.9f
                val p = Offset(c.x + cos(a) * rr, c.y + sin(a) * rr)
                if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            path.close()
            drawPath(path, Brush.linearGradient(rim, start = Offset(0f, 0f), end = Offset(this.size.width, this.size.height)))
            // ombra interna del medaglione
            drawCircle(Color.Black.copy(alpha = 0.18f), r * 0.80f, c.copy(y = c.y + r * 0.02f))
            // medaglione
            drawCircle(
                Brush.radialGradient(listOf(c1, c2), center = Offset(c.x - r * 0.3f, c.y - r * 0.35f), radius = r * 1.1f),
                r * 0.78f, c,
            )
            // anello tratteggiato
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
                "UNESCO", color = Color.White.copy(alpha = 0.9f), fontWeight = FontWeight.Black,
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
                Canvas(Modifier.fillMaxSize()) { drawCircle(Color(0xFF3C4043)) }
                Icon(Icons.Rounded.Lock, null, tint = Color.White, modifier = Modifier.size(size * 0.17f))
            }
        }
    }
}
