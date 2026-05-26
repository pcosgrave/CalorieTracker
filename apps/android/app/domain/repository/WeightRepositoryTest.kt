package com.philipcosgrave.calorietracker.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.philipcosgrave.calorietracker.data.local.CalorieTrackerDatabase
import com.philipcosgrave.calorietracker.data.local.WeightRecordDao
import com.philipcosgrave.calorietracker.domain.createId
import com.philipcosgrave.calorietracker.model.WeightEntry
import org.junit.Assert.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class WeightRepositoryTest {

    private lateinit var database: CalorieTrackerDatabase
    private lateinit var dao: WeightRecordDao
    private lateinit var repository: RoomWeightRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, CalorieTrackerDatabase::class.java)
            .allowMainThreadOperations()
            .build()
        dao = database.weightRecordDao()
        repository = RoomWeightRepository(dao)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun test_list_returns_all_weight_entries() = runBlocking {
        val userId = "user-1"
        val entry1 = WeightEntry(
            id = createId("weight"),
            date = LocalDate.now().minusDays(7),
            weightKg = 70.5
        )
        val entry2 = WeightEntry(
            id = createId("weight"),
            date = LocalDate.now().minusDays(5),
            weightKg = 70.2
        )
        val entry3 = WeightEntry(
            id = createId("weight"),
            date = LocalDate.now(),
            weightKg = 69.8
        )

        repository.save(userId, entry1)
        repository.save(userId, entry2)
        repository.save(userId, entry3)

        val list = repository.list(userId)
        assertEquals(3, list.size)
        assertTrue(list.any { it.date == LocalDate.now().minusDays(7) })
        assertTrue(list.any { it.date == LocalDate.now() })
    }

    @Test
    fun test_list_different_users() = runBlocking {
        val entry1 = WeightEntry(
            id = createId("weight"),
            date = LocalDate.now(),
            weightKg = 70.0
        )
        val entry2 = WeightEntry(
            id = createId("weight"),
            date = LocalDate.now(),
            weightKg = 68.0
        )

        repository.save("user-1", entry1)
        repository.save("user-2", entry2)

        val user1List = repository.list("user-1")
        val user2List = repository.list("user-2")

        assertEquals(1, user1List.size)
        assertEquals(1, user2List.size)
        assertEquals(70.0, user1List[0].weightKg)
        assertEquals(68.0, user2List[0].weightKg)
    }

    @Test
    fun test_list_empty_user() = runBlocking {
        val list = repository.list("nonexistent-user")
        assertTrue(list.isEmpty())
    }

    @Test
    fun test_latest_returns_most_recent_entry() = runBlocking {
        val userId = "user-latest"
        val entry1 = WeightEntry(
            id = createId("weight"),
            date = LocalDate.now().minusDays(7),
            weightKg = 70.5
        )
        val entry2 = WeightEntry(
            id = createId("weight"),
            date = LocalDate.now(),
            weightKg = 69.8
        )

        repository.save(userId, entry1)
        repository.save(userId, entry2)

        val latest = repository.latest(userId)
        assertNotNull(latest)
        assertEquals(LocalDate.now(), latest.date)
        assertEquals(69.8, latest.weightKg)
    }

    @Test
    fun test_latest_returns_null_for_empty_user() = runBlocking {
        val latest = repository.latest("nonexistent-user")
        assertNull(latest)
    }

    @Test
    fun test_getById_returns_weight_entry() = runBlocking {
        val userId = "user-getbyid"
        val entry = WeightEntry(
            id = createId("weight"),
            date = LocalDate.now(),
            weightKg = 70.5
        )

        repository.save(userId, entry)

        val record = repository.getById(userId, entry.id)
        assertNotNull(record)
        assertEquals(70.5, record.weightKg)
        assertEquals(LocalDate.now(), record.date)
    }

    @Test
    fun test_getById_returns_null_for_nonexistent() = runBlocking {
        val record = repository.getById("user-getbyid", "nonexistent")
        assertNull(record)
    }

    @Test
    fun test_getById_returns_null_for_nonexistent_user() = runBlocking {
        val record = repository.getById("nonexistent-user", "weight-id")
        assertNull(record)
    }

    @Test
    fun test_save_adds_weight_entry() = runBlocking {
        val userId = "user-save"
        val entry = WeightEntry(
            id = createId("weight"),
            date = LocalDate.now(),
            weightKg = 70.0
        )

        repository.save(userId, entry)
        val list = repository.list(userId)
        assertEquals(1, list.size)
        assertEquals(70.0, list[0].weightKg)
    }

    @Test
    fun test_save_updates_existing_entry() = runBlocking {
        val userId = "user-save-update"
        val entry1 = WeightEntry(
            id = createId("weight"),
            date = LocalDate.now(),
            weightKg = 70.0
        )

        repository.save(userId, entry1)
        val list1 = repository.list(userId)
        assertEquals(70.0, list1[0].weightKg)

        val entry2 = WeightEntry(
            id = entry1.id,
            date = LocalDate.now(),
            weightKg = 70.5
        )

        repository.save(userId, entry2)
        val list2 = repository.list(userId)
        assertEquals(1, list2.size)
        assertEquals(70.5, list2[0].weightKg)
    }

    @Test
    fun test_save_with_different_dates_for_same_user() = runBlocking {
        val userId = "user-multiple"
        val entry1 = WeightEntry(
            id = createId("weight"),
            date = LocalDate.of(2024, 1, 1),
            weightKg = 70.0
        )
        val entry2 = WeightEntry(
            id = createId("weight"),
            date = LocalDate.of(2024, 2, 1),
            weightKg = 69.5
        )
        val entry3 = WeightEntry(
            id = createId("weight"),
            date = LocalDate.of(2024, 3, 1),
            weightKg = 69.0
        )

        repository.save(userId, entry1)
        repository.save(userId, entry2)
        repository.save(userId, entry3)

        val list = repository.list(userId)
        assertEquals(3, list.size)
        assertTrue(list.any { it.date == LocalDate.of(2024, 1, 1) })
        assertTrue(list.any { it.date == LocalDate.of(2024, 2, 1) })
        assertTrue(list.any { it.date == LocalDate.of(2024, 3, 1) })
    }

    @Test
    fun test_delete_removes_entry() = runBlocking {
        val userId = "user-delete"
        val entry = WeightEntry(
            id = createId("weight"),
            date = LocalDate.now(),
            weightKg = 70.0
        )

        repository.save(userId, entry)
        val listBefore = repository.list(userId)
        assertEquals(1, listBefore.size)

        repository.delete(userId, entry.id)
        val listAfter = repository.list(userId)
        assertTrue(listAfter.isEmpty())
    }

    @Test
    fun test_delete_no_op_for_nonexistent() = runBlocking {
        repository.delete("nonexistent-user", "nonexistent-id")
        // No exception should be thrown
    }

    @Test
    fun test_save_with_decimal_weight() = runBlocking {
        val userId = "user-decimal"
        val entry = WeightEntry(
            id = createId("weight"),
            date = LocalDate.now(),
            weightKg = 70.2345
        )

        repository.save(userId, entry)
        val list = repository.list(userId)
        assertEquals(70.2345, list[0].weightKg)
    }

    @Test
    fun test_save_with_zero_weight() = runBlocking {
        val userId = "user-zero"
        val entry = WeightEntry(
            id = createId("weight"),
            date = LocalDate.now(),
            weightKg = 0.0
        )

        repository.save(userId, entry)
        val list = repository.list(userId)
        assertEquals(0.0, list[0].weightKg)
    }

    @Test
    fun test_save_with_negative_weight() = runBlocking {
        val userId = "user-negative"
        val entry = WeightEntry(
            id = createId("weight"),
            date = LocalDate.now(),
            weightKg = -1.0
        )

        repository.save(userId, entry)
        val list = repository.list(userId)
        assertEquals(-1.0, list[0].weightKg)
    }

    @Test
    fun test_latest_returns_null_for_first_entry() = runBlocking {
        val userId = "user-first"
        // Don't save any entries initially

        val latest = repository.latest(userId)
        assertNull(latest)
    }
}
