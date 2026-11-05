package com.myexperience.app.ui.globe

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix
import android.os.SystemClock
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import java.time.ZoneOffset
import java.time.ZonedDateTime
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLDisplay
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/** Punto evidenziato sul globo. kind: 0 = visitato, 1 = in programma, 2 = nuovo (temporaneo). */
data class GlobeMarker(
    val id: Long,
    val lat: Double,
    val lon: Double,
    val kind: Int,
)

/**
 * Globo terrestre 3D realistico (OpenGL ES 2.0):
 * texture NASA giorno/notte, nuvole in movimento, riflessi degli oceani,
 * atmosfera, ciclo giorno/notte reale, campo stellato e pin luminosi.
 */
@SuppressLint("ViewConstructor", "ClickableViewAccessibility")
class GlobeView(context: Context, private val camera: FloatArray) : GLSurfaceView(context) {

    var onMarkerTap: ((GlobeMarker) -> Unit)? = null
    var onEmptyTap: (() -> Unit)? = null
    var onLongPress: ((Double, Double) -> Unit)? = null
    var onReady: (() -> Unit)? = null

    private val renderer = GlobeRenderer(context, camera)

    init {
        setEGLContextClientVersion(2)
        setEGLConfigChooser(MultisampleConfigChooser())
        preserveEGLContextOnPause = true
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
        renderer.onFirstFrame = { post { onReady?.invoke() } }
    }

    fun setMarkers(markers: List<GlobeMarker>, selectedId: Long?) {
        renderer.setMarkers(markers, selectedId)
    }

    fun flyTo(lat: Double, lon: Double, zoomIn: Boolean) = renderer.flyTo(lat, lon, zoomIn)

    /** Rotazione automatica quando inattivo (disattivata mentre una scheda è aperta). */
    fun setAutoRotate(enabled: Boolean) {
        renderer.autoRotate = enabled
    }

    // ------------------------------------------------------------------ gesti

    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            renderer.zoomBy(detector.scaleFactor)
            return true
        }
    })

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            renderer.touchDown()
            return true
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, dx: Float, dy: Float): Boolean {
            if (e2.pointerCount > 1) return true
            renderer.dragBy(dx, dy)
            return true
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
            renderer.fling(vx, vy)
            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            val m = renderer.pick(e.x, e.y, 34f * resources.displayMetrics.density)
            if (m != null) onMarkerTap?.invoke(m) else onEmptyTap?.invoke()
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            renderer.surfacePoint(e.x, e.y)?.let { (lat, lon) -> renderer.flyTo(lat, lon, true) }
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            renderer.surfacePoint(e.x, e.y)?.let { (lat, lon) -> onLongPress?.invoke(lat, lon) }
        }
    })

    override fun onTouchEvent(event: MotionEvent): Boolean {
        parent?.requestDisallowInterceptTouchEvent(true)
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
            renderer.touchUp()
        }
        return true
    }
}

/** Prova prima l'antialiasing 4x, poi ripiega su una configurazione semplice. */
private class MultisampleConfigChooser : GLSurfaceView.EGLConfigChooser {
    override fun chooseConfig(egl: EGL10, display: EGLDisplay): EGLConfig {
        val msaa = intArrayOf(
            EGL10.EGL_RED_SIZE, 8, EGL10.EGL_GREEN_SIZE, 8, EGL10.EGL_BLUE_SIZE, 8,
            EGL10.EGL_DEPTH_SIZE, 16, EGL10.EGL_RENDERABLE_TYPE, 4,
            EGL10.EGL_SAMPLE_BUFFERS, 1, EGL10.EGL_SAMPLES, 4, EGL10.EGL_NONE,
        )
        val basic = intArrayOf(
            EGL10.EGL_RED_SIZE, 8, EGL10.EGL_GREEN_SIZE, 8, EGL10.EGL_BLUE_SIZE, 8,
            EGL10.EGL_DEPTH_SIZE, 16, EGL10.EGL_RENDERABLE_TYPE, 4, EGL10.EGL_NONE,
        )
        val fallback = intArrayOf(EGL10.EGL_DEPTH_SIZE, 16, EGL10.EGL_RENDERABLE_TYPE, 4, EGL10.EGL_NONE)
        for (attrs in listOf(msaa, basic, fallback)) {
            val num = IntArray(1)
            val configs = arrayOfNulls<EGLConfig>(1)
            if (egl.eglChooseConfig(display, attrs, configs, 1, num) && num[0] > 0 && configs[0] != null) {
                return configs[0]!!
            }
        }
        throw IllegalStateException("Nessuna configurazione EGL disponibile")
    }
}

private class GlobeRenderer(private val context: Context, private val camera: FloatArray) : GLSurfaceView.Renderer {

