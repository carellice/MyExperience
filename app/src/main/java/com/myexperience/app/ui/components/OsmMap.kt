package com.myexperience.app.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.myexperience.app.ui.theme.LocalDarkTheme
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import kotlin.math.max
import kotlin.math.min

data class MapPin(
    val id: Long,
    val lat: Double,
    val lon: Double,
    val title: String,
    val color: Int = 0xFFC8693F.toInt(),
)

/** Tile standard OpenStreetMap: gratuite, nessuna API key. */
private val OsmTiles = XYTileSource(
    "OSM", 1, 19, 256, ".png",
    arrayOf("https://tile.openstreetmap.org/"),
    "© OpenStreetMap contributors",
)

/** Filtro per tema scuro: inverte e ruota la tinta, così l'acqua resta blu. */
private fun darkTileFilter(): android.graphics.ColorMatrixColorFilter {
    val invert = android.graphics.ColorMatrix(
        floatArrayOf(
            -1f, 0f, 0f, 0f, 255f,
            0f, -1f, 0f, 0f, 255f,
            0f, 0f, -1f, 0f, 255f,
            0f, 0f, 0f, 1f, 0f,
        )
    )
    val hue = android.graphics.ColorMatrix().apply {
        // rotazione di 180° della tinta (approssimata) per mantenere i colori naturali
        val cos = -1f
        val lumR = 0.213f; val lumG = 0.715f; val lumB = 0.072f
        set(
            floatArrayOf(
                lumR + cos * (1 - lumR) + 0f, lumG + cos * (-lumG), lumB + cos * (-lumB), 0f, 0f,
                lumR + cos * (-lumR), lumG + cos * (1 - lumG), lumB + cos * (-lumB), 0f, 0f,
                lumR + cos * (-lumR), lumG + cos * (-lumG), lumB + cos * (1 - lumB), 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            )
        )
    }
    val dim = android.graphics.ColorMatrix().apply { setScale(0.85f, 0.85f, 0.9f, 1f) }
    invert.postConcat(hue)
    invert.postConcat(dim)
    return android.graphics.ColorMatrixColorFilter(invert)
}

/**
 * Mappa OpenStreetMap (nessuna API key necessaria).
 * @param autoFit se true inquadra automaticamente tutti i pin.
 * @param focus se valorizzato, centra la mappa su quel punto.
 */
@SuppressLint("ClickableViewAccessibility")
@Composable
fun OsmMap(
    pins: List<MapPin>,
    modifier: Modifier = Modifier,
    drawRoute: Boolean = false,
    autoFit: Boolean = true,
    focus: Pair<Double, Double>? = null,
    onPinClick: (Long) -> Unit = {},
    onMapTap: ((Double, Double) -> Unit)? = null,
    /** false = anteprima statica dentro una lista: non intercetta gli swipe. */
    interactive: Boolean = true,
    onStaticClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val dark = LocalDarkTheme.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val pinClick by rememberUpdatedState(onPinClick)
    val mapTap by rememberUpdatedState(onMapTap)

    val mapView = remember {
        MapView(context).apply {
            setTileSource(OsmTiles)
            if (dark) overlayManager.tilesOverlay.setColorFilter(darkTileFilter())
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            isTilesScaledToDpi = true
            minZoomLevel = 2.0
            maxZoomLevel = 19.0
            isHorizontalMapRepetitionEnabled = true
            isVerticalMapRepetitionEnabled = false
            setScrollableAreaLimitLatitude(
                MapView.getTileSystem().maxLatitude,
                MapView.getTileSystem().minLatitude,
                0,
            )
            setBackgroundColor(if (dark) 0xFF0D1416.toInt() else 0xFFEAE3D3.toInt())
            if (interactive) {
                // evita che lo scroll del contenitore "rubi" i gesti della mappa
                setOnTouchListener { v, _ ->
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                    false
                }
            }
        }
    }

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        mapView.onResume()
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onDetach()
        }
    }

    Box(modifier) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.matchParentSize(),
            update = { map ->
                map.overlays.clear()
                if (mapTap != null) {
                    map.overlays.add(MapEventsOverlay(object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                            mapTap?.invoke(p.latitude, p.longitude)
                            return true
                        }

                        override fun longPressHelper(p: GeoPoint): Boolean = false
                    }))
                }
                if (drawRoute && pins.size > 1) {
                    val line = Polyline(map)
                    line.setPoints(pins.map { GeoPoint(it.lat, it.lon) })
                    line.outlinePaint.apply {
                        color = 0xFF1F6E80.toInt()
                        strokeWidth = 9f
                        strokeCap = Paint.Cap.ROUND
                        pathEffect = DashPathEffect(floatArrayOf(28f, 18f), 0f)
                        isAntiAlias = true
                    }
                    line.infoWindow = null
                    map.overlays.add(line)
                }
                pins.forEachIndexed { i, pin ->
                    val m = Marker(map)
                    m.position = GeoPoint(pin.lat, pin.lon)
                    m.icon = markerIcon(context, pin.color, if (drawRoute) (i + 1).toString() else null)
                    m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    m.title = pin.title
                    m.infoWindow = null
                    m.setOnMarkerClickListener { _, _ ->
                        pinClick(pin.id)
                        true
                    }
                    map.overlays.add(m)
                }
                val key = if (autoFit) pins.joinToString { "${it.lat},${it.lon}" } + "|" + focus else "f|$focus"
                if (map.tag != key) {
                    map.tag = key
                    fit(map, if (autoFit) pins else emptyList(), focus)
                }
                map.invalidate()
            },
        )
        if (!interactive) {
            // Strato trasparente: assorbe tocchi (apre la mappa) e lascia scorrere la pagina
            Box(
                Modifier
                    .matchParentSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onStaticClick?.invoke() },
            )
        }
        Text(
            "© OpenStreetMap contributors",
            fontSize = 9.sp,
            color = Color.Black.copy(alpha = 0.7f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(6.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White.copy(alpha = 0.6f))
                .padding(horizontal = 4.dp, vertical = 1.dp),
        )
    }
}

