package com.myexperience.app.data

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import java.util.UUID

class Repository(private val context: Context, db: AppDatabase) {
    private val placeDao = db.placeDao()
    private val tripDao = db.tripDao()
    private val prefs = context.getSharedPreferences("profile", Context.MODE_PRIVATE)

    private val mediaOrder = compareBy<AlbumMedia>({ it.addedAt }, { it.id })

    // ---------- Luoghi ----------
    val places: Flow<List<PlaceWithMedia>> = placeDao.observeAll().map { list ->
        list.map { it.copy(media = it.media.sortedWith(mediaOrder)) }
    }

    fun place(id: Long): Flow<PlaceWithMedia?> = placeDao.observe(id).map { p ->
        p?.copy(media = p.media.sortedWith(mediaOrder))
    }

    suspend fun getPlace(id: Long): Place? = placeDao.get(id)

    suspend fun savePlace(place: Place): Long {
        return if (place.id == 0L) {
            placeDao.insert(place)
        } else {
            val old = placeDao.get(place.id)
            placeDao.update(place)
            val oldCover = old?.coverPath
            if (oldCover != null && oldCover != place.coverPath) deleteIfUnused(place.id, oldCover)
            place.id
        }
    }

    suspend fun deletePlace(place: Place) = withContext(Dispatchers.IO) {
        val paths = placeDao.mediaFor(place.id).map { it.path }.toMutableSet()
        place.coverPath?.let { paths += it }
        placeDao.delete(place)
        paths.forEach { File(it).delete() }
    }

    suspend fun setFavorite(id: Long, favorite: Boolean) = placeDao.setFavorite(id, favorite)

    suspend fun setCover(id: Long, path: String) {
        val old = placeDao.get(id)?.coverPath
        placeDao.setCover(id, path)
        if (old != null && old != path) deleteIfUnused(id, old)
    }