    var onFirstFrame: (() -> Unit)? = null
    private var firstFrameDone = false

    // ---- stato camera (gradi) ----
    @Volatile private var yaw = camera[0]
    @Volatile private var pitch = camera[1]
    @Volatile private var dist = camera[2]
    @Volatile private var velYaw = 0f
    @Volatile private var velPitch = 0f
    @Volatile private var touching = false
    @Volatile private var lastInteraction = SystemClock.uptimeMillis()
    @Volatile var autoRotate = true

    // volo animato
    @Volatile private var flying = false
    private var flyT = 0f
    private var flyFrom = FloatArray(3)
    private var flyTo = FloatArray(3)

    private var defaultDist = 3f
    private var minDist = 1.25f
    private var maxDist = 5f
    private var viewW = 1
    private var viewH = 1
    private val fov = 40f

    // ---- matrici ----
    private val proj = FloatArray(16)
    private val view = FloatArray(16)
    private val model = FloatArray(16)
    private val mv = FloatArray(16)
    private val mvp = FloatArray(16)
    private val camObj = FloatArray(4)
    private val lastMvp = FloatArray(16)
    private val lastCamObj = FloatArray(3)
    private val lock = Any()

    // ---- marker ----
    private var markers: List<GlobeMarker> = emptyList()
    private var selectedId: Long? = null
    @Volatile private var markersDirty = true
    private var markerBuffer: FloatBuffer? = null
    private var markerCount = 0

    // ---- GL ----
    private var earthProg = 0
    private var atmoProg = 0
    private var starProg = 0
    private var markerProg = 0
    private var sphereVbo = 0
    private var sphereIbo = 0
    private var sphereIndexCount = 0
    private var quadVbo = 0
    private var texDay = 0
    private var texNight = 0
    private var texClouds = 0
    private var texSpec = 0
    private val sun = FloatArray(3)
    private var sunUpdatedAt = 0L
    private val startTime = SystemClock.uptimeMillis()
    private var lastFrame = SystemClock.uptimeMillis()

    fun setMarkers(list: List<GlobeMarker>, selected: Long?) {
        synchronized(lock) {
            markers = list
            selectedId = selected
            markersDirty = true
        }
    }

    // ------------------------------------------------------------ interazione

    fun touchDown() {
        touching = true
        flying = false
        velYaw = 0f
        velPitch = 0f
        lastInteraction = SystemClock.uptimeMillis()
    }

    fun touchUp() {
        touching = false
        lastInteraction = SystemClock.uptimeMillis()
    }

    private fun degPerPx(): Float {
        val d = max(dist, 1.01f)
        val radiusPx = (viewH / 2f) / tan(Math.toRadians(fov / 2.0).toFloat()) / sqrt(d * d - 1f)
        return (57.3f / max(radiusPx, 1f))
    }

    fun dragBy(dx: Float, dy: Float) {
        val k = degPerPx()
        yaw += dx * k
        pitch = (pitch - dy * k).coerceIn(-80f, 80f)
        lastInteraction = SystemClock.uptimeMillis()
    }

    fun fling(vx: Float, vy: Float) {
        val k = degPerPx()
        velYaw = (-vx * k * 0.45f).coerceIn(-260f, 260f)
        velPitch = (vy * k * 0.45f).coerceIn(-120f, 120f)
    }

    fun zoomBy(factor: Float) {
        flying = false
        dist = (dist / factor).coerceIn(minDist, maxDist)
        lastInteraction = SystemClock.uptimeMillis()
    }

    fun flyTo(lat: Double, lon: Double, zoomIn: Boolean) {
        flyFrom = floatArrayOf(yaw, pitch, dist)
        // percorso angolare più breve
        var target = lon.toFloat()
        while (target - yaw > 180f) target -= 360f
        while (target - yaw < -180f) target += 360f
        val targetDist = if (zoomIn) max(minDist + 0.25f, min(dist, defaultDist * 0.55f)) else dist
        flyTo = floatArrayOf(target, lat.toFloat().coerceIn(-75f, 75f), targetDist)
        flyT = 0f
        velYaw = 0f
        velPitch = 0f
        flying = true
        lastInteraction = SystemClock.uptimeMillis()
    }

