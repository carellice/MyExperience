package com.myexperience.app.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.myexperience.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Una release pubblicata su GitHub con il suo APK. */
data class AppRelease(
    val version: String,
    val notes: String,
    val apkUrl: String,
    val apkSize: Long,
    val pageUrl: String,
)

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val release: AppRelease) : UpdateState
    data class Downloading(val release: AppRelease, val progress: Float) : UpdateState
    data class ReadyToInstall(val release: AppRelease, val apk: File) : UpdateState
    data class Failed(val message: String) : UpdateState
}

/**
 * Aggiornamento in-app tramite le GitHub Releases del repository pubblico:
 * legge l'ultima release, confronta la versione e scarica/installa l'APK allegato.
 */
object AppUpdater {
    const val REPO = "carellice/MyExperience"
    private const val LATEST_URL = "https://api.github.com/repos/$REPO/releases/latest"

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    val currentVersion: String get() = BuildConfig.VERSION_NAME

    /** Versione che l'utente ha scelto di ignorare nel controllo automatico. */
    private fun skipped(ctx: Context) =
        ctx.getSharedPreferences("updater", Context.MODE_PRIVATE).getString("skipped", null)

    fun skip(ctx: Context, version: String) {
        ctx.getSharedPreferences("updater", Context.MODE_PRIVATE).edit().putString("skipped", version).apply()
        _state.value = UpdateState.Idle
    }

    fun dismiss() {
        if (_state.value !is UpdateState.Downloading) _state.value = UpdateState.Idle
    }

    /** [manual] = avviato dall'utente: mostra anche "già aggiornata" ed errori, e ignora le versioni saltate. */
    suspend fun check(ctx: Context, manual: Boolean) {
        if (_state.value is UpdateState.Checking || _state.value is UpdateState.Downloading) return
        if (manual) _state.value = UpdateState.Checking
        val result = runCatching { fetchLatest() }
        val release = result.getOrNull()
        _state.value = when {
            result.isFailure -> if (manual) UpdateState.Failed("Impossibile controllare gli aggiornamenti") else UpdateState.Idle
            release != null && isNewer(release.version, currentVersion) &&
                (manual || release.version != skipped(ctx)) -> UpdateState.Available(release)
            manual -> UpdateState.UpToDate
            else -> UpdateState.Idle
        }
    }

    suspend fun download(ctx: Context, release: AppRelease) {
        _state.value = UpdateState.Downloading(release, 0f)
        val dir = File(ctx.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val out = File(dir, "MyExperience-${release.version}.apk")
        val ok = runCatching {
            withContext(Dispatchers.IO) {
                val conn = (URL(release.apkUrl).openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = true
                    connectTimeout = 15_000
                    readTimeout = 30_000
                }
                try {
                    if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode}")
                    val total = conn.contentLengthLong.takeIf { it > 0 } ?: release.apkSize
                    conn.inputStream.use { input ->
                        out.outputStream().use { output ->
                            val buf = ByteArray(64 * 1024)
                            var read = 0L
                            while (true) {
                                val n = input.read(buf)
                                if (n < 0) break
                                output.write(buf, 0, n)
                                read += n
                                if (total > 0) _state.value = UpdateState.Downloading(release, (read.toFloat() / total).coerceIn(0f, 1f))
                            }
                        }
                    }
                } finally {
                    conn.disconnect()
                }
            }
        }.isSuccess
        if (!ok) {
            out.delete()
            _state.value = UpdateState.Failed("Download non riuscito")
            return
        }
        _state.value = UpdateState.ReadyToInstall(release, out)
        install(ctx, out)
    }

    /** Su Android 8+ serve il permesso "installa app sconosciute" per questa app. */
    fun canInstall(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || ctx.packageManager.canRequestPackageInstalls()

    fun openInstallPermissionSettings(ctx: Context) {
        ctx.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun install(ctx: Context, apk: File) {
        if (!canInstall(ctx)) {
            openInstallPermissionSettings(ctx)
            return
        }
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.updates", apk)
        ctx.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private suspend fun fetchLatest(): AppRelease? = withContext(Dispatchers.IO) {
        val conn = (URL(LATEST_URL).openConnection() as HttpURLConnection).apply {
            setRequestProperty("Accept", "application/vnd.github+json")
            connectTimeout = 10_000
            readTimeout = 10_000
        }
        try {
            if (conn.responseCode == 404) return@withContext null // nessuna release pubblicata
            if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode}")
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val assets = json.getJSONArray("assets")
            val apk = (0 until assets.length()).map { assets.getJSONObject(it) }
                .firstOrNull { it.getString("name").endsWith(".apk", ignoreCase = true) }
                ?: return@withContext null
            AppRelease(
                version = json.getString("tag_name").removePrefix("v").removePrefix("V"),
                notes = json.optString("body").trim(),
                apkUrl = apk.getString("browser_download_url"),
                apkSize = apk.optLong("size"),
                pageUrl = json.optString("html_url"),
            )
        } finally {
            conn.disconnect()
        }
    }

    /** Confronto numerico "1.2.10" > "1.2.9"; parti mancanti valgono 0. */
    internal fun isNewer(remote: String, local: String): Boolean {
        fun parts(v: String) = v.split('.', '-').map { p -> p.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }
        val r = parts(remote)
        val l = parts(local)
        for (i in 0 until maxOf(r.size, l.size)) {
            val a = r.getOrElse(i) { 0 }
            val b = l.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }
}
