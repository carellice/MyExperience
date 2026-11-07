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
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.MapTileIndex
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

/** Tile Esri World Street Map, colori simili a Google Maps e nessuna API key (CARTO ora la richiede). */
private val EsriStreets = object : OnlineTileSourceBase(
    "EsriStreets", 1, 19, 256, "",
    arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/"),
    "Tiles © Esri",
) {
    // Esri vuole z/y/x, osmdroid di default costruisce z/x/y
    override fun getTileURLString(index: Long) =
        baseUrl + MapTileIndex.getZoom(index) + "/" + MapTileIndex.getY(index) + "/" + MapTileIndex.getX(index)
}

/** Esri Canvas scura (senza API key): fondo e nomi su due livelli, fino allo zoom 16 (oltre si ingrandisce). */
private fun esriCanvas(layer: String) = object : OnlineTileSourceBase(
    "EsriDark$layer", 1, 16, 256, "",
    arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Dark_Gray_$layer/MapServer/tile/"),
    "Tiles © Esri",
) {
    override fun getTileURLString(index: Long) =
        baseUrl + MapTileIndex.getZoom(index) + "/" + MapTileIndex.getY(index) + "/" + MapTileIndex.getX(index)
}

/** Tinta blu notte sul fondo grigio di Esri, come il tema scuro di Google Maps. */
private fun nightTint() = android.graphics.ColorMatrixColorFilter(
    floatArrayOf(
        0.72f, 0f, 0f, 0f, -4f,
        0f, 0.84f, 0f, 0f, 4f,
        0f, 0f, 1.08f, 0f, 16f,
        0f, 0f, 0f, 1f, 0f,
    )
)

/** Mappa in stile Google Maps senza API key: Esri stradale di giorno, Esri scura con i nomi in tema scuro. */
private fun MapView.useGoogleLikeTiles(dark: Boolean) {
    if (!dark) {
        setTileSource(EsriStreets)
        return
    }
    setTileSource(esriCanvas("Base"))
    overlayManager.tilesOverlay.setColorFilter(nightTint())
    val labels = org.osmdroid.views.overlay.TilesOverlay(
        org.osmdroid.tileprovider.MapTileProviderBasic(context, esriCanvas("Reference")), context,
    ).apply {
        loadingBackgroundColor = android.graphics.Color.TRANSPARENT
        loadingLineColor = android.graphics.Color.TRANSPARENT
    }
    overlays.add(0, labels) // sotto i pin
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
            useGoogleLikeTiles(dark)
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
            "© Esri · © OpenStreetMap contributors",
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