    /** Marker più vicino al tocco (in pixel), solo se sul lato visibile. */
    fun pick(x: Float, y: Float, radiusPx: Float): GlobeMarker? {
        val m = FloatArray(16)
        val cam = FloatArray(3)
        val list: List<GlobeMarker>
        synchronized(lock) {
            System.arraycopy(lastMvp, 0, m, 0, 16)
            System.arraycopy(lastCamObj, 0, cam, 0, 3)
            list = markers
        }
        var best: GlobeMarker? = null
        var bestD = radiusPx
        val v = FloatArray(4)
        val out = FloatArray(4)
        for (mk in list) {
            val p = latLonToXyz(mk.lat, mk.lon)
            val facing = p[0] * (cam[0] - p[0]) + p[1] * (cam[1] - p[1]) + p[2] * (cam[2] - p[2])
            if (facing <= 0f) continue
            v[0] = p[0]; v[1] = p[1]; v[2] = p[2]; v[3] = 1f
            Matrix.multiplyMV(out, 0, m, 0, v, 0)
            if (out[3] <= 0f) continue
            val sx = (out[0] / out[3] + 1f) / 2f * viewW
            val sy = (1f - out[1] / out[3]) / 2f * viewH
            val d = hypot(sx - x, sy - y)
            if (d < bestD) {
                bestD = d
                best = mk
            }
        }
        return best
    }

    /** Coordinate geografiche del punto del globo sotto al dito, o null se si tocca lo spazio. */
    fun surfacePoint(x: Float, y: Float): Pair<Double, Double>? {
        val m = FloatArray(16)
        synchronized(lock) { System.arraycopy(lastMvp, 0, m, 0, 16) }
        val inv = FloatArray(16)
        if (!Matrix.invertM(inv, 0, m, 0)) return null
        val nx = x / viewW * 2f - 1f
        val ny = 1f - y / viewH * 2f
        val near = FloatArray(4)
        val far = FloatArray(4)
        Matrix.multiplyMV(near, 0, inv, 0, floatArrayOf(nx, ny, -1f, 1f), 0)
        Matrix.multiplyMV(far, 0, inv, 0, floatArrayOf(nx, ny, 1f, 1f), 0)
        for (i in 0..2) {
            near[i] /= near[3]; far[i] /= far[3]
        }
        val dx = far[0] - near[0]; val dy = far[1] - near[1]; val dz = far[2] - near[2]
        val len = sqrt(dx * dx + dy * dy + dz * dz)
        val ux = dx / len; val uy = dy / len; val uz = dz / len
        // intersezione raggio-sfera unitaria
        val b = near[0] * ux + near[1] * uy + near[2] * uz
        val c = near[0] * near[0] + near[1] * near[1] + near[2] * near[2] - 1f
        val disc = b * b - c
        if (disc < 0f) return null
        val t = -b - sqrt(disc)
        val px = near[0] + ux * t; val py = near[1] + uy * t; val pz = near[2] + uz * t
        val lat = Math.toDegrees(asin(py.coerceIn(-1f, 1f).toDouble()))
        val lon = Math.toDegrees(atan2(px.toDouble(), pz.toDouble()))
        return lat to lon
    }

