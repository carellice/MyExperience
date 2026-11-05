package com.myexperience.app.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatAlignLeft
import androidx.compose.material.icons.automirrored.rounded.FormatAlignRight
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FormatAlignCenter
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.myexperience.app.data.AlbumMedia
import com.myexperience.app.data.JournalBlock
import com.myexperience.app.data.Place
import com.myexperience.app.data.PlaceStop
import com.myexperience.app.ui.components.GradientButton
import com.myexperience.app.ui.components.appTextFieldColors
import com.myexperience.app.ui.components.bounceClick
import com.myexperience.app.ui.screens.SelectChip
import com.myexperience.app.ui.theme.BrandBrush
import com.myexperience.app.ui.theme.Sea
import java.io.File

val StickerEmojis = listOf(
    "✈️", "🌍", "🗺️", "🧳", "📸", "🏖️", "🏔️", "🏙️", "🌅", "🌊", "🍝", "🍕", "🍣", "🍷", "☕", "🍦",
    "🎒", "🚆", "🚗", "🚢", "⛺", "🏛️", "🎡", "🌸", "🌴", "🌵", "🍁", "❄️", "☀️", "🌙", "⭐", "✨",
    "❤️", "😍", "🤩", "🥳", "😎", "🥹", "🙏", "👣", "🎶", "📍", "🧭", "🎫", "🏨", "💶", "🗽", "🗼",
)

private val textTypes = setOf("HEADING", "TEXT", "QUOTE", "NOTE", "LIST")

