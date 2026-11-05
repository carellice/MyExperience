package com.myexperience.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaceDao {
    @Transaction
    @Query("SELECT * FROM places ORDER BY startDate DESC, id DESC")
    fun observeAll(): Flow<List<PlaceWithMedia>>

    @Transaction
    @Query("SELECT * FROM places WHERE id = :id")
    fun observe(id: Long): Flow<PlaceWithMedia?>

    @Query("SELECT * FROM places WHERE id = :id")
    suspend fun get(id: Long): Place?

    @Insert
    suspend fun insert(place: Place): Long

    @Update
    suspend fun update(place: Place)

    @Delete
    suspend fun delete(place: Place)

    @Query("UPDATE places SET favorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("UPDATE places SET coverPath = :path WHERE id = :id")
    suspend fun setCover(id: Long, path: String?)

    @Query("UPDATE places SET theme = :theme WHERE id = :id")
    suspend fun setTheme(id: Long, theme: String)

    @Insert
    suspend fun insertMedia(items: List<AlbumMedia>): List<Long>

    @Delete
    suspend fun deleteMedia(item: AlbumMedia)

    @Query("SELECT * FROM album_media WHERE placeId = :placeId")
    suspend fun mediaFor(placeId: Long): List<AlbumMedia>

    // ---- diario ----
    @Query("SELECT * FROM journal_blocks WHERE placeId = :placeId ORDER BY orderIndex, id")
    fun observeBlocks(placeId: Long): Flow<List<JournalBlock>>

    @Query("SELECT * FROM journal_blocks WHERE placeId = :placeId ORDER BY orderIndex, id")
    suspend fun blocksFor(placeId: Long): List<JournalBlock>

    @Insert
    suspend fun insertBlock(block: JournalBlock): Long

    @Update
    suspend fun updateBlocks(blocks: List<JournalBlock>)

    @Delete
    suspend fun deleteBlock(block: JournalBlock)
}

@Dao
interface TripDao {
    @Transaction
    @Query("SELECT * FROM trips ORDER BY CASE WHEN startDate IS NULL THEN 1 ELSE 0 END, startDate ASC, id DESC")
    fun observeAll(): Flow<List<TripWithDetails>>

    @Transaction
    @Query("SELECT * FROM trips WHERE id = :id")
    fun observe(id: Long): Flow<TripWithDetails?>

    @Query("SELECT * FROM trips WHERE id = :id")
    suspend fun get(id: Long): PlannedTrip?

    @Insert
    suspend fun insert(trip: PlannedTrip): Long

    @Update
    suspend fun update(trip: PlannedTrip)

    @Delete
    suspend fun delete(trip: PlannedTrip)

    @Query("SELECT * FROM trip_destinations WHERE id = :id")
    suspend fun getDestination(id: Long): TripDestination?

    @Query("SELECT COALESCE(MAX(orderIndex), -1) FROM trip_destinations WHERE tripId = :tripId")
    suspend fun maxOrder(tripId: Long): Int

    @Insert
    suspend fun insertDestination(destination: TripDestination): Long

    @Update
    suspend fun updateDestination(destination: TripDestination)

    @Delete
    suspend fun deleteDestination(destination: TripDestination)

    @Insert
    suspend fun insertTask(task: TripTask): Long

    @Update
    suspend fun updateTask(task: TripTask)

    @Delete
    suspend fun deleteTask(task: TripTask)
}

/** v1 -> v2: tema pagina + blocchi del diario. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `places` ADD COLUMN `theme` TEXT NOT NULL DEFAULT 'paper'")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `journal_blocks` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`placeId` INTEGER NOT NULL, " +
                "`orderIndex` INTEGER NOT NULL, " +
                "`type` TEXT NOT NULL, " +
                "`text` TEXT NOT NULL, " +
                "`mediaIds` TEXT NOT NULL, " +
                "`font` TEXT NOT NULL, " +
                "`size` INTEGER NOT NULL, " +
                "`align` TEXT NOT NULL, " +
                "`color` INTEGER NOT NULL, " +
                "`style` TEXT NOT NULL, " +
                "FOREIGN KEY(`placeId`) REFERENCES `places`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_journal_blocks_placeId` ON `journal_blocks` (`placeId`)")
    }
}

@Database(
    entities = [
        Place::class, AlbumMedia::class, PlannedTrip::class, TripDestination::class,
        TripTask::class, JournalBlock::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun placeDao(): PlaceDao
    abstract fun tripDao(): TripDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "myexperience.db")
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
