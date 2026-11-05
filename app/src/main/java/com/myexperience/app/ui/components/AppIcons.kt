package com.myexperience.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.util.Locale

// Set di icone dell'app: sostituisce le emoji con icone vettoriali coerenti (stile Rounded).

/** Icona di una categoria di esperienza. */
fun categoryIcon(c: String): ImageVector = when (c) {
    "Città" -> Icons.Rounded.LocationCity
    "Mare" -> Icons.Rounded.BeachAccess
    "Montagna" -> Icons.Rounded.Landscape
    "Natura" -> Icons.Rounded.Forest
    "Cultura" -> Icons.Rounded.Museum
    "Food" -> Icons.Rounded.Restaurant
    "Avventura" -> Icons.Rounded.Hiking
    "Relax" -> Icons.Rounded.Spa
    "On the road" -> Icons.Rounded.AirportShuttle
    else -> Icons.Rounded.Place
}

/** Umore di un'esperienza. La chiave salvata nel database resta quella storica. */
data class Mood(val key: String, val label: String, val icon: ImageVector)

val Moods = listOf(
    Mood("😍", "Innamorato", Icons.Rounded.Favorite),
    Mood("🤩", "Entusiasta", Icons.Rounded.AutoAwesome),
    Mood("🥳", "In festa", Icons.Rounded.Celebration),
    Mood("😌", "Sereno", Icons.Rounded.SelfImprovement),
    Mood("🤯", "Sbalordito", Icons.Rounded.Bolt),
    Mood("🥹", "Commosso", Icons.Rounded.WaterDrop),
    Mood("😎", "Al sole", Icons.Rounded.WbSunny),
    Mood("😴", "Riposato", Icons.Rounded.Bedtime),
)

fun moodFor(key: String): Mood = Moods.firstOrNull { it.key == key } ?: Moods[0]

/** Mezzi di trasporto (i viaggi vecchi hanno salvato "✈️ Aereo": si legge solo l'etichetta). */
val Transports = listOf("Aereo", "Treno", "Auto", "Nave", "Bus", "Moto")

fun transportLabel(stored: String): String = stored.trim().let { if (' ' in it) it.substringAfter(' ') else it }

fun transportIcon(stored: String): ImageVector = when (transportLabel(stored)) {
    "Aereo" -> Icons.Rounded.Flight
    "Treno" -> Icons.Rounded.Train
    "Auto" -> Icons.Rounded.DirectionsCar
    "Nave" -> Icons.Rounded.DirectionsBoat
    "Bus" -> Icons.Rounded.DirectionsBus
    "Moto" -> Icons.Rounded.TwoWheeler
    else -> Icons.Rounded.Luggage
}

/** Icone decorative per il blocco "Icone" del diario: chiave salvata -> icona. */
val StickerIcons: List<Pair<String, ImageVector>> = listOf(
    "plane" to Icons.Rounded.Flight,
    "globe" to Icons.Rounded.Public,
    "map" to Icons.Rounded.Map,
    "luggage" to Icons.Rounded.Luggage,
    "camera" to Icons.Rounded.PhotoCamera,
    "beach" to Icons.Rounded.BeachAccess,
    "mountain" to Icons.Rounded.Landscape,
    "city" to Icons.Rounded.LocationCity,
    "sunset" to Icons.Rounded.WbTwilight,
    "waves" to Icons.Rounded.Waves,
    "food" to Icons.Rounded.Restaurant,
    "pizza" to Icons.Rounded.LocalPizza,
    "wine" to Icons.Rounded.WineBar,
    "coffee" to Icons.Rounded.LocalCafe,
    "icecream" to Icons.Rounded.Icecream,
    "backpack" to Icons.Rounded.Backpack,
    "train" to Icons.Rounded.Train,
    "car" to Icons.Rounded.DirectionsCar,
    "boat" to Icons.Rounded.DirectionsBoat,
    "van" to Icons.Rounded.AirportShuttle,
    "tent" to Icons.Rounded.Cabin,
    "museum" to Icons.Rounded.Museum,
    "fun" to Icons.Rounded.Attractions,
    "flower" to Icons.Rounded.LocalFlorist,
    "forest" to Icons.Rounded.Forest,
    "hike" to Icons.Rounded.Hiking,
    "snow" to Icons.Rounded.AcUnit,
    "sun" to Icons.Rounded.WbSunny,
    "moon" to Icons.Rounded.Bedtime,
    "star" to Icons.Rounded.Star,
    "sparkle" to Icons.Rounded.AutoAwesome,
    "heart" to Icons.Rounded.Favorite,
    "party" to Icons.Rounded.Celebration,
    "music" to Icons.Rounded.MusicNote,
    "pin" to Icons.Rounded.Place,
    "compass" to Icons.Rounded.Explore,
    "ticket" to Icons.Rounded.ConfirmationNumber,
    "hotel" to Icons.Rounded.Hotel,
    "euro" to Icons.Rounded.Euro,
    "spa" to Icons.Rounded.SelfImprovement,
    "bolt" to Icons.Rounded.Bolt,
    "drop" to Icons.Rounded.WaterDrop,
)