/** Foglio di modifica di un blocco, con anteprima dal vivo nel tema della pagina. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BlockEditorSheet(
    initial: JournalBlock,
    theme: DiaryTheme,
    place: Place,
    media: List<AlbumMedia>,
    onImport: (multiple: Boolean, onIds: (List<Long>) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onSave: (JournalBlock) -> Unit,
    stops: List<PlaceStop> = emptyList(),
) {
    var draft by remember(initial.id, initial.type) { mutableStateOf(initial) }
    val mediaById = remember(media) { media.associateBy { it.id } }
    val info = BlockTypes.firstOrNull { it.type == draft.type }
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("${info?.emoji ?: ""}  ${info?.label ?: "Elemento"}", style = MaterialTheme.typography.titleLarge)

            // anteprima
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .diaryPage(theme)
                    .heightIn(min = 90.dp, max = 340.dp)
                    .padding(18.dp),
                contentAlignment = Alignment.Center,
            ) {
                JournalBlockView(draft, theme, place, mediaById, onOpenMedia = {}, stops = stops)
            }

            // testo
            when (draft.type) {
                in textTypes, "STAMP" -> OutlinedTextField(
                    value = draft.text,
                    onValueChange = { draft = draft.copy(text = it) },
                    label = {
                        Text(
                            when (draft.type) {
                                "LIST" -> "Una voce per riga"
                                "STAMP" -> "Testo del timbro (vuoto = luogo automatico)"
                                else -> "Testo"
                            }
                        )
                    },
                    minLines = if (draft.type == "HEADING" || draft.type == "STAMP") 1 else 3,
                    shape = RoundedCornerShape(16.dp),
                    colors = appTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                "PHOTO", "GALLERY" -> OutlinedTextField(
                    value = draft.text,
                    onValueChange = { draft = draft.copy(text = it) },
                    label = { Text("Didascalia") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = appTextFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                "STICKER" -> {
                    OutlinedTextField(
                        value = draft.text,
                        onValueChange = { draft = draft.copy(text = it) },
                        label = { Text("I tuoi sticker") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = appTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        StickerEmojis.forEach { e ->
                            Box(
                                Modifier.size(42.dp).clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .bounceClick { draft = draft.copy(text = (draft.text + " " + e).trim()) },
                                contentAlignment = Alignment.Center,
                            ) { Text(e, fontSize = 22.sp) }
                        }
                    }
                    TextButton(onClick = { draft = draft.copy(text = "") }) { Text("Svuota") }
                }
            }

            // foto
            if (draft.type == "PHOTO" || draft.type == "GALLERY") {
                val multiple = draft.type == "GALLERY"
                Label(if (multiple) "Scegli le foto del collage (tocca in ordine)" else "Scegli una foto o un video")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
                            .bounceClick {
                                onImport(multiple) { ids ->
                                    val cur = draft.mediaIdList
                                    val next = if (multiple) (cur + ids).distinct().take(9) else ids.take(1)
                                    draft = draft.copy(mediaIds = next.joinToString(","))
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.AddPhotoAlternate, null, tint = MaterialTheme.colorScheme.primary)
                            Text("Dal telefono", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    media.forEach { m ->
                        val selected = draft.mediaIdList
                        val pos = selected.indexOf(m.id)
                        Box(
                            Modifier
                                .size(76.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .then(if (pos >= 0) Modifier.border(3.dp, Sea, RoundedCornerShape(14.dp)) else Modifier)
                                .bounceClick {
                                    val next = if (multiple) {
                                        if (pos >= 0) selected - m.id else (selected + m.id).take(9)
                                    } else listOf(m.id)
                                    draft = draft.copy(mediaIds = next.joinToString(","))
                                },
                        ) {
                            AsyncImage(File(m.path), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            if (m.isVideo) Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.align(Alignment.Center))
                            if (pos >= 0) {
                                Box(
                                    Modifier.align(Alignment.TopEnd).padding(4.dp).size(22.dp).clip(CircleShape).background(BrandBrush),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (multiple) Text("${pos + 1}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    else Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
                if (!multiple) {
                    Label("Stile")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SelectChip("📷 Polaroid", draft.style != "full") { draft = draft.copy(style = "polaroid") }
                        SelectChip("🖼️ Grande", draft.style == "full") { draft = draft.copy(style = "full") }
                    }
                }
            }

            // stile testo
            if (draft.type in textTypes || draft.type == "PHOTO" && draft.style == "full") {
                Label("Carattere")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Fonts.forEach { (key, label) ->
                        SelectChip(label, draft.font == key) { draft = draft.copy(font = key) }
                    }
                }
            }
            if (draft.type in textTypes || draft.type == "STICKER") {
                Label("Dimensione")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("S", "M", "L", "XL").forEachIndexed { i, s ->
                        SelectChip(s, draft.size == i) { draft = draft.copy(size = i) }
                    }
                }
            }
            if (draft.type in textTypes || draft.type == "STICKER" || draft.type == "STAMP") {
                Label("Allineamento")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AlignButton(Icons.AutoMirrored.Rounded.FormatAlignLeft, draft.align == "start") { draft = draft.copy(align = "start") }
                    AlignButton(Icons.Rounded.FormatAlignCenter, draft.align == "center") { draft = draft.copy(align = "center") }
                    AlignButton(Icons.AutoMirrored.Rounded.FormatAlignRight, draft.align == "end") { draft = draft.copy(align = "end") }
                }
            }
            if (draft.type in textTypes) {
                Label("Colore")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InkPalette.forEachIndexed { i, c ->
                        val shown = if (c == Color.Unspecified) theme.ink else c
                        Box(
                            Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(shown)
                                .then(
                                    if (draft.color == i) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    else Modifier.border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                )
                                .bounceClick { draft = draft.copy(color = i) },
                        )
                    }
                }
            }
            if (draft.type == "NOTE") {
                Label("Colore del foglietto")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NoteColors.forEach { (key, c) ->
                        Box(
                            Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(c)
                                .then(if ((draft.style.ifBlank { "yellow" }) == key) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp)) else Modifier)
                                .bounceClick { draft = draft.copy(style = key) },
                        )
                    }
                }
            }
            if (draft.type == "DIVIDER") {
                Label("Stile")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SelectChip("✈ Aereo", draft.style.isBlank() || draft.style == "plane") { draft = draft.copy(style = "plane") }
                    SelectChip("✦ Stelle", draft.style == "stars") { draft = draft.copy(style = "stars") }
                    SelectChip("– – Linea", draft.style == "line") { draft = draft.copy(style = "line") }
                }
            }
            if (draft.type == "ROUTE") {
                Label("Linea del percorso")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SelectChip("〰 Continua", draft.style != "dashed") { draft = draft.copy(style = "") }
                    SelectChip("┄ Tratteggiata", draft.style == "dashed") { draft = draft.copy(style = "dashed") }
                }
                Text(
                    "Le tappe si modificano da ⋮ › Dati dell'esperienza.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDismiss) { Text("Annulla") }
                GradientButton("Salva", Modifier.weight(1f), icon = Icons.Rounded.Check) { onSave(draft) }
            }
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun AlignButton(icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) BrandBrush else androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.surfaceVariant))
            .bounceClick(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurface) }
}

/** Tavolozza degli elementi inseribili nella pagina. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BlockPaletteSheet(onDismiss: () -> Unit, onPick: (String) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text("Aggiungi alla pagina", style = MaterialTheme.typography.titleLarge)
            Text(
                "Componi il tuo diario come vuoi: ogni elemento si può modificare e spostare.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            BlockTypes.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { bt ->
                        Row(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .bounceClick { onPick(bt.type) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface),
                                contentAlignment = Alignment.Center,
                            ) { Text(bt.emoji, fontSize = 20.sp) }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(bt.label, style = MaterialTheme.typography.titleSmall)
                                Text(bt.hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/** Scelta del tema grafico della pagina. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemePickerSheet(current: String, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text("Tema della pagina", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(14.dp))
            DiaryThemes.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { t ->
                        Column(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .diaryPage(t)
                                .then(if (t.id == current) Modifier.border(3.dp, Sea, RoundedCornerShape(20.dp)) else Modifier)
                                .bounceClick { onPick(t.id) }
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(t.emoji, fontSize = 24.sp)
                            Text("Aa", color = t.ink, fontFamily = t.font, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                            Box(Modifier.size(width = 30.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(t.accent))
                            Spacer(Modifier.height(6.dp))
                            Text(t.name, color = t.ink, fontSize = 13.sp)
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}
