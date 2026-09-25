package com.philipcosgrave.calorietracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        FoodRecordEntity::class,
        LeftoverRecord::class,
        BarcodeAliasEntity::class,
        DiaryRecordEntity::class,
        SyncOutboxEntity::class,
        WeightRecordEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class CalorieTrackerDatabase : RoomDatabase() {
    abstract fun leftoverDao(): LeftoverDao
    abstract fun foodRecordDao(): FoodRecordDao
    abstract fun barcodeAliasDao(): BarcodeAliasDao
    abstract fun diaryRecordDao(): DiaryRecordDao
    abstract fun syncOutboxDao(): SyncOutboxDao
    abstract fun weightRecordDao(): WeightRecordDao
}
