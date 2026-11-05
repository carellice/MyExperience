package com.myexperience.app.ui.journal

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.composed
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.myexperience.app.data.AlbumMedia
import com.myexperience.app.data.JournalBlock
import com.myexperience.app.data.Place
import com.myexperience.app.ui.components.countryFlag
import com.myexperience.app.ui.components.formatRange
import java.io.File

// ------------------------------------------------------------------ temi

data class DiaryTheme(
    val id: String,
    val name: String,
    val emoji: String,
    val page: Color,
    val pageAlt: Color,
    val ink: Color,
    val soft: Color,
    val accent: Color,
    val font: FontFamily,
    val ruled: Boolean,
    val dark: Boolean,
)

val DiaryThemes = listOf(
    DiaryTheme("paper", "Carta", "📜", Color(0xFFF7EFDF), Color(0xFFEFE3C9), Color(0xFF3B2F2A), Color(0xFF7A6A5E), Color(0xFFC0583A), FontFamily.Serif, true, false),
    DiaryTheme("night", "Notte", "🌙", Color(0xFF111633), Color(0xFF1B2050), Color(0xFFEDEBFF), Color(0xFFA9A8D8), Color(0xFFFFB86B), FontFamily.SansSerif, false, true),
    DiaryTheme("ocean", "Oceano", "🌊", Color(0xFFE5F6FA), Color(0xFFCDEDF4), Color(0xFF0D3B4A), Color(0xFF4E7B88), Color(0xFF0A9396), FontFamily.SansSerif, false, false),
    DiaryTheme("sunset", "Tramonto", "🌅", Color(0xFFFFEDE4), Color(0xFFFFD6DC), Color(0xFF5A2A3A), Color(0xFF8E5A68), Color(0xFFE85D75), FontFamily.Serif, false, false),
    DiaryTheme("kraft", "Kraft", "📦", Color(0xFFD4B48C), Color(0xFFC6A276), Color(0xFF2E2118), Color(0xFF5A4532), Color(0xFF7A2E1A), FontFamily.Serif, true, false),
    DiaryTheme("forest", "Foresta", "🌲", Color(0xFFE9F1E4), Color(0xFFD6E5CC), Color(0xFF1F3A26), Color(0xFF55705C), Color(0xFF3A7D44), FontFamily.Serif, false, false),
    DiaryTheme("minimal", "Minimal", "⚪", Color(0xFFFFFFFF), Color(0xFFF3F3F6), Color(0xFF15142A), Color(0xFF6A6884), Color(0xFFFF3D7F), FontFamily.SansSerif, false, false),
)

fun themeById(id: String) = DiaryThemes.firstOrNull { it.id == id } ?: DiaryThemes[0]

val InkPalette = listOf(
    Color.Unspecified, // 0 = inchiostro del tema
    Color(0xFFC0392B), Color(0xFFE85D75), Color(0xFF8E44AD), Color(0xFF2E86DE),
    Color(0xFF0A9396), Color(0xFF27AE60), Color(0xFFE67E22), Color(0xFF7F8C8D),
)

val NoteColors = mapOf(
    "yellow" to Color(0xFFFFF1A8),
    "pink" to Color(0xFFFFD1DC),
    "blue" to Color(0xFFCDE7FF),
    "green" to Color(0xFFD5F5C8),
    "orange" to Color(0xFFFFDDB5),
)

val Fonts = listOf("default" to "Tema", "sans" to "Moderno", "serif" to "Classico", "cursive" to "A mano", "mono" to "Macchina")

fun fontFor(key: String, theme: DiaryTheme): FontFamily = when (key) {
    "sans" -> FontFamily.SansSerif
    "serif" -> FontFamily.Serif
    "cursive" -> FontFamily.Cursive
    "mono" -> FontFamily.Monospace
    else -> theme.font
}

fun inkFor(block: JournalBlock, theme: DiaryTheme): Color =
    InkPalette.getOrNull(block.color)?.takeIf { it != Color.Unspecified } ?: theme.ink

