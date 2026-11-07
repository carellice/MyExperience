package com.myexperience.app.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/** Un luogo visitato / esperienza vissuta, con la sua scheda descrittiva. */
@Entity(tableName = "places")
data class Place(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val locationName: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val startDate: Long,
    val endDate: Long,
    val description: String = "",
    val highlights: String = "",
    val companions: String = "",
    val rating: Int = 4,
    val mood: String = "😍",
    val category: String = "Città",
    val budget: String = "",
    val coverPath: String? = null,
    val favorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    /** Tema grafico della pagina diario. */
    val theme: String = "paper",
)

/**
 * Elemento libero del diario di un album (titolo, testo, foto, sticker...).
 * [type]: HEADING, TEXT, QUOTE, NOTE, LIST, PHOTO, GALLERY, STICKER, STAMP, DIVIDER
 */
@Entity(
    tableName = "journal_blocks",
    foreignKeys = [ForeignKey(
        entity = Place::class,
        parentColumns = ["id"],
        childColumns = ["placeId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("placeId")],
)
data class JournalBlock(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val placeId: Long,
    val orderIndex: Int = 0,
    val type: String,
    val text: String = "",
    /** id dei media dell'album, separati da virgola */
    val mediaIds: String = "",
    /** default | sans | serif | cursive | mono */
    val font: String = "default",
    /** 0 = S, 1 = M, 2 = L, 3 = XL */
    val size: Int = 1,
    /** start | center | end */
    val align: String = "start",
    /** indice palette colori, 0 = inchiostro del tema */
    val color: Int = 0,
    /** variante grafica (polaroid/full, colore nota, stile separatore...) */
    val style: String = "",
) {
    val mediaIdList: List<Long> get() = mediaIds.split(',').mapNotNull { it.trim().toLongOrNull() }
}

/**
 * Tappa di un album: un album "on the road" ne ha più di una.
 * La prima tappa coincide con il luogo principale dell'album ([Place.latitude]/[Place.longitude]).
 */
@Entity(
    tableName = "place_stops",
    foreignKeys = [ForeignKey(
        entity = Place::class,
        parentColumns = ["id"],
        childColumns = ["placeId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("placeId")],
)
data class PlaceStop(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val placeId: Long,
    val name: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val orderIndex: Int = 0,
    val nights: Int = 0,
    val notes: String = "",
)

/** Foto o video dell'album di un luogo. */
@Entity(
    tableName = "album_media",
    foreignKeys = [ForeignKey(
        entity = Place::class,
        parentColumns = ["id"],
        childColumns = ["placeId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("placeId")],
)
data class AlbumMedia(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val placeId: Long,
    val path: String,
    val isVideo: Boolean,
    val addedAt: Long = System.currentTimeMillis(),
)

/** Viaggio da organizzare. */
@Entity(tableName = "trips")
data class PlannedTrip(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val budget: String = "",
    val transport: String = "",
    val accommodation: String = "",
    val notes: String = "",
    val colorIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "trip_destinations",
    foreignKeys = [ForeignKey(
        entity = PlannedTrip::class,
        parentColumns = ["id"],
        childColumns = ["tripId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("tripId")],
)
data class TripDestination(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val name: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val notes: String = "",
    val nights: Int = 1,
    val orderIndex: Int = 0,
    val visited: Boolean = false,
)

@Entity(
    tableName = "trip_tasks",
    foreignKeys = [ForeignKey(
        entity = PlannedTrip::class,
        parentColumns = ["id"],
        childColumns = ["tripId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("tripId")],
)
data class TripTask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tripId: Long,
    val text: String,
    val done: Boolean = false,
)

data class PlaceWithMedia(
    @Embedded val place: Place,
    @Relation(parentColumn = "id", entityColumn = "placeId")
    val media: List<AlbumMedia>,
    @Relation(parentColumn = "id", entityColumn = "placeId")
    val stops: List<PlaceStop> = emptyList(),
) {
    /** Tappe in ordine; un album senza tappe salvate ha come unica tappa il suo luogo. */
    val route: List<PlaceStop>
        get() = stops.ifEmpty {
            listOf(PlaceStop(placeId = place.id, name = place.locationName, country = place.country,
                latitude = place.latitude, longitude = place.longitude))
        }

    val countries: List<String> get() = route.map { it.country }.filter { it.isNotBlank() }.distinct()
}

data class TripWithDetails(
    @Embedded val trip: PlannedTrip,
    @Relation(parentColumn = "id", entityColumn = "tripId")
    val destinations: List<TripDestination>,
    @Relation(parentColumn = "id", entityColumn = "tripId")
    val tasks: List<TripTask>,
)

/** Luogo scelto sulla mappa / dalla ricerca. */
data class PickedLocation(
    val latitude: Double,
    val longitude: Double,
    val name: String,
    val country: String,
)

data class Profile(
    val name: String,
    val bio: String,
    val avatarPath: String?,
    val coverPath: String? = null,
)
