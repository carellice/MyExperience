package com.myexperience.app.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
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
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

data class MapPin(
    val id: Long,
    val lat: Double,
    val lon: Double,
    val title: String,
    val color: Int = 0xFFC8693F.toInt(),
)

/**
 * Mappa vettoriale in stile Google Maps (MapLibre + tile OpenFreeMap, nessuna API key).
 * Gli stili chiaro e scuro sono in assets/map_light.json e map_dark.json.
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
    var ready by remember { mutableStateOf<Pair<MapLibreMap, Style>?>(null) }
    val fitted = remember { arrayOfNulls<String>(1) }

    val mapView = remember {
        MapView(context).apply {
            onCreate(null)
            if (interactive) {
                // evita che lo scroll del contenitore "rubi" i gesti della mappa
                setOnTouchListener { v, _ ->
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                    false
                }
            }
            getMapAsync { map ->
                map.uiSettings.apply {
                    isLogoEnabled = false
                    isAttributionEnabled = false
                    isCompassEnabled = false
                    isRotateGesturesEnabled = false
                    isTiltGesturesEnabled = false
                    setAllGesturesEnabled(interactive)
                }
                map.setMinZoomPreference(1.0)
                map.addOnMapClickListener { at ->
                    val hit = map.queryRenderedFeatures(map.projection.toScreenLocation(at), PINS_LAYER).firstOrNull()
                    val id = hit?.getNumberProperty("id")?.toLong()
                    val tap = mapTap
                    when {
                        id != null -> pinClick(id)
                        tap != null -> tap(at.latitude, at.longitude)
                        else -> return@addOnMapClickListener false
                    }
                    true
                }
                map.setStyle(Style.Builder().fromUri("asset://map_${if (dark) "dark" else "light"}.json")) { style ->
                    style.addSource(GeoJsonSource(ROUTE_SOURCE))
                    style.addSource(GeoJsonSource(PINS_SOURCE))
                    style.addLayer(
                        LineLayer(ROUTE_LAYER, ROUTE_SOURCE).withProperties(
                            PropertyFactory.lineColor(0xFF1F6E80.toInt()),
                            PropertyFactory.lineWidth(3.5f),
                            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                            PropertyFactory.lineDasharray(arrayOf(2f, 1.6f)),
                        )
                    )
                    style.addLayer(
                        SymbolLayer(PINS_LAYER, PINS_SOURCE).withProperties(
                            PropertyFactory.iconImage(Expression.get("icon")),
                            PropertyFactory.iconAnchor(Property.ICON_ANCHOR_BOTTOM),
                            PropertyFactory.iconAllowOverlap(true),
                            PropertyFactory.iconIgnorePlacement(true),
                        )
                    )
                    ready = map to style
                }
            }
        }
    }

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        mapView.onStart()
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onResume()
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    // pin, percorso e inquadratura seguono i dati (dopo che lo stile è pronto)
    ready?.let { (map, style) -> SideEffect {
        val features = pins.mapIndexed { i, pin ->
            val label = if (drawRoute) (i + 1).toString() else null
            val key = "pin_${pin.color}_${label.orEmpty()}"
            if (style.getImage(key) == null) style.addImage(key, markerIcon(context, pin.color, label))
            Feature.fromGeometry(Point.fromLngLat(pin.lon, pin.lat)).apply {
                addNumberProperty("id", pin.id)
                addStringProperty("icon", key)
            }
        }
        style.getSourceAs<GeoJsonSource>(PINS_SOURCE)?.setGeoJson(FeatureCollection.fromFeatures(features))
        val route = if (drawRoute && pins.size > 1) {
            listOf(Feature.fromGeometry(LineString.fromLngLats(pins.map { Point.fromLngLat(it.lon, it.lat) })))
        } else emptyList()
        style.getSourceAs<GeoJsonSource>(ROUTE_SOURCE)?.setGeoJson(FeatureCollection.fromFeatures(route))
        val key = if (autoFit) pins.joinToString { "${it.lat},${it.lon}" } + "|" + focus else "f|$focus"
        if (fitted[0] != key) {
            fitted[0] = key
            fit(mapView, map, if (autoFit) pins else emptyList(), focus)
        }
    } }

    Box(modifier) {
        AndroidView(factory = { mapView }, modifier = Modifier.matchParentSize())
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
            "© OpenFreeMap · © OpenStreetMap contributors",
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

private const val PINS_SOURCE = "pins"
private const val PINS_LAYER = "pins-layer"
private const val ROUTE_SOURCE = "route"
private const val ROUTE_LAYER = "route-layer"

/** Zoom MapLibre (tile da 512 px): uno in meno rispetto alla vecchia mappa a tile da 256 px. */
private fun fit(view: MapView, map: MapLibreMap, pins: List<MapPin>, focus: Pair<Double, Double>?) {
    val action = {
        val distinct = pins.map { it.lat to it.lon }.distinct()
        when {
            focus != null -> map.moveCamera(CameraUpdateFactory.newCameraPosition(at(focus.first, focus.second, 10.0)))
            distinct.size == 1 -> map.moveCamera(CameraUpdateFactory.newCameraPosition(at(distinct[0].first, distinct[0].second, 8.0)))
            distinct.size > 1 -> {
                val bounds = LatLngBounds.Builder().apply { distinct.forEach { include(LatLng(it.first, it.second)) } }.build()
                val pad = (60 * view.resources.displayMetrics.density).toInt()
                map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, pad))
                if (map.cameraPosition.zoom > 11.0) map.moveCamera(CameraUpdateFactory.zoomTo(11.0))
            }
            else -> map.moveCamera(CameraUpdateFactory.newCameraPosition(at(42.0, 12.5, 2.0)))
        }
    }
    if (view.width > 0 && view.height > 0) action() else view.post { action() }
}

private fun at(lat: Double, lon: Double, zoom: Double) = CameraPosition.Builder().target(LatLng(lat, lon)).zoom(zoom).build()

private fun markerIcon(context: Context, color: Int, label: String?): Bitmap {
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
    return bmp
}
