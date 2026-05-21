package com.philipcosgrave.calorietracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        FoodRecordEntity::class,
        BarcodeAliasEntity::class,
        DiaryRecordEntity::class,
        SyncOutboxEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class CalorieTrackerDatabase : RoomDatabase() {
    abstract fun foodRecordDao(): FoodRecordDao
    abstract fun barcodeAliasDao(): BarcodeAliasDao
    abstract fun diaryRecordDao(): DiaryRecordDao
    abstract fun syncOutboxDao(): SyncOutboxDao
}