    // ------------------------------------------------------------ GL lifecycle

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.01f, 0.012f, 0.03f, 1f)
        earthProg = program(EARTH_VS, EARTH_FS)
        atmoProg = program(ATMO_VS, ATMO_FS)
        starProg = program(STAR_VS, STAR_FS)
        markerProg = program(MARKER_VS, MARKER_FS)
        buildSphere(64, 128)
        buildQuad()

        val maxTex = IntArray(1)
        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, maxTex, 0)
        val big = maxTex[0] >= 4096
        texDay = loadTexture("earth/day.jpg", if (big) 1 else 2, Bitmap.Config.ARGB_8888)
        texNight = loadTexture("earth/night.jpg", 1, Bitmap.Config.RGB_565)
        texClouds = loadTexture("earth/clouds.jpg", 1, Bitmap.Config.RGB_565)
        texSpec = loadTexture("earth/spec.jpg", 1, Bitmap.Config.RGB_565)
        markersDirty = true
        firstFrameDone = false
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        viewW = max(width, 1)
        viewH = max(height, 1)
        val aspect = viewW.toFloat() / viewH
        Matrix.perspectiveM(proj, 0, fov, aspect, 0.05f, 60f)
        // distanza per cui il globo occupa ~86% del lato corto
        val tanHalf = tan(Math.toRadians(fov / 2.0)).toFloat()
        val targetRadiusNdc = 0.86f * min(1f, aspect) // in NDC verticali
        val s = 1f / (tanHalf * targetRadiusNdc)
        defaultDist = sqrt(s * s + 1f)
        minDist = 1.22f
        maxDist = defaultDist * 1.5f
        if (dist <= 0f) dist = defaultDist
        dist = dist.coerceIn(minDist, maxDist)
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = SystemClock.uptimeMillis()
        val dt = ((now - lastFrame) / 1000f).coerceIn(0f, 0.1f)
        lastFrame = now
        updateCamera(dt, now)

        if (now - sunUpdatedAt > 30_000 || sunUpdatedAt == 0L) {
            computeSun()
            sunUpdatedAt = now
        }

        Matrix.setLookAtM(view, 0, 0f, 0f, dist, 0f, 0f, 0f, 0f, 1f, 0f)
        Matrix.setIdentityM(model, 0)
        Matrix.rotateM(model, 0, pitch, 1f, 0f, 0f)
        Matrix.rotateM(model, 0, -yaw, 0f, 1f, 0f)
        Matrix.multiplyMM(mv, 0, view, 0, model, 0)
        Matrix.multiplyMM(mvp, 0, proj, 0, mv, 0)
        // camera in coordinate oggetto: Rᵀ · (0,0,dist)
        camObj[0] = model[2] * dist
        camObj[1] = model[6] * dist
        camObj[2] = model[10] * dist
        synchronized(lock) {
            System.arraycopy(mvp, 0, lastMvp, 0, 16)
            lastCamObj[0] = camObj[0]; lastCamObj[1] = camObj[1]; lastCamObj[2] = camObj[2]
        }

        val time = (now - startTime) / 1000f
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        drawStars(time)
        drawEarth(time)
        drawAtmosphere()
        drawMarkers(time)

        if (!firstFrameDone) {
            firstFrameDone = true
            onFirstFrame?.invoke()
        }
        camera[0] = yaw; camera[1] = pitch; camera[2] = dist
    }

    private fun updateCamera(dt: Float, now: Long) {
        if (flying) {
            flyT = min(1f, flyT + dt / 1.4f)
            val e = easeInOut(flyT)
            yaw = lerp(flyFrom[0], flyTo[0], e)
            pitch = lerp(flyFrom[1], flyTo[1], e)
            dist = lerp(flyFrom[2], flyTo[2], e)
            if (flyT >= 1f) flying = false
            return
        }
        if (touching) return
        if (abs(velYaw) > 0.01f || abs(velPitch) > 0.01f) {
            yaw += velYaw * dt
            pitch = (pitch + velPitch * dt).coerceIn(-80f, 80f)
            val decay = exp(-2.8f * dt)
            velYaw *= decay
            velPitch *= decay
            if (abs(velYaw) < 0.5f) velYaw = 0f
            if (abs(velPitch) < 0.5f) velPitch = 0f
        } else if (autoRotate && now - lastInteraction > 6000) {
            // rotazione automatica lenta quando il globo è inattivo
            val ramp = ((now - lastInteraction - 6000) / 3000f).coerceIn(0f, 1f)
            yaw -= 2.5f * dt * ramp // verso est, come la vera rotazione terrestre
        }
        if (yaw > 360f) yaw -= 360f
        if (yaw < -360f) yaw += 360f
    }

    /** Punto sub-solare in base a data e ora UTC: il terminatore giorno/notte è reale. */
    private fun computeSun() {
        val t = ZonedDateTime.now(ZoneOffset.UTC)
        val doy = t.dayOfYear
        val hours = t.hour + t.minute / 60.0 + t.second / 3600.0
        val decl = -23.44 * cos(2 * PI / 365.0 * (doy + 10))
        val lon = (12.0 - hours) * 15.0
        val p = latLonToXyz(decl, lon)
        sun[0] = p[0]; sun[1] = p[1]; sun[2] = p[2]
    }

    // ------------------------------------------------------------ disegno

    private fun drawStars(time: Float) {
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glUseProgram(starProg)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, quadVbo)
        val aPos = GLES20.glGetAttribLocation(starProg, "aPos")
        GLES20.glEnableVertexAttribArray(aPos)
        GLES20.glVertexAttribPointer(aPos, 2, GLES20.GL_FLOAT, false, 8, 0)
        GLES20.glUniform2f(GLES20.glGetUniformLocation(starProg, "uOffset"), yaw * 4f, pitch * 4f)
        GLES20.glUniform2f(GLES20.glGetUniformLocation(starProg, "uRes"), viewW.toFloat(), viewH.toFloat())
        GLES20.glUniform1f(GLES20.glGetUniformLocation(starProg, "uTime"), time)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(aPos)
    }

    private fun bindSphere(prog: Int): Pair<Int, Int> {
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, sphereVbo)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, sphereIbo)
        val aPos = GLES20.glGetAttribLocation(prog, "aPos")
        val aUv = GLES20.glGetAttribLocation(prog, "aUv")
        GLES20.glEnableVertexAttribArray(aPos)
        GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, 20, 0)
        if (aUv >= 0) {
            GLES20.glEnableVertexAttribArray(aUv)
            GLES20.glVertexAttribPointer(aUv, 2, GLES20.GL_FLOAT, false, 20, 12)
        }
        return aPos to aUv
    }

    private fun drawEarth(time: Float) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthMask(true)
        GLES20.glDisable(GLES20.GL_BLEND)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glCullFace(GLES20.GL_BACK)
        GLES20.glUseProgram(earthProg)
        val (aPos, aUv) = bindSphere(earthProg)
        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(earthProg, "uMVP"), 1, false, mvp, 0)
        GLES20.glUniform3f(GLES20.glGetUniformLocation(earthProg, "uSun"), sun[0], sun[1], sun[2])
        GLES20.glUniform3f(GLES20.glGetUniformLocation(earthProg, "uCam"), camObj[0], camObj[1], camObj[2])
        GLES20.glUniform1f(GLES20.glGetUniformLocation(earthProg, "uCloudShift"), (time * 0.0012f) % 1f)
        bindTex(earthProg, "uDay", texDay, 0)
        bindTex(earthProg, "uNight", texNight, 1)
        bindTex(earthProg, "uClouds", texClouds, 2)
        bindTex(earthProg, "uSpec", texSpec, 3)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, sphereIndexCount, GLES20.GL_UNSIGNED_SHORT, 0)
        GLES20.glDisableVertexAttribArray(aPos)
        if (aUv >= 0) GLES20.glDisableVertexAttribArray(aUv)
    }

    private fun drawAtmosphere() {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthMask(false)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glCullFace(GLES20.GL_FRONT)
        GLES20.glUseProgram(atmoProg)
        val (aPos, aUv) = bindSphere(atmoProg)
        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(atmoProg, "uMVP"), 1, false, mvp, 0)
        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(atmoProg, "uMV"), 1, false, mv, 0)
        GLES20.glUniform3f(GLES20.glGetUniformLocation(atmoProg, "uSun"), sun[0], sun[1], sun[2])
        GLES20.glUniform1f(GLES20.glGetUniformLocation(atmoProg, "uScale"), 1.13f)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, sphereIndexCount, GLES20.GL_UNSIGNED_SHORT, 0)
        GLES20.glDisableVertexAttribArray(aPos)
        if (aUv >= 0) GLES20.glDisableVertexAttribArray(aUv)
        GLES20.glDisable(GLES20.GL_CULL_FACE)
        GLES20.glDepthMask(true)
    }

    private fun drawMarkers(time: Float) {
        if (markersDirty) rebuildMarkers()
        val buf = markerBuffer ?: return
        if (markerCount == 0) return
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthMask(false)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glUseProgram(markerProg)
        val aPos = GLES20.glGetAttribLocation(markerProg, "aPos")
        val aColor = GLES20.glGetAttribLocation(markerProg, "aColor")
        val aSel = GLES20.glGetAttribLocation(markerProg, "aSel")
        buf.position(0)
        GLES20.glEnableVertexAttribArray(aPos)
        GLES20.glVertexAttribPointer(aPos, 3, GLES20.GL_FLOAT, false, 32, buf)
        buf.position(3)
        GLES20.glEnableVertexAttribArray(aColor)
        GLES20.glVertexAttribPointer(aColor, 4, GLES20.GL_FLOAT, false, 32, buf)
        buf.position(7)
        GLES20.glEnableVertexAttribArray(aSel)
        GLES20.glVertexAttribPointer(aSel, 1, GLES20.GL_FLOAT, false, 32, buf)
        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(markerProg, "uMVP"), 1, false, mvp, 0)
        GLES20.glUniform3f(GLES20.glGetUniformLocation(markerProg, "uCam"), camObj[0], camObj[1], camObj[2])
        GLES20.glUniform1f(GLES20.glGetUniformLocation(markerProg, "uTime"), time)
        val density = context.resources.displayMetrics.density
        // pin un po' più grandi quando si è vicini
        val zoomBoost = (defaultDist / dist).coerceIn(1f, 1.6f)
        GLES20.glUniform1f(GLES20.glGetUniformLocation(markerProg, "uPx"), 30f * density * zoomBoost)
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, markerCount)
        GLES20.glDisableVertexAttribArray(aPos)
        GLES20.glDisableVertexAttribArray(aColor)
        GLES20.glDisableVertexAttribArray(aSel)
        GLES20.glDepthMask(true)
    }

    private fun rebuildMarkers() {
        val list: List<GlobeMarker>
        val sel: Long?
        synchronized(lock) {
            list = markers
            sel = selectedId
            markersDirty = false
        }
        // i selezionati vanno disegnati per ultimi (sopra gli altri)
        val ordered = list.sortedBy { if (it.id == sel && it.kind != 2) 1 else 0 }
        val data = FloatArray(ordered.size * 8)
        ordered.forEachIndexed { i, m ->
            val p = latLonToXyz(m.lat, m.lon)
            val c = when (m.kind) {
                1 -> floatArrayOf(0.56f, 0.83f, 0.87f)   // in programma: azzurro cielo
                2 -> floatArrayOf(1f, 1f, 1f)            // nuovo punto
                else -> floatArrayOf(0.94f, 0.64f, 0.42f) // visitato: terracotta
            }
            val o = i * 8
            data[o] = p[0]; data[o + 1] = p[1]; data[o + 2] = p[2]
            data[o + 3] = c[0]; data[o + 4] = c[1]; data[o + 5] = c[2]
            data[o + 6] = ((m.id * 0.37) % 1.0).toFloat() // fase dell'onda
            data[o + 7] = if (m.id == sel || m.kind == 2) 1f else 0f
        }
        markerBuffer = floatBuffer(data)
        markerCount = ordered.size
    }

    // ------------------------------------------------------------ helper GL

    private fun buildSphere(stacks: Int, slices: Int) {
        val verts = FloatArray((stacks + 1) * (slices + 1) * 5)
        var k = 0
        for (i in 0..stacks) {
            val lat = 90.0 - 180.0 * i / stacks
            for (j in 0..slices) {
                val lon = -180.0 + 360.0 * j / slices
                val p = latLonToXyz(lat, lon)
                verts[k++] = p[0]; verts[k++] = p[1]; verts[k++] = p[2]
                verts[k++] = j.toFloat() / slices
                verts[k++] = i.toFloat() / stacks
            }
        }
        val idx = ShortArray(stacks * slices * 6)
        k = 0
        val row = slices + 1
        for (i in 0 until stacks) {
            for (j in 0 until slices) {
                val a = (i * row + j).toShort()
                val b = ((i + 1) * row + j).toShort()
                val c = (i * row + j + 1).toShort()
                val d = ((i + 1) * row + j + 1).toShort()
                idx[k++] = a; idx[k++] = b; idx[k++] = c
                idx[k++] = b; idx[k++] = d; idx[k++] = c
            }
        }
        sphereIndexCount = idx.size
        val ids = IntArray(2)
        GLES20.glGenBuffers(2, ids, 0)
        sphereVbo = ids[0]
        sphereIbo = ids[1]
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, sphereVbo)
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, verts.size * 4, floatBuffer(verts), GLES20.GL_STATIC_DRAW)
        val sb: ShortBuffer = ByteBuffer.allocateDirect(idx.size * 2).order(ByteOrder.nativeOrder()).asShortBuffer()
        sb.put(idx).position(0)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, sphereIbo)
        GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, idx.size * 2, sb, GLES20.GL_STATIC_DRAW)
    }

    private fun buildQuad() {
        val q = floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f)
        val ids = IntArray(1)
        GLES20.glGenBuffers(1, ids, 0)
        quadVbo = ids[0]
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, quadVbo)
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, q.size * 4, floatBuffer(q), GLES20.GL_STATIC_DRAW)
    }

    private fun loadTexture(asset: String, sample: Int, config: Bitmap.Config): Int {
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = config
            inScaled = false
        }
        val bmp = try {
            context.assets.open(asset).use { BitmapFactory.decodeStream(it, null, opts) }
        } catch (e: Throwable) {
            null
        }
        val ids = IntArray(1)
        GLES20.glGenTextures(1, ids, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, ids[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        val ext = GLES20.glGetString(GLES20.GL_EXTENSIONS) ?: ""
        if (ext.contains("GL_EXT_texture_filter_anisotropic")) {
            val maxAniso = FloatArray(1)
            GLES20.glGetFloatv(0x84FF, maxAniso, 0)
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, 0x84FE, min(8f, max(1f, maxAniso[0])))
        }
        if (bmp != null) {
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0)
            bmp.recycle()
        } else {
            // texture di riserva 1x1
            val px = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder())
            px.put(byteArrayOf(30, 60, 120, -1)).position(0)
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, 1, 1, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, px)
        }
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)
        return ids[0]
    }

    private fun bindTex(prog: Int, name: String, tex: Int, unit: Int) {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + unit)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(prog, name), unit)
    }

    private fun program(vs: String, fs: String): Int {
        val v = shader(GLES20.GL_VERTEX_SHADER, vs)
        val f = shader(GLES20.GL_FRAGMENT_SHADER, fs)
        val p = GLES20.glCreateProgram()
        GLES20.glAttachShader(p, v)
        GLES20.glAttachShader(p, f)
        GLES20.glLinkProgram(p)
        val ok = IntArray(1)
        GLES20.glGetProgramiv(p, GLES20.GL_LINK_STATUS, ok, 0)
        if (ok[0] == 0) android.util.Log.e("Globe", "link: " + GLES20.glGetProgramInfoLog(p))
        return p
    }

    private fun shader(type: Int, src: String): Int {
        val s = GLES20.glCreateShader(type)
        GLES20.glShaderSource(s, src)
        GLES20.glCompileShader(s)
        val ok = IntArray(1)
        GLES20.glGetShaderiv(s, GLES20.GL_COMPILE_STATUS, ok, 0)
        if (ok[0] == 0) android.util.Log.e("Globe", "shader: " + GLES20.glGetShaderInfoLog(s))
        return s
    }
}

