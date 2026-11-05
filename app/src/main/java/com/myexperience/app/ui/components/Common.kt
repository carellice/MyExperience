package com.myexperience.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.myexperience.app.ui.theme.BrandBrush
import com.myexperience.app.ui.theme.BrandColors
import com.myexperience.app.ui.theme.Sand
import com.myexperience.app.ui.theme.gradientFor
import kotlinx.coroutines.delay
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

// ---------------- Costanti di dominio ----------------

val Categories = listOf("Città", "Mare", "Montagna", "Natura", "Cultura", "Food", "Avventura", "Relax", "On the road")

// ---------------- Date ----------------

private val dayFmt = DateTimeFormatter.ofPattern("d MMM", Locale.ITALIAN)
private val fullFmt = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ITALIAN)

fun Long.toLocalDateUtc(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
fun todayUtcMillis(): Long = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

fun formatDate(ms: Long): String = ms.toLocalDateUtc().format(fullFmt)

fun formatRange(start: Long?, end: Long?): String {
    if (start == null) return "Date da definire"
    val s = start.toLocalDateUtc()
    val e = (end ?: start).toLocalDateUtc()
    return when {
        s == e -> s.format(fullFmt)
        s.year == e.year -> "${s.format(dayFmt)} – ${e.format(fullFmt)}"
        else -> "${s.format(fullFmt)} – ${e.format(fullFmt)}"
    }
}

fun daysBetween(start: Long, end: Long): Long =
    ChronoUnit.DAYS.between(start.toLocalDateUtc(), end.toLocalDateUtc()) + 1

fun daysUntil(ms: Long): Long = ChronoUnit.DAYS.between(LocalDate.now(), ms.toLocalDateUtc())

fun countdownLabel(start: Long?, end: Long?): String {
    if (start == null) return "Date da definire"
    val d = daysUntil(start)
    val endD = daysUntil(end ?: start)
    return when {
        d > 1 -> "tra $d giorni"
        d == 1L -> "domani!"
        d == 0L -> "si parte oggi!"
        endD >= 0 -> "in corso"
        else -> "concluso"
    }
}

// ---------------- Modifier animati ----------------

/** Comparsa con dissolvenza + salita, sfalsata per indice. */
fun Modifier.appear(index: Int = 0): Modifier = composed {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay((index.coerceAtMost(8) * 70).toLong())
        anim.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
    }
    graphicsLayer {
        alpha = anim.value
        translationY = (1f - anim.value) * 80f
        val s = 0.94f + 0.06f * anim.value
        scaleX = s
        scaleY = s
    }
}

/** Click con effetto "rimbalzo". */
fun Modifier.bounceClick(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.94f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "bounce",
    )
    graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
}

// ---------------- Componenti ----------------

@Composable
fun GradientButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .height(56.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (enabled) BrandBrush else Brush.linearGradient(listOf(Color.Gray, Color.Gray)))
            .bounceClick(enabled, onClick)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
            }
            Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

/** Immagine di copertina o, se manca, gradiente con l'icona della categoria. */
@Composable
fun CoverImage(
    path: String?,
    seed: Long,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.Public,
    iconSize: Dp = 56.dp,
) {
    Box(modifier.background(gradientFor(seed)), contentAlignment = Alignment.Center) {
        if (path != null) {
            AsyncImage(
                model = File(path),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            FloatingIcon(icon, iconSize, Color.White.copy(alpha = 0.92f))
        }
    }
}

@Composable
fun Avatar(path: String?, name: String, size: Dp, modifier: Modifier = Modifier, ring: Boolean = false) {
    val inner = @Composable {
        Box(
            Modifier
                .size(if (ring) size - 6.dp else size)
                .clip(CircleShape)
                .background(BrandBrush),
            contentAlignment = Alignment.Center,
        ) {
            if (path != null) {
                AsyncImage(File(path), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Text(
                    name.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = (size.value * 0.42f).sp,
                )
            }
        }
    }
    if (ring) {
        Box(
            modifier
                .size(size)
                .border(3.dp, MaterialTheme.colorScheme.background, CircleShape)
                .padding(0.dp),
            contentAlignment = Alignment.Center,
        ) { inner() }
    } else {
        Box(modifier) { inner() }
    }
}

/** Anello a gradiente rotante, stile "storie". */
@Composable
fun StoryRing(size: Dp, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val t = rememberInfiniteTransition(label = "ring")
    val angle by t.animateFloat(
        0f, 360f, infiniteRepeatable(tween(4000, easing = LinearEasing)), label = "angle",
    )
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .matchParentSize()
                .rotate(angle)
                .clip(CircleShape)
                .background(Brush.sweepGradient(BrandColors + BrandColors.first())),
        )
        Box(
            Modifier
                .size(size - 6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(size - 12.dp).clip(CircleShape)) { content() }
        }
    }
}

