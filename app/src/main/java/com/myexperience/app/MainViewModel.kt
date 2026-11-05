package com.myexperience.app

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.myexperience.app.data.AlbumMedia
import com.myexperience.app.data.JournalBlock
import com.myexperience.app.data.PickedLocation
import com.myexperience.app.data.Place
import com.myexperience.app.data.PlaceStop
import com.myexperience.app.data.PlaceWithMedia
import com.myexperience.app.data.PlannedTrip
import com.myexperience.app.data.Profile
import com.myexperience.app.data.TripDestination
import com.myexperience.app.data.TripTask
import com.myexperience.app.data.TripWithDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as MyExperienceApp).repository

    val places: StateFlow<List<PlaceWithMedia>?> =
        repo.places.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val trips: StateFlow<List<TripWithDetails>?> =
        repo.trips.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _profile = MutableStateFlow(repo.loadProfile())
    val profile: StateFlow<Profile> = _profile.asStateFlow()

    private val _picked = MutableStateFlow<PickedLocation?>(null)
    val pickedLocation: StateFlow<PickedLocation?> = _picked.asStateFlow()

    private val _importing = MutableStateFlow(false)
    val importing: StateFlow<Boolean> = _importing.asStateFlow()

    init {
        viewModelScope.launch {
            repo.addMissingRoutes()
            repo.convertEmojiStickers()
        }
    }

    fun place(id: Long): Flow<PlaceWithMedia?> = repo.place(id)
    fun trip(id: Long): Flow<TripWithDetails?> = repo.trip(id)

    suspend fun getPlace(id: Long) = repo.getPlace(id)
    suspend fun getTrip(id: Long) = repo.getTrip(id)
    suspend fun getDestination(id: Long) = repo.getDestination(id)

    // ---- luoghi ----
    fun toggleFavorite(p: Place) = viewModelScope.launch { repo.setFavorite(p.id, !p.favorite) }

    suspend fun getStops(placeId: Long) = repo.getStops(placeId)

    fun savePlace(place: Place, stops: List<PlaceStop>, newCover: Uri?, onDone: (Long) -> Unit) = viewModelScope.launch {
        var p = place
        if (newCover != null) {
            repo.importFile(newCover)?.let { p = p.copy(coverPath = it) }
        }
        val isNew = place.id == 0L
        val id = repo.savePlace(p, stops)
        if (isNew) repo.seedTemplate(id)
        onDone(id)
    }

    // ---- diario ----
    fun blocks(placeId: Long): Flow<List<JournalBlock>> = repo.blocks(placeId)
    fun saveBlock(block: JournalBlock, index: Int = -1) = viewModelScope.launch { repo.saveBlock(block, index) }
    fun moveBlock(block: JournalBlock, delta: Int) = viewModelScope.launch { repo.moveBlock(block, delta) }
    fun duplicateBlock(block: JournalBlock) = viewModelScope.launch { repo.duplicateBlock(block) }
    fun deleteBlock(block: JournalBlock) = viewModelScope.launch { repo.deleteBlock(block) }
    fun setTheme(placeId: Long, theme: String) = viewModelScope.launch { repo.setTheme(placeId, theme) }
    fun seedTemplate(placeId: Long) = viewModelScope.launch { repo.seedTemplate(placeId) }

    /** Importa media e restituisce gli id creati (per inserirli subito in un blocco). */
    fun importMedia(placeId: Long, uris: List<Uri>, onDone: (List<Long>) -> Unit) = viewModelScope.launch {
        _importing.value = true
        try {
            onDone(repo.addMedia(placeId, uris))
        } finally {
            _importing.value = false
        }
    }

    // ---- globo ----
    /** Orientamento del globo conservato tra una visita e l'altra della Home. */
    val globeCamera = floatArrayOf(12.5f, 30f, 0f) // yaw(lon), pitch(lat), distanza (0 = default)

    fun deletePlace(p: Place) = viewModelScope.launch { repo.deletePlace(p) }

    fun addMedia(placeId: Long, uris: List<Uri>) = viewModelScope.launch {
        _importing.value = true
        try {
            repo.addMedia(placeId, uris)
        } finally {
            _importing.value = false
        }
    }

    fun deleteMedia(item: AlbumMedia) = viewModelScope.launch { repo.deleteMedia(item) }
    fun setCover(placeId: Long, path: String) = viewModelScope.launch { repo.setCover(placeId, path) }

    fun toggleVisited(d: TripDestination) = viewModelScope.launch { repo.updateDestination(d.copy(visited = !d.visited)) }

    /** Chiude il viaggio trasformandolo in album; [onDone] riceve l'id dell'album (null se senza mete). */
    fun completeTrip(tripId: Long, onDone: (Long?) -> Unit) = viewModelScope.launch {
        onDone(repo.tripToAlbum(tripId))
    }

    /** Controlla i viaggi conclusi e li trasforma in album (all'avvio e al ritorno nell'app). */
    fun completeEndedTrips() = viewModelScope.launch { repo.completeEndedTrips() }

    // ---- posizione ----
    fun setPicked(loc: PickedLocation) { _picked.value = loc }
    fun consumePicked() { _picked.value = null }
    suspend fun searchLocation(q: String) = repo.searchLocation(q)
    suspend fun reverse(lat: Double, lon: Double) = repo.reverse(lat, lon)

    // ---- viaggi ----
    fun saveTrip(trip: PlannedTrip, onDone: (Long) -> Unit) = viewModelScope.launch {
        onDone(repo.saveTrip(trip))
    }

    fun deleteTrip(trip: PlannedTrip) = viewModelScope.launch { repo.deleteTrip(trip) }
    fun addDestination(tripId: Long, loc: PickedLocation) = viewModelScope.launch { repo.addDestination(tripId, loc) }
    fun updateDestination(d: TripDestination) = viewModelScope.launch { repo.updateDestination(d) }
    fun deleteDestination(d: TripDestination) = viewModelScope.launch { repo.deleteDestination(d) }
    fun addTask(tripId: Long, text: String) = viewModelScope.launch { repo.addTask(tripId, text) }
    fun toggleTask(t: TripTask) = viewModelScope.launch { repo.updateTask(t.copy(done = !t.done)) }
    fun deleteTask(t: TripTask) = viewModelScope.launch { repo.deleteTask(t) }

    // ---- profilo ----
    fun updateProfile(name: String, bio: String) {
        val p = _profile.value.copy(name = name.ifBlank { "Viaggiatore" }, bio = bio)
        _profile.value = p
        repo.saveProfile(p)
    }

    fun setAvatar(uri: Uri) = viewModelScope.launch {
        val path = repo.importFile(uri) ?: return@launch
        val old = _profile.value.avatarPath
        val p = _profile.value.copy(avatarPath = path)
        _profile.value = p
        repo.saveProfile(p)
        if (old != null) java.io.File(old).delete()
    }
}
