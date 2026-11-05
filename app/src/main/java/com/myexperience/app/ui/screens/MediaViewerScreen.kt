package com.myexperience.app.ui.screens

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.myexperience.app.MainViewModel
import com.myexperience.app.ui.components.CircleIconButton
import com.myexperience.app.ui.components.GlassChip
import com.myexperience.app.ui.theme.Pink
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun MediaViewerScreen(vm: MainViewModel, placeId: Long, startIndex: Int, onBack: () -> Unit) {
    val flow = remember(placeId) { vm.place(placeId) }
    val data by flow.collectAsStateWithLifecycle(initialValue = null)
    val media = data?.media
    if (media == null) {
        Box(Modifier.fillMaxSize().background(Color.Black))
        return
    }
    if (media.isEmpty()) {
        LaunchedEffect(Unit) { onBack() }
        Box(Modifier.fillMaxSize().background(Color.Black))
        return
    }

    val pager = rememberPagerState(initialPage = startIndex.coerceIn(0, media.lastIndex)) { media.size }
    val thumbs = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var chrome by remember { mutableStateOf(true) }
    val chromeAlpha by animateFloatAsState(if (chrome) 1f else 0f, label = "chrome")

    LaunchedEffect(pager.currentPage) {
        if (pager.currentPage in media.indices) thumbs.animateScrollToItem((pager.currentPage - 2).coerceAtLeast(0))
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(
            state = pager,
            key = { media[it].id },
            beyondViewportPageCount = 1,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            val item = media[page]
            if (item.isVideo) {
                VideoPlayer(item.path, active = pager.currentPage == page)
            } else {
                ZoomableImage(item.path, onTap = { chrome = !chrome })
            }
        }

        val current = media.getOrNull(pager.currentPage)
        Row(
            Modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = chromeAlpha }
                .statusBarsPadding()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleIconButton(Icons.Rounded.Close, onClick = onBack)
            Spacer(Modifier.width(10.dp))
            GlassChip("${pager.currentPage + 1} / ${media.size}")
            Spacer(Modifier.weight(1f))
            if (current != null && !current.isVideo) {
                CircleIconButton(Icons.Rounded.Wallpaper, onClick = { vm.setCover(placeId, current.path) })
                Spacer(Modifier.width(8.dp))
            }
            if (current != null) {
                CircleIconButton(Icons.Rounded.Delete, onClick = { vm.deleteMedia(current) })
            }
        }

        LazyRow(
            state = thumbs,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .graphicsLayer { alpha = chromeAlpha }
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(media, key = { _, m -> m.id }) { i, m ->
                val selected = i == pager.currentPage
                val s by animateFloatAsState(if (selected) 1.12f else 1f, label = "thumb")
                AsyncImage(
                    File(m.path), null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(54.dp)
                        .graphicsLayer { scaleX = s; scaleY = s }
                        .clip(RoundedCornerShape(12.dp))
                        .then(if (selected) Modifier.border(2.dp, Pink, RoundedCornerShape(12.dp)) else Modifier)
                        .pointerInput(i) { detectTapGestures { scope.launch { pager.animateScrollToPage(i) } } },
                )
            }
        }
    }
}

@Composable
private fun ZoomableImage(path: String, onTap: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val shownScale by animateFloatAsState(scale, label = "zoom")

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f; offset = Offset.Zero
                        } else scale = 2.5f
                    },
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val multi = event.changes.size > 1
                        if (multi || scale > 1f) {
                            val zoom = event.calculateZoom()
                            val pan = event.calculatePan()
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            offset = if (scale > 1f) offset + pan else Offset.Zero
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = File(path),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = shownScale
                    scaleY = shownScale
                    translationX = offset.x
                    translationY = offset.y
                },
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun VideoPlayer(path: String, active: Boolean) {
    val context = LocalContext.current
    val player = remember(path) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(File(path))))
            repeatMode = Player.REPEAT_MODE_ONE
            prepare()
        }
    }
    LaunchedEffect(active) {
        player.playWhenReady = active
        if (!active) player.pause()
    }
    DisposableEffect(player) { onDispose { player.release() } }
    AndroidView(
        factory = {
            PlayerView(it).apply {
                this.player = player
                useController = true
                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                setBackgroundColor(android.graphics.Color.BLACK)
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}
