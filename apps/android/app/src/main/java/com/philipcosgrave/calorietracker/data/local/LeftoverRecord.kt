package com.philipcosgrave.calorietracker.data.local

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.philipcosgrave.calorietracker.domain.Leftover
import org.json.JSONArray

@Entity(tableName = "leftovers")
data class LeftoverRecord(@PrimaryKey val id: String, val ownerUserId: String, val name: String, val entriesJson: String)

fun Leftover.toRecord(owner: String) = LeftoverRecord(id, owner, name, JSONArray(entries.map { it.toJsonString() }).toString())
fun LeftoverRecord.toLeftover(): Leftover {
    val array = JSONArray(entriesJson)
    return Leftover(id, name, List(array.length()) { diaryEntryFromJsonString(array.getString(it)) })
}

@Dao
interface LeftoverDao {
    @Query("SELECT * FROM leftovers WHERE ownerUserId = :owner ORDER BY rowid DESC")
    suspend fun list(owner: String): List<LeftoverRecord>
    @Query("SELECT * FROM leftovers WHERE id = :id AND ownerUserId = :owner")
    suspend fun get(id: String, owner: String): LeftoverRecord?
    @Insert suspend fun insert(record: LeftoverRecord)
    @Query("DELETE FROM leftovers WHERE id = :id AND ownerUserId = :owner")
    suspend fun delete(id: String, owner: String): Int
}

val LeftoverMigration = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `leftovers` (`id` TEXT NOT NULL, `ownerUserId` TEXT NOT NULL, `name` TEXT NOT NULL, `entriesJson` TEXT NOT NULL, PRIMARY KEY(`id`))")
    }
}
