package com.myexperience.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.myexperience.app.MainViewModel
import com.myexperience.app.data.AlbumMedia
import com.myexperience.app.data.JournalBlock
import com.myexperience.app.ui.components.*
import com.myexperience.app.ui.journal.*
import com.myexperience.app.ui.theme.BrandBrush
import com.myexperience.app.ui.theme.Sea
import java.io.File

private data class EditRequest(val block: JournalBlock, val index: Int)

/** Album di un'esperienza: copertina + pagina di diario liberamente personalizzabile + rullino. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaceDetailScreen(
    vm: MainViewModel,
    placeId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onOpenMedia: (Int) -> Unit,
) {
    val flow = remember(placeId) { vm.place(placeId) }
    val data by flow.collectAsStateWithLifecycle(initialValue = null)
    val blocksFlow = remember(placeId) { vm.blocks(placeId) }
    val blocks by blocksFlow.collectAsStateWithLifecycle(initialValue = null)
    val importing by vm.importing.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    var editMode by rememberSaveable { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var showThemes by remember { mutableStateOf(false) }
    var paletteAt by remember { mutableStateOf<Int?>(null) }
    var editing by remember { mutableStateOf<EditRequest?>(null) }
    var mediaOptions by remember { mutableStateOf<AlbumMedia?>(null) }

    // import: per il rullino o per un blocco in modifica
    var pendingImport by remember { mutableStateOf<((List<Long>) -> Unit)?>(null) }
    val multiPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(50)) { uris ->
        val cb = pendingImport
        pendingImport = null
        if (uris.isNotEmpty()) vm.importMedia(placeId, uris) { ids -> cb?.invoke(ids) }
    }
    val singlePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val cb = pendingImport
        pendingImport = null
        if (uri != null) vm.importMedia(placeId, listOf(uri)) { ids -> cb?.invoke(ids) }
    }
    val request = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
    val importToRoll = {
        pendingImport = null
        multiPicker.launch(request)
    }

    val d = data
    val list = blocks
    if (d == null || list == null) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        return
    }
    val p = d.place
    val route = d.route
    val theme = themeById(p.theme)
    val mediaById = remember(d.media) { d.media.associateBy { it.id } }
    val openMedia: (AlbumMedia) -> Unit = { m ->
        val idx = d.media.indexOfFirst { it.id == m.id }
        if (idx >= 0) onOpenMedia(idx)
    }

    val scrolledPastHero by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 800 }
    }

    Box(Modifier.fillMaxSize().background(theme.pageAlt)) {
        CompositionLocalProvider(LocalContentColor provides theme.ink) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 120.dp)) {
            // ---------- COPERTINA ----------
            item(key = "hero") {
                Box(Modifier.fillMaxWidth().height(440.dp).clipToBounds()) {
                    CoverImage(
                        p.coverPath, p.id,
                        Modifier.fillMaxSize().graphicsLayer {
                            val off = if (listState.firstVisibleItemIndex == 0) listState.firstVisibleItemScrollOffset else 0
                            translationY = off * 0.5f
                            val s = 1f + off / 3000f
                            scaleX = s; scaleY = s
                        },
                        emoji = categoryEmoji(p.category), emojiSize = 110,
                    )
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.verticalGradient(
                                0f to Color.Black.copy(alpha = 0.35f),
                                0.35f to Color.Transparent,
                                0.62f to Color.Transparent,
                                0.92f to theme.page.copy(alpha = 0.85f),
                                1f to theme.page,
                            )
                        ),
                    )
                    Column(Modifier.align(Alignment.BottomStart).padding(horizontal = 22.dp, vertical = 18.dp).appear()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (route.size > 1) {
                                GlassChip("🚐 ${route.size} tappe · ${d.countries.joinToString("") { countryFlag(it) }}")
                            } else {
                                GlassChip("${countryFlag(p.country)} ${p.locationName}")
                            }
                            GlassChip(p.mood)
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            p.title, color = theme.ink, fontFamily = theme.font, fontSize = 34.sp,
                            fontWeight = FontWeight.Bold, lineHeight = 38.sp,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(formatRange(p.startDate, p.endDate), color = theme.soft, style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.width(10.dp))
                            RatingStars(p.rating, 16.dp)
                        }
                    }
                }
            }

            // ---------- TAPPE (viaggio on the road) ----------
            if (route.size > 1) {
                item(key = "route") {
                    Column(Modifier.fillMaxWidth().background(theme.page).padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Box(
                            Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(24.dp)),
                        ) {
                            OsmMap(
                                pins = route.mapIndexed { i, s -> MapPin(i.toLong(), s.latitude, s.longitude, s.name) },
                                modifier = Modifier.fillMaxSize(),
                                drawRoute = true,
                                interactive = false,
                            )
                            GlassChip("🧭 On the road", Modifier.padding(10.dp))
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            route.forEachIndexed { i, s ->
                                if (i > 0) Text("  →  ", color = theme.soft, fontSize = 13.sp)
                                Text(
                                    "${i + 1}. ${s.name}" + if (s.nights > 0) " · 🌙${s.nights}" else "",
                                    color = theme.ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                                    modifier = Modifier.clip(RoundedCornerShape(50))
                                        .background(theme.accent.copy(alpha = 0.12f))
                                        .padding(horizontal = 10.dp, vertical = 5.dp),
                                )
                            }
                        }
                    }
                }
            }

            // ---------- PAGINA DIARIO ----------
            item(key = "page-top") {
                Row(
                    Modifier.fillMaxWidth().diaryPage(theme).padding(start = 40.dp, end = 20.dp, top = 6.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("📖 Diario di viaggio", color = theme.soft, fontFamily = theme.font, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    AnimatedVisibility(editMode) {
                        Text(
                            "${theme.emoji} Tema", color = theme.accent, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                            modifier = Modifier.clip(RoundedCornerShape(50)).border(1.dp, theme.accent, RoundedCornerShape(50))
                                .bounceClick { showThemes = true }.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }

            if (list.isEmpty()) {
                item(key = "empty") {
                    Column(
                        Modifier.fillMaxWidth().diaryPage(theme).padding(horizontal = 32.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        FloatingEmoji("✍️", 48)
                        Spacer(Modifier.height(8.dp))
                        Text("La pagina è ancora bianca", color = theme.ink, fontFamily = theme.font, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "Crea il tuo diario: testi, foto, note, sticker… tutto come piace a te.",
                            color = theme.soft, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                        Spacer(Modifier.height(16.dp))
                        GradientButton("Pagina pronta da personalizzare", icon = Icons.Rounded.AutoAwesome) {
                            vm.seedTemplate(p.id); editMode = true
                        }
                        TextButton(onClick = { editMode = true; paletteAt = 0 }) {
                            Text("Parti da zero", color = theme.accent)
                        }
                    }
                }
            }

            if (editMode && list.isNotEmpty()) {
                item(key = "ins-top") { InsertHere(theme) { paletteAt = 0 } }
            }

            itemsIndexed(list, key = { _, b -> "b${b.id}" }) { i, block ->
                Column(
                    Modifier
                        .animateItem()
                        .fillMaxWidth()
                        .diaryPage(theme)
                        .padding(start = 36.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
                ) {
                    if (editMode) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .drawBehind {
                                    drawRoundRect(
                                        theme.accent.copy(alpha = 0.6f),
                                        cornerRadius = CornerRadius(16.dp.toPx()),
                                        style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))),
                                    )
                                }
                                .clip(RoundedCornerShape(16.dp))
                                .combinedClickable(onClick = { editing = EditRequest(block, i) })
                                .padding(12.dp),
                        ) {
                            JournalBlockView(block, theme, p, mediaById, onOpenMedia = { editing = EditRequest(block, i) }, stops = route)
                            Spacer(Modifier.height(8.dp))
                            BlockToolbar(
                                theme = theme,
                                canUp = i > 0,
                                canDown = i < list.lastIndex,
                                onUp = { vm.moveBlock(block, -1) },
                                onDown = { vm.moveBlock(block, 1) },
                                onEdit = { editing = EditRequest(block, i) },
                                onCopy = { vm.duplicateBlock(block) },
                                onDelete = { vm.deleteBlock(block) },
                            )
                        }
                        InsertHere(theme) { paletteAt = i + 1 }
                    } else {
                        JournalBlockView(block, theme, p, mediaById, onOpenMedia = openMedia, modifier = Modifier.appear(i.coerceAtMost(4)), stops = route)
                    }
                }
            }

            item(key = "page-end") {
                Box(Modifier.fillMaxWidth().height(36.dp).diaryPage(theme))
            }

            // ---------- RULLINO ----------
            item(key = "roll-title") {
                Column(Modifier.fillMaxWidth().background(theme.pageAlt).padding(top = 12.dp)) {
                    SectionTitle("Rullino · ${d.media.size}") {
                        FilledTonalButton(onClick = importToRoll, shape = RoundedCornerShape(16.dp)) {
                            Icon(Icons.Rounded.AddAPhoto, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Aggiungi")
                        }
                    }
                    AnimatedVisibility(importing, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                        LinearProgressIndicator(
                            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp).clip(RoundedCornerShape(4.dp)),
                            color = Sea,
                        )
                    }
                    Text(
                        "Tutte le foto e i video di questa esperienza. Usali nella pagina con Foto o Collage.",
                        style = MaterialTheme.typography.bodySmall,
                        color = theme.soft,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }
            val rows = d.media.chunked(3)
            if (rows.isEmpty()) {
                item(key = "roll-empty") {
                    Box(Modifier.fillMaxWidth().background(theme.pageAlt).padding(16.dp)) {
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                                .border(2.dp, theme.soft.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                                .bounceClick(onClick = importToRoll).padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("📷", fontSize = 36.sp)
                            Text("Aggiungi le foto e i video del viaggio", style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
            }
            itemsIndexed(rows, key = { r, _ -> "roll$r" }) { r, row ->
                Row(
                    Modifier.fillMaxWidth().background(theme.pageAlt).padding(horizontal = 3.dp, vertical = 1.5.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    row.forEachIndexed { c, m ->
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .combinedClickable(onClick = { onOpenMedia(r * 3 + c) }, onLongClick = { mediaOptions = m }),
                        ) {
                            AsyncImage(File(m.path), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            if (m.isVideo) {
                                Box(
                                    Modifier.align(Alignment.Center).size(34.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)),
                                    contentAlignment = Alignment.Center,
                                ) { Icon(Icons.Rounded.PlayArrow, null, tint = Color.White) }
                            }
                            if (m.path == p.coverPath) {
                                Text("⭐", fontSize = 13.sp, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp))
                            }
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            item(key = "roll-pad") { Box(Modifier.fillMaxWidth().height(40.dp).background(theme.pageAlt)) }
        }
        }

        // ---------- BARRA SUPERIORE ----------
        val barBg by animateColorAsState(
            if (scrolledPastHero) theme.page.copy(alpha = 0.96f) else Color.Transparent, label = "bar",
        )
        Row(
            Modifier.fillMaxWidth().background(barBg).statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleIconButton(Icons.AutoMirrored.Rounded.ArrowBack, onClick = onBack)
            Box(Modifier.weight(1f)) {
                androidx.compose.animation.AnimatedVisibility(scrolledPastHero, enter = fadeIn(), exit = fadeOut()) {
                    Text(
                        p.title, color = theme.ink, fontFamily = theme.font, fontWeight = FontWeight.Bold,
                        fontSize = 18.sp, maxLines = 1, modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
            }
            LikeButton(p.favorite, { vm.toggleFavorite(p) }, tintOff = if (scrolledPastHero) theme.ink else Color.White)
            Spacer(Modifier.width(8.dp))
            Box {
                CircleIconButton(Icons.Rounded.MoreVert, onClick = { menu = true })
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text("Dati dell'esperienza") },
                        leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                        onClick = { menu = false; onEdit() },
                    )
                    DropdownMenuItem(
                        text = { Text("Tema della pagina") },
                        leadingIcon = { Icon(Icons.Rounded.Palette, null) },
                        onClick = { menu = false; showThemes = true },
                    )
                    DropdownMenuItem(
                        text = { Text("Aggiungi foto/video") },
                        leadingIcon = { Icon(Icons.Rounded.AddAPhoto, null) },
                        onClick = { menu = false; importToRoll() },
                    )
                    DropdownMenuItem(
                        text = { Text("Elimina album", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { menu = false; confirmDelete = true },
                    )
                }
            }
        }

        // ---------- PULSANTE PERSONALIZZA ----------
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(20.dp),
        ) {
            AnimatedContent(editMode, label = "fab", transitionSpec = {
                (scaleIn(spring(dampingRatio = 0.6f)) + fadeIn()) togetherWith (scaleOut() + fadeOut())
            }) { on ->
                if (on) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                .bounceClick { paletteAt = list.size },
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Rounded.Add, "Aggiungi elemento") }
                        GradientButton("Fine", icon = Icons.Rounded.Check) { editMode = false }
                    }
                } else {
                    GradientButton("Personalizza", icon = Icons.Rounded.Edit) { editMode = true }
                }
            }
        }
    }

    // ---------- FOGLI ----------
    paletteAt?.let { at ->
        BlockPaletteSheet(onDismiss = { paletteAt = null }) { type ->
            paletteAt = null
            val defaults = when (type) {
                "HEADING" -> JournalBlock(placeId = p.id, type = type, text = "", size = 2)
                "STICKER" -> JournalBlock(placeId = p.id, type = type, text = "", align = "center")
                "STAMP" -> JournalBlock(placeId = p.id, type = type, align = "end")
                "NOTE" -> JournalBlock(placeId = p.id, type = type, style = "yellow")
                "PHOTO" -> JournalBlock(placeId = p.id, type = type, style = "polaroid")
                "DIVIDER" -> JournalBlock(placeId = p.id, type = type, style = "plane")
                else -> JournalBlock(placeId = p.id, type = type)
            }
            if (type == "DIVIDER" || type == "ROUTE") vm.saveBlock(defaults, at) else editing = EditRequest(defaults, at)
        }
    }

    editing?.let { req ->
        BlockEditorSheet(
            initial = req.block,
            theme = theme,
            place = p,
            media = d.media,
            onImport = { multiple, cb ->
                pendingImport = cb
                if (multiple) multiPicker.launch(request) else singlePicker.launch(request)
            },
            onDismiss = { editing = null },
            onSave = { b ->
                vm.saveBlock(b, req.index)
                editing = null
            },
            stops = route,
        )
    }

    if (showThemes) {
        ThemePickerSheet(p.theme, onDismiss = { showThemes = false }) { id ->
            vm.setTheme(p.id, id)
            showThemes = false
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Eliminare questo album?") },
            text = { Text("Verranno eliminati la pagina di diario e tutti i ${d.media.size} file del rullino. L'operazione non è reversibile.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onBack()
                    vm.deletePlace(p)
                }) { Text("Elimina", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Annulla") } },
        )
    }

    mediaOptions?.let { m ->
        AlertDialog(
            onDismissRequest = { mediaOptions = null },
            title = { Text(if (m.isVideo) "Video" else "Foto") },
            text = {
                Column {
                    if (!m.isVideo) {
                        ListItem(
                            headlineContent = { Text("Usa come copertina") },
                            leadingContent = { Icon(Icons.Rounded.Wallpaper, null) },
                            modifier = Modifier.clip(RoundedCornerShape(12.dp)).bounceClick { vm.setCover(p.id, m.path); mediaOptions = null },
                        )
                    }
                    ListItem(
                        headlineContent = { Text("Elimina dal rullino", color = MaterialTheme.colorScheme.error) },
                        leadingContent = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) },
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).bounceClick { vm.deleteMedia(m); mediaOptions = null },
                    )
                }
            },
            confirmButton = { TextButton(onClick = { mediaOptions = null }) { Text("Chiudi") } },
        )
    }
}

@Composable
private fun InsertHere(theme: DiaryTheme, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
        Row(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(theme.accent.copy(alpha = 0.12f))
                .bounceClick(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Add, null, tint = theme.accent, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("inserisci qui", color = theme.accent, fontSize = 12.sp, fontFamily = FontFamily.SansSerif)
        }
    }
}

@Composable
private fun BlockToolbar(
    theme: DiaryTheme,
    canUp: Boolean,
    canDown: Boolean,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onEdit: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        ToolIcon(Icons.Rounded.ArrowUpward, theme, canUp, onUp)
        ToolIcon(Icons.Rounded.ArrowDownward, theme, canDown, onDown)
        ToolIcon(Icons.Rounded.Edit, theme, true, onEdit)
        ToolIcon(Icons.Rounded.ContentCopy, theme, true, onCopy)
        ToolIcon(Icons.Rounded.Delete, theme, true, onDelete, danger = true)
    }
}

@Composable
private fun ToolIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    theme: DiaryTheme,
    enabled: Boolean,
    onClick: () -> Unit,
    danger: Boolean = false,
) {
    Box(
        Modifier
            .padding(start = 6.dp)
            .size(34.dp)
            .clip(CircleShape)
            .background(theme.ink.copy(alpha = if (enabled) 0.08f else 0.03f))
            .bounceClick(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon, null,
            tint = when {
                !enabled -> theme.soft.copy(alpha = 0.3f)
                danger -> Color(0xFFD64545)
                else -> theme.ink
            },
            modifier = Modifier.size(18.dp),
        )
    }
}