fun alignFor(a: String) = when (a) {
    "center" -> TextAlign.Center
    "end" -> TextAlign.End
    else -> TextAlign.Start
}

private fun sizeFor(type: String, size: Int): Float {
    val s = size.coerceIn(0, 3)
    return when (type) {
        "HEADING" -> listOf(20f, 26f, 32f, 40f)[s]
        "QUOTE" -> listOf(15f, 18f, 21f, 25f)[s]
        "NOTE" -> listOf(15f, 18f, 21f, 25f)[s]
        "STICKER" -> listOf(30f, 42f, 56f, 72f)[s]
        else -> listOf(14f, 16f, 19f, 23f)[s]
    }
}

data class BlockType(val type: String, val emoji: String, val label: String, val hint: String)

val BlockTypes = listOf(
    BlockType("HEADING", "🅰️", "Titolo", "Un titolo grande"),
    BlockType("TEXT", "📝", "Testo", "Racconta liberamente"),
    BlockType("QUOTE", "❝", "Citazione", "Una frase da ricordare"),
    BlockType("NOTE", "🗒️", "Nota adesiva", "Post-it scritto a mano"),
    BlockType("LIST", "✦", "Elenco", "Momenti, tappe, cibi…"),
    BlockType("PHOTO", "🖼️", "Foto / video", "Polaroid o foto grande"),
    BlockType("GALLERY", "🧩", "Collage", "Più foto insieme"),
    BlockType("STICKER", "😍", "Sticker", "Emoji decorative"),
    BlockType("STAMP", "📮", "Timbro", "Luogo e data"),
    BlockType("DIVIDER", "✂️", "Separatore", "Dividi le sezioni"),
)

// ------------------------------------------------------------------ pagina