// ---------------------------------------------------------------- utilità

/** lat/lon (gradi) -> punto sulla sfera unitaria. lon=0 guarda verso +z. */
fun latLonToXyz(lat: Double, lon: Double): FloatArray {
    val la = Math.toRadians(lat)
    val lo = Math.toRadians(lon)
    return floatArrayOf(
        (cos(la) * sin(lo)).toFloat(),
        sin(la).toFloat(),
        (cos(la) * cos(lo)).toFloat(),
    )
}

private fun floatBuffer(data: FloatArray): FloatBuffer =
    ByteBuffer.allocateDirect(data.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
        put(data)
        position(0)
    }

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
private fun easeInOut(t: Float) = if (t < 0.5f) 4 * t * t * t else 1 - (-2 * t + 2).let { it * it * it } / 2

// ---------------------------------------------------------------- shader

private const val PRECISION = """
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif
"""

private const val EARTH_VS = """
attribute vec3 aPos;
attribute vec2 aUv;
uniform mat4 uMVP;
varying vec3 vPos;
varying vec2 vUv;
void main() {
    vPos = aPos;
    vUv = aUv;
    gl_Position = uMVP * vec4(aPos, 1.0);
}
"""

private const val EARTH_FS = PRECISION + """
uniform sampler2D uDay;
uniform sampler2D uNight;
uniform sampler2D uClouds;
uniform sampler2D uSpec;
uniform vec3 uSun;
uniform vec3 uCam;
uniform float uCloudShift;
varying vec3 vPos;
varying vec2 vUv;
void main() {
    vec3 n = normalize(vPos);
    vec3 v = normalize(uCam - vPos);
    float ndl = dot(n, uSun);
    float dayMix = smoothstep(-0.12, 0.22, ndl);

    vec3 dayCol = texture2D(uDay, vUv).rgb;
    dayCol = pow(dayCol, vec3(0.95)) * 1.05;
    vec3 nightCol = texture2D(uNight, vUv).rgb;
    nightCol = pow(nightCol, vec3(1.4)) * vec3(1.6, 1.25, 0.8);

    float cloud = texture2D(uClouds, vec2(vUv.x + uCloudShift, vUv.y)).r;
    cloud = smoothstep(0.12, 0.95, cloud);
    // ombra delle nuvole leggermente spostata
    float cloudShadow = texture2D(uClouds, vec2(vUv.x + uCloudShift + 0.0015, vUv.y + 0.001)).r;

    float light = max(ndl, 0.0);
    vec3 lit = dayCol * (0.06 + 1.05 * light) * (1.0 - 0.35 * smoothstep(0.2, 1.0, cloudShadow));

    // riflesso del sole sugli oceani
    float water = texture2D(uSpec, vUv).r;
    vec3 h = normalize(uSun + v);
    float spec = pow(max(dot(n, h), 0.0), 90.0) * water * 0.75 * dayMix;
    float specWide = pow(max(dot(n, h), 0.0), 18.0) * water * 0.06 * dayMix;

    // lato notte: luci delle città + leggera "luce lunare" per vedere i continenti
    vec3 moon = dayCol * vec3(0.16, 0.19, 0.27);
    vec3 nightSide = moon + nightCol * (1.0 - cloud * 0.85);
    vec3 col = mix(nightSide, lit, dayMix);
    vec3 cloudCol = mix(vec3(0.12, 0.14, 0.2), vec3(1.0), smoothstep(-0.1, 0.4, ndl)) * (0.25 + 0.8 * light);
    col = mix(col, cloudCol, cloud * 0.92);
    col += (spec + specWide) * vec3(1.0, 0.93, 0.82) * (1.0 - cloud);

    // diffusione atmosferica ai bordi
    float fres = pow(1.0 - max(dot(n, v), 0.0), 3.0);
    float sunSide = 0.15 + 0.85 * smoothstep(-0.3, 0.6, ndl);
    col += vec3(0.30, 0.55, 1.0) * fres * 0.85 * sunSide;
    // tramonto: bordo del terminatore più caldo
    float dusk = smoothstep(-0.07, 0.0, ndl) * (1.0 - smoothstep(0.0, 0.08, ndl));
    col += vec3(0.30, 0.12, 0.03) * dusk * 0.22 * (1.0 - cloud * 0.5);

    gl_FragColor = vec4(col, 1.0);
}
"""