@Composable
fun RatingStars(rating: Int, size: Dp = 18.dp, onChange: ((Int) -> Unit)? = null) {
    Row {
        for (i in 1..5) {
            val active = i <= rating
            val scale by animateFloatAsState(
                if (active) 1f else 0.85f,
                spring(dampingRatio = Spring.DampingRatioHighBouncy),
                label = "star",
            )
            Icon(
                Icons.Rounded.Star,
                contentDescription = null,
                tint = if (active) Sand else MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .size(size)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .then(if (onChange != null) Modifier.clickable { onChange(i) } else Modifier),
            )
        }
    }
}

@Composable
fun AnimatedCounter(value: Int, label: String, modifier: Modifier = Modifier) {
    var target by remember { mutableIntStateOf(0) }
    LaunchedEffect(value) { target = value }
    val shown by animateIntAsState(target, tween(1100, easing = FastOutSlowInEasing), label = "count")
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            shown.toString(),
            style = MaterialTheme.typography.headlineSmall.copy(brush = BrandBrush),
            fontWeight = FontWeight.Black,
        )
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SectionTitle(title: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(width = 4.dp, height = 20.dp).clip(RoundedCornerShape(2.dp)).background(BrandBrush))
        Spacer(Modifier.width(10.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

@Composable
fun InfoChip(
    text: String,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.surfaceVariant,
    content: Color = MaterialTheme.colorScheme.onSurface,
    icon: ImageVector? = null,
) {
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(container)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = content, modifier = Modifier.size(16.dp))
            if (text.isNotEmpty()) Spacer(Modifier.width(6.dp))
        }
        if (text.isNotEmpty()) Text(text, style = MaterialTheme.typography.labelLarge, color = content)
    }
}

@Composable
fun GlassChip(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    InfoChip(text, modifier, container = Color.Black.copy(alpha = 0.35f), content = Color.White, icon = icon)
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: () -> Unit = {},
) {
    Column(
        modifier.fillMaxWidth().padding(32.dp).appear(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val t = rememberInfiniteTransition(label = "empty")
        val pulse by t.animateFloat(
            0.9f, 1.08f, infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "p",
        )
        Box(
            Modifier
                .size(140.dp)
                .graphicsLayer { scaleX = pulse; scaleY = pulse }
                .drawBehind {
                    drawCircle(
                        Brush.radialGradient(
                            listOf(BrandColors[1].copy(alpha = 0.35f), Color.Transparent),
                            center = Offset(size.width / 2, size.height / 2),
                            radius = size.minDimension / 2,
                        )
                    )
                },
            contentAlignment = Alignment.Center,
        ) { FloatingIcon(icon, 64.dp, MaterialTheme.colorScheme.primary) }
        Spacer(Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionText != null) {
            Spacer(Modifier.height(20.dp))
            GradientButton(actionText, onClick = onAction)
        }
    }
}

@Composable
fun CircleIconButton(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    container: Color = Color.Black.copy(alpha = 0.35f),
    onClick: () -> Unit,
) {
    Box(
        modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(container)
            .bounceClick(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = tint) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangeDialog(
    start: Long?,
    end: Long?,
    onDismiss: () -> Unit,
    onConfirm: (Long, Long) -> Unit,
) {
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = start,
        initialSelectedEndDateMillis = end,
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val s = state.selectedStartDateMillis
                    if (s != null) onConfirm(s, state.selectedEndDateMillis ?: s)
                },
                enabled = state.selectedStartDateMillis != null,
            ) { Text("Conferma") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
    ) {
        DateRangePicker(
            state = state,
            modifier = Modifier.weight(1f),
            title = { Text("Seleziona le date", modifier = Modifier.padding(start = 24.dp, top = 16.dp)) },
            showModeToggle = false,
        )
    }
}

@Composable
fun FormCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        content = content,
    )
}

@Composable
fun FieldLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
fun appTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
)