/** Sfondo della pagina diario (eventualmente a righe, come un quaderno). */
fun Modifier.diaryPage(theme: DiaryTheme): Modifier = this
    .background(theme.page)
    .drawBehind {
        if (theme.ruled) {
            val step = 30.dp.toPx()
            var y = step
            while (y < size.height) {
                drawLine(theme.soft.copy(alpha = 0.12f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                y += step
            }
            drawLine(theme.accent.copy(alpha = 0.25f), Offset(28.dp.toPx(), 0f), Offset(28.dp.toPx(), size.height), 1.dp.toPx())
        }
    }

// ------------------------------------------------------------------ rendering blocchi

@Composable
fun JournalBlockView(
    block: JournalBlock,
    theme: DiaryTheme,
    place: Place,
    mediaById: Map<Long, AlbumMedia>,
    onOpenMedia: (AlbumMedia) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = inkFor(block, theme)
    val font = fontFor(block.font, theme)
    val align = alignFor(block.align)
    val fs = sizeFor(block.type, block.size)
    val tilt = if (block.id % 2L == 0L) -2.2f else 1.8f

    Box(modifier.fillMaxWidth()) {
        when (block.type) {
            "HEADING" -> Text(
                block.text.ifBlank { "Titolo" },
                color = ink, fontFamily = font, fontSize = fs.sp, lineHeight = (fs * 1.15f).sp,
                fontWeight = if (block.font == "cursive") FontWeight.Normal else FontWeight.Bold,
                textAlign = align, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            )

            "TEXT" -> Text(
                block.text.ifBlank { "Scrivi qualcosa…" },
                color = ink, fontFamily = font, fontSize = fs.sp, lineHeight = (fs * 1.55f).sp,
                textAlign = align, modifier = Modifier.fillMaxWidth(),
            )

            "QUOTE" -> Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                Box(Modifier.width(4.dp).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(theme.accent))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("“", color = theme.accent, fontSize = 48.sp, fontFamily = FontFamily.Serif, lineHeight = 40.sp)
                    Text(
                        block.text.ifBlank { "Una frase da ricordare" },
                        color = ink, fontFamily = font, fontSize = fs.sp, lineHeight = (fs * 1.45f).sp,
                        fontStyle = FontStyle.Italic, textAlign = align, modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            "NOTE" -> {
                val bg = NoteColors[block.style] ?: NoteColors.getValue("yellow")
                Box(Modifier.fillMaxWidth(), contentAlignment = when (block.align) {
                    "center" -> Alignment.Center
                    "end" -> Alignment.CenterEnd
                    else -> Alignment.CenterStart
                }) {
                    Box(
                        Modifier
                            .fillMaxWidth(0.82f)
                            .rotate(tilt)
                            .shadow(6.dp, RoundedCornerShape(4.dp))
                            .background(bg, RoundedCornerShape(4.dp))
                            .padding(start = 18.dp, end = 18.dp, top = 26.dp, bottom = 20.dp),
                    ) {
                        Text(
                            block.text.ifBlank { "Una nota…" },
                            color = InkPalette.getOrNull(block.color)?.takeIf { it != Color.Unspecified } ?: Color(0xFF3B2F2A),
                            fontFamily = if (block.font == "default") FontFamily.Cursive else font,
                            fontSize = fs.sp, lineHeight = (fs * 1.4f).sp, textAlign = align,
                        )
                    }
                    // nastro adesivo
                    Box(
                        Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (-8).dp)
                            .rotate(-4f)
                            .size(width = 80.dp, height = 22.dp)
                            .background(Color.White.copy(alpha = 0.55f), RoundedCornerShape(2.dp)),
                    )
                }
            }

            "LIST" -> Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                block.text.lines().map { it.trim() }.filter { it.isNotEmpty() }.ifEmpty { listOf("Primo momento", "Secondo momento") }
                    .forEach { line ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text("✦", color = theme.accent, fontSize = fs.sp, modifier = Modifier.padding(end = 10.dp))
                            Text(line, color = ink, fontFamily = font, fontSize = fs.sp, lineHeight = (fs * 1.4f).sp)
                        }
                    }
            }

            "PHOTO" -> {
                val m = block.mediaIdList.firstNotNullOfOrNull { mediaById[it] }
                if (block.style == "full") {
                    Column(Modifier.fillMaxWidth()) {
                        MediaThumb(m, Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(RoundedCornerShape(18.dp)), onOpenMedia)
                        if (block.text.isNotBlank()) {
                            Text(
                                block.text, color = theme.soft, fontFamily = font, fontStyle = FontStyle.Italic,
                                fontSize = 13.sp, textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                            )
                        }
                    }
                } else {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(
                            Modifier
                                .fillMaxWidth(0.78f)
                                .rotate(tilt)
                                .shadow(10.dp, RoundedCornerShape(3.dp))
                                .background(Color(0xFFFDFDFB), RoundedCornerShape(3.dp))
                                .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
                        ) {
                            MediaThumb(m, Modifier.fillMaxWidth().aspectRatio(1f), onOpenMedia)
                            Text(
                                block.text.ifBlank { " " }, color = Color(0xFF3B2F2A), fontFamily = FontFamily.Cursive,
                                fontSize = 20.sp, textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
                            )
                        }
                    }
                }
            }

            "GALLERY" -> {
                val items = block.mediaIdList.mapNotNull { mediaById[it] }
                Column(Modifier.fillMaxWidth()) {
                    Collage(items, onOpenMedia)
                    if (block.text.isNotBlank()) {
                        Text(
                            block.text, color = theme.soft, fontFamily = font, fontStyle = FontStyle.Italic,
                            fontSize = 13.sp, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        )
                    }
                }
            }

            "STICKER" -> {
                val inf = rememberInfiniteTransition(label = "sticker")
                val y by inf.animateFloat(-4f, 4f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "y")
                Text(
                    block.text.ifBlank { "✨" }, fontSize = fs.sp, textAlign = align, lineHeight = (fs * 1.2f).sp,
                    modifier = Modifier.fillMaxWidth().graphicsLayer { translationY = y },
                )
            }

            "STAMP" -> {
                val label = block.text.ifBlank {
                    "${countryFlag(place.country)} ${place.locationName.uppercase()}" +
                        (if (place.country.isNotBlank()) " · ${place.country.uppercase()}" else "")
                }
                Box(Modifier.fillMaxWidth(), contentAlignment = when (block.align) {
                    "center" -> Alignment.Center
                    "start" -> Alignment.CenterStart
                    else -> Alignment.CenterEnd
                }) {
                    Column(
                        Modifier
                            .rotate(-6f)
                            .graphicsLayer { alpha = 0.88f }
                            .border(2.5.dp, theme.accent, RoundedCornerShape(10.dp))
                            .padding(3.dp)
                            .border(1.dp, theme.accent.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(label, color = theme.accent, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            formatRange(place.startDate, place.endDate).uppercase(),
                            color = theme.accent, fontFamily = FontFamily.Monospace, fontSize = 11.sp,
                        )
                    }
                }
            }

            "DIVIDER" -> when (block.style) {
                "line" -> Box(
                    Modifier.fillMaxWidth().height(20.dp).drawBehind {
                        drawLine(
                            theme.soft.copy(alpha = 0.6f), Offset(0f, size.height / 2), Offset(size.width, size.height / 2),
                            2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)),
                        )
                    },
                )
                "stars" -> Text("✦   ✦   ✦", color = theme.accent, fontSize = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                else -> Text("· · · · · ✈ · · · · ·", color = theme.soft, fontSize = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }

            else -> Text(block.text, color = ink)
        }
    }
}