private const val ATMO_VS = """
attribute vec3 aPos;
uniform mat4 uMVP;
uniform mat4 uMV;
uniform float uScale;
varying vec3 vViewPos;
varying vec3 vViewN;
varying vec3 vObjN;
void main() {
    vec3 p = aPos * uScale;
    vObjN = aPos;
    vViewPos = (uMV * vec4(p, 1.0)).xyz;
    vViewN = (uMV * vec4(aPos, 0.0)).xyz;
    gl_Position = uMVP * vec4(p, 1.0);
}
"""

private const val ATMO_FS = PRECISION + """
uniform vec3 uSun;
varying vec3 vViewPos;
varying vec3 vViewN;
varying vec3 vObjN;
void main() {
    vec3 n = normalize(vViewN);
    vec3 v = normalize(-vViewPos);
    float d = dot(n, v);           // < 0 sulle facce posteriori
    float k = smoothstep(0.0, 0.55, -d);
    float intensity = pow(k, 2.2) * 1.15;
    float sunSide = 0.12 + 0.88 * smoothstep(-0.35, 0.55, dot(normalize(vObjN), uSun));
    vec3 col = mix(vec3(0.25, 0.45, 1.0), vec3(0.55, 0.78, 1.0), k) * intensity * sunSide;
    gl_FragColor = vec4(col, 1.0);
}
"""