private fun fit(map: MapView, pins: List<MapPin>, focus: Pair<Double, Double>?) {
    val action = {
        val distinct = pins.map { it.lat to it.lon }.distinct()
        when {
            focus != null -> {
                map.controller.setZoom(11.0)
                map.controller.setCenter(GeoPoint(focus.first, focus.second))
            }
            distinct.size == 1 -> {
                map.controller.setZoom(9.0)
                map.controller.setCenter(GeoPoint(distinct[0].first, distinct[0].second))
            }
            distinct.size > 1 -> {
                val lats = distinct.map { it.first }
                val lons = distinct.map { it.second }
                val padLat = max((lats.max() - lats.min()) * 0.25, 0.4)
                val padLon = max((lons.max() - lons.min()) * 0.25, 0.4)
                val box = BoundingBox(
                    min(84.0, lats.max() + padLat),
                    min(179.0, lons.max() + padLon),
                    max(-84.0, lats.min() - padLat),
                    max(-179.0, lons.min() - padLon),
                )
                map.zoomToBoundingBox(box, false, 60)
                if (map.zoomLevelDouble > 12.0) map.controller.setZoom(12.0)
            }
            else -> {
                map.controller.setZoom(3.0)
                map.controller.setCenter(GeoPoint(42.0, 12.5))
            }
        }
    }
    if (map.width > 0 && map.height > 0) action()
    else map.addOnFirstLayoutListener { _, _, _, _, _ -> action() }
}

private fun markerIcon(context: Context, color: Int, label: String?): Drawable {
    val d = context.resources.displayMetrics.density
    val w = (36 * d).toInt()
    val h = (46 * d).toInt()
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    val r = w / 2f - 3 * d
    val cx = w / 2f
    val cy = r + 3 * d

    // ombra
    p.color = 0x55000000
    c.drawCircle(cx, h - 3 * d, 4 * d, p)

    // goccia
    val path = Path().apply {
        moveTo(cx - r * 0.72f, cy + r * 0.62f)
        lineTo(cx, h - 4 * d)
        lineTo(cx + r * 0.72f, cy + r * 0.62f)
        close()
    }
    p.color = android.graphics.Color.WHITE
    c.drawCircle(cx, cy, r + 2 * d, p)
    p.color = color
    c.drawPath(path, p)
    c.drawCircle(cx, cy, r, p)

    p.color = android.graphics.Color.WHITE
    if (label != null) {
        c.drawCircle(cx, cy, r * 0.66f, p)
        p.color = color
        p.textSize = r * 0.9f
        p.textAlign = Paint.Align.CENTER
        p.isFakeBoldText = true
        c.drawText(label, cx, cy - (p.descent() + p.ascent()) / 2, p)
    } else {
        c.drawCircle(cx, cy, r * 0.38f, p)
    }
    return BitmapDrawable(context.resources, bmp)
}
