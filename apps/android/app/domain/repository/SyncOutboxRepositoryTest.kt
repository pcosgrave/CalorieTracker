package com.philipcosgrave.calorietracker.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.philipcosgrave.calorietracker.data.local.CalorieTrackerDatabase
import com.philipcosgrave.calorietracker.data.local.SyncOutboxDao
import com.philipcosgrave.calorietracker.domain.createChangeEnvelope
import com.philipcosgrave.calorietracker.model.*
import org.junit.Assert.*
import org.junit.After
import org.junit.Before
import org.junit.Test

class SyncOutboxRepositoryTest {

    private lateinit var database: CalorieTrackerDatabase
    private lateinit var dao: SyncOutboxDao
    private lateinit var repository: RoomSyncOutboxRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, CalorieTrackerDatabase::class.java)
            .allowMainThreadOperations()
            .build()
        dao = database.syncOutboxDao()
        repository = RoomSyncOutboxRepository(dao)
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun test_listPendingChanges_returns_all_pending() = runBlocking {
        // Create some changes in different states
        val envelope1 = createChangeEnvelope(
            entityType = SyncEntityType.FoodProduct,
            operation = SyncOperation.Upsert,
            deviceId = "device-1",
            recordId = "food-1"
        )
        val envelope2 = createChangeEnvelope(
            entityType = SyncEntityType.DiaryEntry,
            operation = SyncOperation.Upsert,
            deviceId = "device-1",
            recordId = "entry-1"
        )

        repository.enqueue(envelope1)
        repository.enqueue(envelope2)

        // All are pending (no ack yet)
        val pending = repository.listPendingChanges()
        assertEquals(2, pending.size)
        assertTrue(pending.any { it.recordId == "food-1" })
        assertTrue(pending.any { it.recordId == "entry-1" })
    }

    @Test
    fun test_listPendingChanges_empty_outbox() = runBlocking {
        val pending = repository.listPendingChanges()
        assertTrue(pending.isEmpty())
    }

    @Test
    fun test_enqueue_adds_change() = runBlocking {
        val change = createChangeEnvelope(
            entityType = SyncEntityType.FoodProduct,
            operation = SyncOperation.Upsert,
            deviceId = "device-1",
            recordId = "new-food"
        )

        repository.enqueue(change)

        val pending = repository.listPendingChanges()
        assertEquals(1, pending.size)
        assertEquals(SyncEntityType.FoodProduct, pending[0].entityType)
        assertEquals(SyncOperation.Upsert, pending[0].operation)
    }

    @Test
    fun test_enqueue_preserves_change_id() = runBlocking {
        val change = createChangeEnvelope(
            changeId = "custom-change-id",
            entityType = SyncEntityType.FoodProduct,
            operation = SyncOperation.Upsert,
            deviceId = "device-1",
            recordId = "food"
        )

        repository.enqueue(change)

        val pending = repository.listPendingChanges()
        assertEquals("custom-change-id", pending[0].changeId)
    }

    @Test
    fun test_acknowledge_removes_changes() = runBlocking {
        val envelope1 = createChangeEnvelope(
            entityType = SyncEntityType.FoodProduct,
            operation = SyncOperation.Upsert,
            deviceId = "device-1",
            recordId = "food-1"
        )
        val envelope2 = createChangeEnvelope(
            entityType = SyncEntityType.DiaryEntry,
            operation = SyncOperation.Upsert,
            deviceId = "device-1",
            recordId = "entry-1"
        )
        val envelope3 = createChangeEnvelope(
            entityType = SyncEntityType.FoodProduct,
            operation = SyncOperation.Upsert,
            deviceId = "device-1",
            recordId = "food-2"
        )

        repository.enqueue(envelope1)
        repository.enqueue(envelope2)
        repository.enqueue(envelope3)

        assertEquals(3, repository.listPendingChanges().size)

        // Acknowledge first two
        repository.acknowledge(
            listOf(envelope1.changeId, envelope2.changeId),
            "2024-01-01T12:00:00Z"
        )

        val pending = repository.listPendingChanges()
        assertEquals(1, pending.size)
        assertEquals("food-2", pending[0].recordId)
    }

    @Test
    fun test_acknowledge_empty_list() = runBlocking {
        // Acknowledge non-existent changes
        repository.acknowledge(emptyList(), "2024-01-01T12:00:00Z")
        // No exception should be thrown
    }

    @Test
    fun test_acknowledge_partial_ack() = runBacking {
        val envelope1 = createChangeEnvelope(
            entityType = SyncEntityType.FoodProduct,
            operation = SyncOperation.Upsert,
            deviceId = "device-1",
            recordId = "food-1"
        )
        val envelope2 = createChangeEnvelope(
            entityType = SyncEntityType.FoodProduct,
            operation = SyncOperation.Upsert,
            deviceId = "device-1",
            recordId = "food-2"
        )

        repository.enqueue(envelope1)
        repository.enqueue(envelope2)

        // Acknowledge only food-2
        repository.acknowledge(
            listOf(envelope2.changeId),
            "2024-01-01T12:00:00Z"
        )

        val pending = repository.listPendingChanges()
        assertEquals(1, pending.size)
        assertEquals("food-1", pending[0].recordId)
    }

    @Test
    fun test_markRejected_marks_change_as_rejected() = runBlocking {
        val envelope = createChangeEnvelope(
            entityType = SyncEntityType.FoodProduct,
            operation = SyncOperation.Upsert,
            deviceId = "device-1",
            recordId = "food-reject"
        )

        repository.enqueue(envelope)
        assertEquals(1, repository.listPendingChanges().size)

        repository.markRejected(envelope.changeId)

        val pending = repository.listPendingChanges()
        assertTrue(pending.isEmpty())
    }

    @Test
    fun test_markRejected_no_op_for_nonexistent() = runBlocking {
        repository.markRejected("nonexistent-change-id")
        // No exception should be thrown
    }

    @Test
    fun test_clear_removes_all_changes() = runBacking {
        val envelope1 = createChangeEnvelope(
            entityType = SyncEntityType.FoodProduct,
            operation = SyncOperation.Upsert,
            deviceId = "device-1",
            recordId = "food-1"
        )
        val envelope2 = createChangeEnvelope(
            entityType = SyncEntityType.DiaryEntry,
            operation = SyncOperation.Upsert,
            deviceId = "device-1",
            recordId = "entry-1"
        )
        val envelope3 = createChangeEnvelope(
            entityType = SyncEntityType.FoodProduct,
            operation = SyncOperation.Upsert,
            deviceId = "device-1",
            recordId = "food-2"
        )

        repository.enqueue(envelope1)
        repository.enqueue(envelope2)
        repository.enqueue(envelope3)

        assertEquals(3, repository.listPendingChanges().size)

        repository.clear()

        val pending = repository.listPendingChanges()
        assertTrue(pending.isEmpty())
    }

    @Test
    fun test_clear_empty_outbox() = runBlocking {
        repository.clear()
        // No exception should be thrown
    }

    @Test
    fun test_listPendingChanges_by_entity_type() = runBlocking {
        val foodChanges = mutableListOf<SyncChangeEnvelope<*>>()
        for (i in 1..5) {
            foodChanges.add(createChangeEnvelope(
                entityType = SyncEntityType.FoodProduct,
                operation = SyncOperation.Upsert,
                deviceId = "device-1",
                recordId = "food-$i"
            ))
        }

        val diaryChanges = listOf(
            createChangeEnvelope(
                entityType = SyncEntityType.DiaryEntry,
                operation = SyncOperation.Upsert,
                deviceId = "device-1",
                recordId = "entry-1"
            ),
            createChangeEnvelope(
                entityType = SyncEntityType.DiaryEntry,
                operation = SyncOperation.Upsert,
                deviceId = "device-1",
                recordId = "entry-2"
            )
        )

        for (change in foodChanges) repository.enqueue(change)
        for (change in diaryChanges) repository.enqueue(change)

        val pending = repository.listPendingChanges()
        assertEquals(7, pending.size)
        assertTrue(pending.all { it.entityType == SyncEntityType.FoodProduct })
    }

    @Test
    fun test_enqueue_with_different_devices() = runBlocking {
        val device1Change = createChangeEnvelope(
            entityType = SyncEntityType.FoodProduct,
            operation = SyncOperation.Upsert,
            deviceId = "device-1",
            recordId = "food-device1"
        )

        val device2Change = createChangeEnvelope(
            entityType = SyncEntityType.FoodProduct,
            operation = SyncOperation.Upsert,
            deviceId = "device-2",
            recordId = "food-device2"
        )

        repository.enqueue(device1Change)
        repository.enqueue(device2Change)

        val pending = repository.listPendingChanges()
        assertEquals(2, pending.size)
        assertTrue(pending.any { it.deviceId == "device-1" })
        assertTrue(pending.any { it.deviceId == "device-2" })
    }

    @Test
    fun test_enqueue_preserves_all_fields() = runBlocking {
        val envelope = createChangeEnvelope(
            changeId = "test-change",
            entityType = SyncEntityType.FoodProduct,
            operation = SyncOperation.Upsert,
            deviceId = "test-device",
            recordId = "test-food",
            payload = mapOf("name" to "Test"),
            baseVersion = 5L
        )

        repository.enqueue(envelope)

        val pending = repository.listPendingChanges()
        assertEquals("test-change", pending[0].changeId)
        assertEquals(SyncEntityType.FoodProduct, pending[0].entityType)
        assertEquals(SyncOperation.Upsert, pending[0].operation)
        assertEquals("test-device", pending[0].deviceId)
        assertEquals("test-food", pending[0].recordId)
        assertNotNull(pending[0].payload)
        assertEquals(5L, pending[0].baseVersion)
    }

    @Test
    fun test_acknowledge_same_change_twice() = runBlocking {
        val envelope = createChangeEnvelope(
            entityType = SyncEntityType.FoodProduct,
            operation = SyncOperation.Upsert,
            deviceId = "device-1",
            recordId = "food"
        )

        repository.enqueue(envelope)
        assertEquals(1, repository.listPendingChanges().size)

        // Acknowledge first time
        repository.acknowledge(listOf(envelope.changeId), "2024-01-01T12:00:00Z")
        assertEquals(0, repository.listPendingChanges().size)

        // Acknowledge second time (should be no-op)
        repository.acknowledge(listOf(envelope.changeId), "2024-01-01T12:01:00Z")
        assertEquals(0, repository.listPendingChanges().size)
    }

    @Test
    fun test_enqueue_multiple_times_same_record() = runBlocking {
        val foodRecord1 = createChangeEnvelope(
            entityType = SyncEntityType.FoodProduct,
            operation = SyncOperation.Upsert,
            deviceId = "device-1",
            recordId = "food-override"
        )
        val foodRecord2 = createChangeEnvelope(
            entityType = SyncEntityType.FoodProduct,
            operation = SyncOperation.Upsert,
            deviceId = "device-2",
            recordId = "food-override"
        )

        repository.enqueue(foodRecord1)
        assertEquals(1, repository.listPendingChanges().size)

        repository.enqueue(foodRecord2)
        assertEquals(1, repository.listPendingChanges().size) // Still only one

        // The last one should be visible
        val pending = repository.listPendingChanges()
        assertEquals("device-2", pending[0].deviceId)
    }
}
