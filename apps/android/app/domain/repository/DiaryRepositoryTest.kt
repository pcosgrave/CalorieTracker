package com.philipcosgrave.calorietracker.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.philipcosgrave.calorietracker.data.local.CalorieTrackerDatabase
import com.philipcosgrave.calorietracker.data.local.DiaryRecordDao
import com.philipcosgrave.calorietracker.domain.createDiaryRecord
import com.philipcosgrave.calorietracker.domain.nowIsoString
import com.philipcosgrave.calorietracker.model.*
import org.junit.Assert.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class DiaryRepositoryTest {

    private lateinit var database: CalorieTrackerDatabase
    private lateinit var dao: DiaryRecordDao
    private lateinit var repository: RoomDiaryRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, CalorieTrackerDatabase::class.java)
            .allowMainThreadOperations()
            .build()
        dao = database.diaryRecordDao()
        repository = RoomDiaryRepository(dao)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun test_list_returns_all_diary_entries() = runBlocking {
        val entry1 = DiaryEntry(
            id = "entry-1",
            food = FoodItem(
                id = "food-1",
                kind = FoodKind.Ingredient,
                name = "Oatmeal",
                servingQuantity = 100.0,
                servingUnit = "gram",
                nutrients = Nutrients(150.0)
            ),
            date = LocalDate.now().minusDays(2),
            meal = Meal.Breakfast,
            servingMultiplier = 1.5
        )
        val entry2 = DiaryEntry(
            id = "entry-2",
            food = FoodItem(
                id = "food-2",
                kind = FoodKind.Ingredient,
                name = "Chicken Salad",
                servingQuantity = 200.0,
                servingUnit = "gram",
                nutrients = Nutrients(350.0)
            ),
            date = LocalDate.now(),
            meal = Meal.Lunch,
            servingMultiplier = 1.0
        )

        repository.save(createDiaryRecord(entry1, "device-1"))
        repository.save(createDiaryRecord(entry2, "device-1"))

        val list = repository.list()
        assertEquals(2, list.size)
        assertTrue(list.any { it.entry.id == "entry-1" })
        assertTrue(list.any { it.entry.id == "entry-2" })
    }

    @Test
    fun test_list_by_date_range() = runBlocking {
        val entry1 = DiaryEntry(
            id = "entry-1",
            food = FoodItem(
                id = "food-1",
                kind = FoodKind.Ingredient,
                name = "Breakfast Food",
                servingQuantity = 100.0,
                servingUnit = "gram",
                nutrients = Nutrients(150.0)
            ),
            date = LocalDate.now().minusDays(1),
            meal = Meal.Breakfast,
            servingMultiplier = 1.0
        )
        val entry2 = DiaryEntry(
            id = "entry-2",
            food = FoodItem(
                id = "food-2",
                kind = FoodKind.Ingredient,
                name = "Lunch Food",
                servingQuantity = 200.0,
                servingUnit = "gram",
                nutrients = Nutrients(350.0)
            ),
            date = LocalDate.now(),
            meal = Meal.Lunch,
            servingMultiplier = 1.0
        )
        val entry3 = DiaryEntry(
            id = "entry-3",
            food = FoodItem(
                id = "food-3",
                kind = FoodKind.Ingredient,
                name = "Previous Day Food",
                servingQuantity = 100.0,
                servingUnit = "gram",
                nutrients = Nutrients(150.0)
            ),
            date = LocalDate.now().minusDays(5),
            meal = Meal.Dinner,
            servingMultiplier = 1.0
        )

        repository.save(createDiaryRecord(entry1, "device-1"))
        repository.save(createDiaryRecord(entry2, "device-1"))
        repository.save(createDiaryRecord(entry3, "device-1"))

        // Get entries from yesterday to today
        val list = repository.listByDateRange(
            LocalDate.now().minusDays(1),
            LocalDate.now()
        )
        assertEquals(2, list.size)
        assertTrue(list.any { it.entry.date == LocalDate.now().minusDays(1) })
        assertTrue(list.any { it.entry.date == LocalDate.now() })

        // Get entries from week ago to yesterday
        val previousWeek = repository.listByDateRange(
            LocalDate.now().minusDays(10),
            LocalDate.now().minusDays(1)
        )
        assertEquals(1, previousWeek.size)
        assertEquals(LocalDate.now().minusDays(5), previousWeek[0].entry.date)
    }

    @Test
    fun test_list_by_date_range_empty_range() = runBlocking {
        val list = repository.listByDateRange(
            LocalDate.now().minusDays(1),
            LocalDate.now().minusDays(5)
        )
        assertEquals(0, list.size)
    }

    @Test
    fun test_getById_returns_diary_entry() = runBlocking {
        val entry = DiaryEntry(
            id = "test-entry",
            food = FoodItem(
                id = "food-test",
                kind = FoodKind.Ingredient,
                name = "Test Entry",
                servingQuantity = 100.0,
                servingUnit = "gram",
                nutrients = Nutrients(100.0)
            ),
            date = LocalDate.now(),
            meal = Meal.Breakfast,
            servingMultiplier = 1.0
        )

        repository.save(createDiaryRecord(entry, "device-1"))

        val record = repository.getById("test-entry")
        assertNotNull(record)
        assertEquals("Test Entry", record?.entry.food.name)
        assertEquals(Meal.Breakfast, record?.entry.meal)
    }

    @Test
    fun test_getById_returns_null_for_nonexistent() = runBlocking {
        val record = repository.getById("nonexistent")
        assertNull(record)
    }

    @Test
    fun test_save_upserts_diary_record() = runBlocking {
        val entry1 = DiaryEntry(
            id = "upsert-entry",
            food = FoodItem(
                id = "food-upsert",
                kind = FoodKind.Ingredient,
                name = "Original Name",
                servingQuantity = 100.0,
                servingUnit = "gram",
                nutrients = Nutrients(100.0)
            ),
            date = LocalDate.now(),
            meal = Meal.Breakfast,
            servingMultiplier = 1.0
        )

        repository.save(createDiaryRecord(entry1, "device-1"))
        val list1 = repository.list()
        assertEquals(1, list1.size)
        assertEquals("Original Name", list1[0].entry.food.name)

        // Upsert with updated name
        val entry2 = DiaryEntry(
            id = "upsert-entry",
            food = FoodItem(
                id = "food-upsert",
                kind = FoodKind.Ingredient,
                name = "Updated Name",
                servingQuantity = 150.0,
                servingUnit = "gram",
                nutrients = Nutrients(150.0)
            ),
            date = LocalDate.now(),
            meal = Meal.Breakfast,
            servingMultiplier = 1.0
        )

        repository.save(createDiaryRecord(entry2, "device-1"))
        val list2 = repository.list()
        assertEquals(1, list2.size)
        assertEquals("Updated Name", list2[0].entry.food.name)
        assertEquals(2, list2[0].sync.version)
    }

    @Test
    fun test_soft_delete_marks_record_as_deleted() = runBlocking {
        val entry = DiaryEntry(
            id = "delete-entry",
            food = FoodItem(
                id = "food-delete",
                kind = FoodKind.Ingredient,
                name = "To Be Deleted",
                servingQuantity = 100.0,
                servingUnit = "gram",
                nutrients = Nutrients(100.0)
            ),
            date = LocalDate.now(),
            meal = Meal.Lunch,
            servingMultiplier = 1.0
        )

        repository.save(createDiaryRecord(entry, "device-1"))
        val listBefore = repository.list()
        assertEquals(1, listBefore.size)

        repository.softDelete("delete-entry", "2024-01-02T00:00:00Z")
        val listAfter = repository.list()
        assertEquals(1, listAfter.size)
        assertEquals("To Be Deleted", listAfter[0].entry.food.name)
        assertNotNull(listAfter[0].sync.deletedAt)
        assertEquals(SyncStatus.LocalOnly, listAfter[0].sync.syncStatus)
    }

    @Test
    fun test_soft_delete_no_op_for_nonexistent() = runBlocking {
        repository.softDelete("nonexistent", "2024-01-02T00:00:00Z")
        // No exception should be thrown
    }

    @Test
    fun test_save_preserves_sync_metadata() = runBlocking {
        val entry = DiaryEntry(
            id = "metadata-entry",
            food = FoodItem(
                id = "food-metadata",
                kind = FoodKind.Ingredient,
                name = "Metadata Test",
                servingQuantity = 100.0,
                servingUnit = "gram",
                nutrients = Nutrients(100.0)
            ),
            date = LocalDate.now(),
            meal = Meal.Dinner,
            servingMultiplier = 1.0
        )

        val metadata = SyncMetadata(
            recordId = "metadata-entry",
            deviceId = "special-device",
            updatedAt = "2024-01-01T10:00:00Z",
            version = 10,
            deletedAt = "2024-01-02T00:00:00Z",
            lastSyncedAt = "2024-01-01T09:59:59Z",
            syncStatus = SyncStatus.SyncError
        )

        repository.save(DiaryEntryRecord(
            entry = entry,
            sync = metadata
        ))

        val record = repository.getById("metadata-entry")
        assertNotNull(record)
        assertEquals("special-device", record?.sync.deviceId)
        assertEquals("2024-01-01T10:00:00Z", record?.sync.updatedAt)
        assertEquals(10, record?.sync.version)
        assertEquals(SyncStatus.SyncError, record?.sync.syncStatus)
    }

    @Test
    fun test_list_empty_database() = runBlocking {
        val list = repository.list()
        assertTrue(list.isEmpty())
    }

    @Test
    fun test_getById_empty_database() = runBlocking {
        val record = repository.getById("nonexistent")
        assertNull(record)
    }

    @Test
    fun test_list_by_date_range_single_day() = runBlocking {
        val entry = DiaryEntry(
            id = "single-day-entry",
            food = FoodItem(
                id = "food-single",
                kind = FoodKind.Ingredient,
                name = "Single Day Food",
                servingQuantity = 100.0,
                servingUnit = "gram",
                nutrients = Nutrients(100.0)
            ),
            date = LocalDate.now(),
            meal = Meal.Lunch,
            servingMultiplier = 1.0
        )

        repository.save(createDiaryRecord(entry, "device-1"))

        val list = repository.listByDateRange(LocalDate.now(), LocalDate.now())
        assertEquals(1, list.size)
        assertEquals(Meal.Lunch, list[0].entry.meal)
    }

    @Test
    fun test_list_by_date_range_with_boundary_dates() = runBlocking {
        val entry1 = DiaryEntry(
            id = "start-entry",
            food = FoodItem(
                id = "food-start",
                kind = FoodKind.Ingredient,
                name = "Start Day Food",
                servingQuantity = 100.0,
                servingUnit = "gram",
                nutrients = Nutrients(100.0)
            ),
            date = LocalDate.now().minusDays(3),
            meal = Meal.Breakfast,
            servingMultiplier = 1.0
        )
        val entry2 = DiaryEntry(
            id = "end-entry",
            food = FoodItem(
                id = "food-end",
                kind = FoodKind.Ingredient,
                name = "End Day Food",
                servingQuantity = 100.0,
                servingUnit = "gram",
                nutrients = Nutrients(100.0)
            ),
            date = LocalDate.now().minusDays(1),
            meal = Meal.Dinner,
            servingMultiplier = 1.0
        )

        repository.save(createDiaryRecord(entry1, "device-1"))
        repository.save(createDiaryRecord(entry2, "device-1"))

        val list = repository.listByDateRange(
            LocalDate.now().minusDays(3),
            LocalDate.now().minusDays(1)
        )
        assertEquals(2, list.size)
        assertTrue(list.any { it.entry.date == LocalDate.now().minusDays(3) })
        assertTrue(list.any { it.entry.date == LocalDate.now().minusDays(1) })
    }
}