private const val STAR_VS = """
attribute vec2 aPos;
void main() { gl_Position = vec4(aPos, 0.999, 1.0); }
"""

private const val STAR_FS = PRECISION + """
uniform vec2 uOffset;
uniform vec2 uRes;
uniform float uTime;
float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}
float stars(vec2 coord, float cellSize, float density) {
    vec2 p = coord / cellSize;
    vec2 cell = floor(p);
    vec2 f = fract(p);
    float h = hash(cell);
    if (h > density) return 0.0;
    vec2 sp = vec2(hash(cell + 3.1), hash(cell + 7.7)) * 0.8 + 0.1;
    float size = 0.06 + 0.12 * hash(cell + 11.3);
    float d = length(f - sp);
    float tw = 0.65 + 0.35 * sin(uTime * (1.0 + 2.0 * hash(cell + 5.0)) + h * 60.0);
    return smoothstep(size, 0.0, d) * tw * (0.5 + 0.5 * hash(cell + 2.0));
}
void main() {
    vec2 uv = gl_FragCoord.xy / uRes;
    vec3 bg = mix(vec3(0.004, 0.006, 0.018), vec3(0.025, 0.02, 0.06), uv.y);
    // velatura tipo via lattea
    float band = exp(-pow((uv.x - uv.y * 0.6 - 0.2) * 3.0, 2.0));
    bg += vec3(0.03, 0.025, 0.05) * band;
    vec2 c = gl_FragCoord.xy + uOffset;
    float s = stars(c, 7.0, 0.035) + stars(c * 1.7 + 50.0, 9.0, 0.02) * 0.7 + stars(c * 0.6, 14.0, 0.04) * 1.2;
    vec3 col = bg + vec3(0.85, 0.9, 1.0) * s;
    gl_FragColor = vec4(col, 1.0);
}
"""