private val stickerByKey = StickerIcons.toMap()

fun stickerIcon(key: String): ImageVector? = stickerByKey[key]

/** Conversione dei vecchi sticker emoji nelle nuove chiavi (quelli senza corrispondenza si perdono). */
val LegacyStickerKeys = mapOf(
    "✈️" to "plane", "✈" to "plane", "🌍" to "globe", "🗺️" to "map", "🧳" to "luggage", "📸" to "camera",
    "🏖️" to "beach", "🏔️" to "mountain", "🏙️" to "city", "🌅" to "sunset", "🌊" to "waves", "🍝" to "food",
    "🍕" to "pizza", "🍣" to "food", "🍷" to "wine", "☕" to "coffee", "🍦" to "icecream", "🎒" to "backpack",
    "🚆" to "train", "🚗" to "car", "🚢" to "boat", "⛺" to "tent", "🏛️" to "museum", "🎡" to "fun",
    "🌸" to "flower", "🌴" to "beach", "🌵" to "forest", "🍁" to "forest", "❄️" to "snow", "☀️" to "sun",
    "🌙" to "moon", "⭐" to "star", "✨" to "sparkle", "❤️" to "heart", "😍" to "heart", "🤩" to "sparkle",
    "🥳" to "party", "😎" to "sun", "🥹" to "drop", "😌" to "spa", "🤯" to "bolt", "😴" to "moon",
    "🙏" to "heart", "👣" to "hike", "🎶" to "music", "📍" to "pin", "🧭" to "compass", "🎫" to "ticket",
    "🏨" to "hotel", "💶" to "euro", "🗽" to "city", "🗼" to "city",
)

/** Chiave sticker coerente con l'umore scelto. */
fun moodSticker(moodKey: String): String = LegacyStickerKeys[moodKey] ?: "heart"

/** Sigla ISO del paese dal nome (in qualsiasi lingua comune), vuota se non riconosciuto. */
private val codeCache = mutableMapOf<String, String>()

fun countryCode(country: String): String = codeCache.getOrPut(country.lowercase()) {
    val name = country.trim().lowercase()
    if (name.isEmpty()) return@getOrPut ""
    val langs = listOf(Locale.getDefault(), Locale.ITALIAN, Locale.ENGLISH, Locale("es"), Locale("fr"), Locale("de"), Locale("pt"))
    Locale.getISOCountries().firstOrNull { iso ->
        val loc = Locale("", iso)
        langs.any { loc.getDisplayCountry(it).lowercase() == name } || iso.lowercase() == name
    } ?: ""
}

/** Bandiera emoji dal nome del paese, vuota se non riconosciuto. */
fun countryFlag(country: String): String =
    countryCode(country).uppercase().map { ch -> String(Character.toChars(0x1F1E6 + (ch - 'A'))) }.joinToString("")

/** Bandiera seguita da uno spazio, o niente se il paese non è riconosciuto. */
fun flagPrefix(country: String): String = countryFlag(country).let { if (it.isEmpty()) "" else "$it " }

/** Icona che fluttua piano su e giù (sostituisce le emoji animate). */
@Composable
fun FloatingIcon(icon: ImageVector, size: Dp, tint: Color, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "float")
    val y by t.animateFloat(
        -6f, 6f,
        infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "y",
    )
    Icon(icon, null, tint = tint, modifier = modifier.size(size).graphicsLayer { translationY = y })
}

/** Icona in un riquadro arrotondato colorato. */
@Composable
fun IconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    container: Color,
    tint: Color,
    corner: Dp = 14.dp,
) {
    Box(
        modifier.size(size).clip(RoundedCornerShape(corner)).background(container),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.55f)) }
}