@Composable
private fun MediaThumb(m: AlbumMedia?, modifier: Modifier, onOpen: (AlbumMedia) -> Unit) {
    Box(
        modifier
            .background(Color(0x22000000))
            .then(if (m != null) Modifier.clickableNoRipple { onOpen(m) } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (m != null) {
            AsyncImage(File(m.path), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            if (m.isVideo) {
                Box(
                    Modifier.size(46.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(30.dp)) }
            }
        } else {
            Text("🖼️  Scegli una foto", color = Color.Gray, fontSize = 13.sp)
        }
    }
}

@Composable
private fun Collage(items: List<AlbumMedia>, onOpen: (AlbumMedia) -> Unit) {
    val gap = 6.dp
    val r = RoundedCornerShape(14.dp)
    when {
        items.isEmpty() -> MediaThumb(null, Modifier.fillMaxWidth().height(160.dp).clip(r), onOpen)
        items.size == 1 -> MediaThumb(items[0], Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(r), onOpen)
        items.size == 2 -> Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            items.forEach { MediaThumb(it, Modifier.weight(1f).aspectRatio(0.8f).clip(r), onOpen) }
        }
        items.size == 3 -> Row(Modifier.fillMaxWidth().height(260.dp), horizontalArrangement = Arrangement.spacedBy(gap)) {
            MediaThumb(items[0], Modifier.weight(1.3f).fillMaxHeight().clip(r), onOpen)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(gap)) {
                MediaThumb(items[1], Modifier.weight(1f).fillMaxWidth().clip(r), onOpen)
                MediaThumb(items[2], Modifier.weight(1f).fillMaxWidth().clip(r), onOpen)
            }
        }
        else -> Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            items.take(4).chunked(2).forEachIndexed { row, pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    pair.forEachIndexed { col, m ->
                        Box(Modifier.weight(1f).aspectRatio(1f).clip(r)) {
                            MediaThumb(m, Modifier.fillMaxSize(), onOpen)
                            val extra = items.size - 4
                            if (row == 1 && col == 1 && extra > 0) {
                                Box(
                                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)).clickableNoRipple { onOpen(m) },
                                    contentAlignment = Alignment.Center,
                                ) { Text("+$extra", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold) }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = composed {
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick,
    )
}