private const val MARKER_VS = """
attribute vec3 aPos;
attribute vec4 aColor;
attribute float aSel;
uniform mat4 uMVP;
uniform vec3 uCam;
uniform float uPx;
varying vec4 vColor;
varying float vFade;
varying float vSel;
void main() {
    vec3 p = aPos * 1.008;
    gl_Position = uMVP * vec4(p, 1.0);
    float facing = dot(normalize(aPos), normalize(uCam - p));
    vFade = smoothstep(0.02, 0.3, facing);
    vColor = aColor;
    vSel = aSel;
    gl_PointSize = uPx * (1.0 + aSel * 0.7);
}
"""

private const val MARKER_FS = PRECISION + """
uniform float uTime;
varying vec4 vColor;
varying float vFade;
varying float vSel;
void main() {
    vec2 c = gl_PointCoord * 2.0 - 1.0;
    float r = length(c);
    if (r > 1.0) discard;
    float core = smoothstep(0.30, 0.20, r);
    float border = smoothstep(0.38, 0.30, r) - core;
    float glow = exp(-r * r * 6.0) * 0.55;
    float t = fract(uTime * 0.55 + vColor.a);
    float ring = smoothstep(0.09, 0.0, abs(r - (0.3 + t * 0.7))) * (1.0 - t);
    vec3 col = vColor.rgb;
    vec3 outc = col * glow + col * core + vec3(1.0) * border;
    float a = max(max(core + border, glow), ring * (0.7 + vSel * 0.3));
    outc = mix(outc, col, ring * (1.0 - core));
    gl_FragColor = vec4(outc, a * vFade);
}
"""
