package com.philipcosgrave.calorietracker.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FoodRecordDao {
    @Query("SELECT * FROM food_records ORDER BY updatedAt DESC")
    suspend fun listAll(): List<FoodRecordEntity>

    @Query("SELECT * FROM food_records WHERE recordId = :recordId LIMIT 1")
    suspend fun getById(recordId: String): FoodRecordEntity?

    @Query("SELECT * FROM food_records WHERE barcode = :barcode LIMIT 1")
    suspend fun getByBarcode(barcode: String): FoodRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: FoodRecordEntity)
}

@Dao
interface BarcodeAliasDao {
    @Query("SELECT * FROM barcode_alias_records ORDER BY updatedAt DESC")
    suspend fun listAll(): List<BarcodeAliasEntity>

    @Query("SELECT * FROM barcode_alias_records WHERE barcode = :barcode LIMIT 1")
    suspend fun getByBarcode(barcode: String): BarcodeAliasEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: BarcodeAliasEntity)
}

@Dao
interface DiaryRecordDao {
    @Query("SELECT * FROM diary_records ORDER BY loggedAt DESC")
    suspend fun listAll(): List<DiaryRecordEntity>

    @Query("SELECT * FROM diary_records WHERE recordId = :recordId LIMIT 1")
    suspend fun getById(recordId: String): DiaryRecordEntity?

    @Query("SELECT * FROM diary_records WHERE loggedAt >= :startLoggedAt AND loggedAt <= :endLoggedAt ORDER BY loggedAt DESC")
    suspend fun listByDateRange(startLoggedAt: String, endLoggedAt: String): List<DiaryRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DiaryRecordEntity)
}

@Dao
interface SyncOutboxDao {
    @Query("SELECT * FROM sync_outbox ORDER BY changedAt ASC")
    suspend fun listAll(): List<SyncOutboxEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SyncOutboxEntity)

    @Query("DELETE FROM sync_outbox WHERE changeId IN (:changeIds)")
    suspend fun deleteByIds(changeIds: List<String>)

    @Query("DELETE FROM sync_outbox")
    suspend fun clear()
}