    /** Importa i file nell'album e ritorna gli id dei nuovi media. */
    suspend fun addMedia(placeId: Long, uris: List<Uri>): List<Long> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val items = uris.mapIndexedNotNull { i, uri ->
            val stored = import(uri) ?: return@mapIndexedNotNull null
            AlbumMedia(placeId = placeId, path = stored.first, isVideo = stored.second, addedAt = now + i)
        }
        if (items.isEmpty()) return@withContext emptyList()
        val ids = placeDao.insertMedia(items)
        // Prima foto dell'album diventa copertina se manca
        val place = placeDao.get(placeId)
        if (place != null && place.coverPath == null) {
            items.firstOrNull { !it.isVideo }?.let { placeDao.setCover(placeId, it.path) }
        }
        ids
    }

    // ---------- Diario ----------
    fun blocks(placeId: Long): Flow<List<JournalBlock>> = placeDao.observeBlocks(placeId)

    suspend fun setTheme(placeId: Long, theme: String) = placeDao.setTheme(placeId, theme)

    /** Inserisce o aggiorna un blocco; per i nuovi [index] è la posizione (-1 = in fondo). */
    suspend fun saveBlock(block: JournalBlock, index: Int) {
        val list = placeDao.blocksFor(block.placeId).toMutableList()
        if (block.id != 0L) {
            placeDao.updateBlocks(listOf(block))
            return
        }
        val newId = placeDao.insertBlock(block)
        val inserted = block.copy(id = newId)
        val pos = if (index < 0 || index > list.size) list.size else index
        list.add(pos, inserted)
        placeDao.updateBlocks(list.mapIndexed { i, b -> b.copy(orderIndex = i) })
    }

    suspend fun moveBlock(block: JournalBlock, delta: Int) {
        val list = placeDao.blocksFor(block.placeId).toMutableList()
        val from = list.indexOfFirst { it.id == block.id }
        val to = from + delta
        if (from < 0 || to < 0 || to >= list.size) return
        val b = list.removeAt(from)
        list.add(to, b)
        placeDao.updateBlocks(list.mapIndexed { i, x -> x.copy(orderIndex = i) })
    }

    suspend fun duplicateBlock(block: JournalBlock) {
        val list = placeDao.blocksFor(block.placeId)
        val pos = list.indexOfFirst { it.id == block.id } + 1
        saveBlock(block.copy(id = 0), pos)
    }

    suspend fun deleteBlock(block: JournalBlock) = placeDao.deleteBlock(block)

    /** Crea una prima pagina di diario già impaginata, da personalizzare. */
    suspend fun seedTemplate(placeId: Long) {
        val place = placeDao.get(placeId) ?: return
        if (placeDao.blocksFor(placeId).isNotEmpty()) return
        val images = placeDao.mediaFor(placeId).filter { !it.isVideo }.sortedBy { it.addedAt }
        val blocks = mutableListOf<JournalBlock>()
        fun add(type: String, text: String = "", media: String = "", font: String = "default",
                size: Int = 1, align: String = "start", color: Int = 0, style: String = "") {
            blocks += JournalBlock(placeId = placeId, type = type, text = text, mediaIds = media,
                font = font, size = size, align = align, color = color, style = style)
        }
        add("STAMP", align = "end")
        add("HEADING", place.title, size = 3, align = "center")
        add("STICKER", listOf(place.mood, "✈️", "📍").joinToString(" "), size = 1, align = "center")
        add(
            "TEXT",
            place.description.ifBlank {
                "Scrivi qui il racconto di questa esperienza: cosa hai visto, chi hai incontrato, " +
                    "cosa ti porterai dietro. Tocca ✏️ Personalizza per modificare tutto."
            },
        )
        images.firstOrNull()?.let { add("PHOTO", place.locationName, it.id.toString(), style = "polaroid") }
        if (images.size > 1) {
            add("GALLERY", "", images.drop(1).take(4).joinToString(",") { it.id.toString() })
        }
        val highlights = place.highlights.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (highlights.isNotEmpty()) {
            add("HEADING", "Momenti top", size = 1, font = "cursive")
            add("LIST", highlights.joinToString("\n"))
        }
        add("NOTE", "Da non dimenticare…", font = "cursive", style = "yellow")
        add("DIVIDER", style = "plane")
        add("QUOTE", "Il mondo è un libro, e chi non viaggia ne legge solo una pagina.\n— Sant'Agostino", align = "center")
        blocks.forEachIndexed { i, b -> placeDao.insertBlock(b.copy(orderIndex = i)) }
    }

    suspend fun deleteMedia(item: AlbumMedia) = withContext(Dispatchers.IO) {
        placeDao.deleteMedia(item)
        val place = placeDao.get(item.placeId)
        if (place?.coverPath == item.path) {
            val next = placeDao.mediaFor(item.placeId).firstOrNull { !it.isVideo }
            placeDao.setCover(item.placeId, next?.path)
        }
        File(item.path).delete()
    }

    suspend fun importFile(uri: Uri): String? = withContext(Dispatchers.IO) { import(uri)?.first }

    private suspend fun deleteIfUnused(placeId: Long, path: String) = withContext(Dispatchers.IO) {
        val used = placeDao.mediaFor(placeId).any { it.path == path }
        if (!used) File(path).delete()
    }

    /** Copia il file scelto nella memoria privata dell'app. Ritorna (percorso, isVideo). */
    private fun import(uri: Uri): Pair<String, Boolean>? = try {
        val type = context.contentResolver.getType(uri) ?: ""
        val isVideo = type.startsWith("video")
        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(type)
            ?: if (isVideo) "mp4" else "jpg"
        val dir = File(context.filesDir, "media").apply { mkdirs() }
        val file = File(dir, "${UUID.randomUUID()}.$ext")
        val input = context.contentResolver.openInputStream(uri)
        if (input == null) {
            null
        } else {
            input.use { inp -> file.outputStream().use { inp.copyTo(it) } }
            file.absolutePath to isVideo
        }
    } catch (e: Exception) {
        null
    }

    // ---------- Viaggi ----------
    val trips: Flow<List<TripWithDetails>> = tripDao.observeAll().map { list ->
        list.map { t ->
            t.copy(
                destinations = t.destinations.sortedWith(compareBy({ it.orderIndex }, { it.id })),
                tasks = t.tasks.sortedBy { it.id },
            )
        }
    }

    fun trip(id: Long): Flow<TripWithDetails?> = tripDao.observe(id).map { t ->
        t?.copy(
            destinations = t.destinations.sortedWith(compareBy({ it.orderIndex }, { it.id })),
            tasks = t.tasks.sortedBy { it.id },
        )
    }

    suspend fun getTrip(id: Long) = tripDao.get(id)

    suspend fun saveTrip(trip: PlannedTrip): Long =
        if (trip.id == 0L) tripDao.insert(trip) else { tripDao.update(trip); trip.id }

    suspend fun deleteTrip(trip: PlannedTrip) = tripDao.delete(trip)

    suspend fun getDestination(id: Long) = tripDao.getDestination(id)

    suspend fun addDestination(tripId: Long, loc: PickedLocation) {
        val order = tripDao.maxOrder(tripId) + 1
        tripDao.insertDestination(
            TripDestination(
                tripId = tripId, name = loc.name, country = loc.country,
                latitude = loc.latitude, longitude = loc.longitude, orderIndex = order,
            )
        )
    }

    suspend fun updateDestination(d: TripDestination) = tripDao.updateDestination(d)
    suspend fun deleteDestination(d: TripDestination) = tripDao.deleteDestination(d)
    suspend fun addTask(tripId: Long, text: String) = tripDao.insertTask(TripTask(tripId = tripId, text = text))
    suspend fun updateTask(t: TripTask) = tripDao.updateTask(t)
    suspend fun deleteTask(t: TripTask) = tripDao.deleteTask(t)

    // ---------- Profilo ----------
    fun loadProfile() = Profile(
        name = prefs.getString("name", null) ?: "Viaggiatore",
        bio = prefs.getString("bio", null) ?: "Collezionista di tramonti e biglietti di sola andata ✈️",
        avatarPath = prefs.getString("avatar", null),
    )

    fun saveProfile(p: Profile) {
        prefs.edit()
            .putString("name", p.name)
            .putString("bio", p.bio)
            .putString("avatar", p.avatarPath)
            .apply()
    }

    // ---------- Geocoding ----------
    suspend fun searchLocation(query: String): List<PickedLocation> = withContext(Dispatchers.IO) {
        val fromGeocoder = try {
            if (Geocoder.isPresent()) {
                @Suppress("DEPRECATION")
                Geocoder(context, Locale.getDefault()).getFromLocationName(query, 6)
                    ?.map { it.toPicked() } ?: emptyList()
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        fromGeocoder.ifEmpty { nominatimSearch(query) }
            .distinctBy { "${it.name.lowercase()}|${it.country.lowercase()}" }
    }

    suspend fun reverse(lat: Double, lon: Double): PickedLocation = withContext(Dispatchers.IO) {
        val fromGeocoder = try {
            if (Geocoder.isPresent()) {
                @Suppress("DEPRECATION")
                Geocoder(context, Locale.getDefault()).getFromLocation(lat, lon, 1)
                    ?.firstOrNull()?.toPicked()?.copy(latitude = lat, longitude = lon)
            } else null
        } catch (e: Exception) {
            null
        }
        fromGeocoder?.takeIf { it.name.isNotBlank() } ?: nominatimReverse(lat, lon)
        ?: PickedLocation(lat, lon, String.format(Locale.US, "%.2f°, %.2f°", lat, lon), "")
    }

    private fun Address.toPicked() = PickedLocation(
        latitude = latitude,
        longitude = longitude,
        name = locality ?: subAdminArea ?: adminArea ?: featureName ?: countryName ?: "Luogo",
        country = countryName ?: "",
    )

    private fun httpGet(url: String): String? = try {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", "MyExperience-Android/1.0")
        conn.setRequestProperty("Accept-Language", Locale.getDefault().language)
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.inputStream.bufferedReader().use { it.readText() }
    } catch (e: Exception) {
        null
    }

    private fun nominatimSearch(q: String): List<PickedLocation> {
        val body = httpGet(
            "https://nominatim.openstreetmap.org/search?format=json&addressdetails=1&limit=6&q=" +
                URLEncoder.encode(q, "UTF-8")
        ) ?: return emptyList()
        return try {
            val arr = JSONArray(body)
            (0 until arr.length()).map { i -> parseNominatim(arr.getJSONObject(i)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun nominatimReverse(lat: Double, lon: Double): PickedLocation? {
        val body = httpGet(
            "https://nominatim.openstreetmap.org/reverse?format=json&zoom=10&lat=$lat&lon=$lon"
        ) ?: return null
        return try {
            val o = JSONObject(body)
            if (o.has("error")) null
            else parseNominatim(o).copy(latitude = lat, longitude = lon).takeIf { it.name.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseNominatim(o: JSONObject): PickedLocation {
        val addr = o.optJSONObject("address")
        val name = addr?.let {
            listOf("city", "town", "village", "municipality", "county", "state")
                .firstNotNullOfOrNull { k -> it.optString(k).takeIf { s -> s.isNotBlank() } }
        } ?: o.optString("name").ifBlank { o.optString("display_name").substringBefore(",") }
        return PickedLocation(
            latitude = o.optString("lat").toDoubleOrNull() ?: 0.0,
            longitude = o.optString("lon").toDoubleOrNull() ?: 0.0,
            name = name,
            country = addr?.optString("country") ?: "",
        )
    }
}
