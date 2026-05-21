package com.philipcosgrave.calorietracker.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.philipcosgrave.calorietracker.data.readDiaryEntries
import com.philipcosgrave.calorietracker.data.readFoodItems
import com.philipcosgrave.calorietracker.data.readStringList
import com.philipcosgrave.calorietracker.data.repository.AndroidLocalStore
import com.philipcosgrave.calorietracker.data.repository.DataStoreSyncStateRepository
import com.philipcosgrave.calorietracker.data.repository.LocalRepositoryFactory
import com.philipcosgrave.calorietracker.data.repository.RoomBarcodeAliasRepository
import com.philipcosgrave.calorietracker.data.repository.RoomDiaryRepository
import com.philipcosgrave.calorietracker.data.repository.RoomFoodRepository
import com.philipcosgrave.calorietracker.data.repository.RoomSyncOutboxRepository
import com.philipcosgrave.calorietracker.data.sync.ApiSyncService
import com.philipcosgrave.calorietracker.data.seedFoods
import com.philipcosgrave.calorietracker.data.seedRecipes
import com.philipcosgrave.calorietracker.domain.createBarcodeAliasRecord
import com.philipcosgrave.calorietracker.domain.createChangeEnvelope
import com.philipcosgrave.calorietracker.domain.createFoodRecord
import com.philipcosgrave.calorietracker.domain.createId
import com.philipcosgrave.calorietracker.domain.createSyncMetadata
import com.philipcosgrave.calorietracker.domain.nowIsoString
import com.philipcosgrave.calorietracker.domain.toFoodItem
import com.philipcosgrave.calorietracker.domain.toRecipeDraft
import com.philipcosgrave.calorietracker.model.DiaryEntry
import com.philipcosgrave.calorietracker.model.DiaryEntryRecord
import com.philipcosgrave.calorietracker.model.FoodItem
import com.philipcosgrave.calorietracker.model.FoodKind
import com.philipcosgrave.calorietracker.model.Nutrients
import com.philipcosgrave.calorietracker.model.RecipeComponent
import com.philipcosgrave.calorietracker.model.RecipeDraft
import com.philipcosgrave.calorietracker.model.SyncEntityType
import com.philipcosgrave.calorietracker.model.SyncOperation
import com.philipcosgrave.calorietracker.model.SyncSettings
import com.philipcosgrave.calorietracker.ui.screens.AddFoodScreen
import com.philipcosgrave.calorietracker.ui.screens.DiaryScreen
import com.philipcosgrave.calorietracker.ui.screens.LogFoodScreen
import com.philipcosgrave.calorietracker.ui.screens.NewIngredientScreen
import com.philipcosgrave.calorietracker.ui.screens.QuickCaloriesScreen
import com.philipcosgrave.calorietracker.ui.screens.RecipeBuilderScreen
import com.philipcosgrave.calorietracker.ui.screens.SyncSettingsScreen
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun CalorieTrackerApp() {
    val context = LocalContext.current
    val database = remember { LocalRepositoryFactory.database(context) }
    val localStore = remember {
        AndroidLocalStore(
            context = context,
            foodRepository = RoomFoodRepository(database.foodRecordDao()),
            barcodeAliasRepository = RoomBarcodeAliasRepository(database.barcodeAliasDao()),
            diaryRepository = RoomDiaryRepository(database.diaryRecordDao()),
            syncOutboxRepository = RoomSyncOutboxRepository(database.syncOutboxDao()),
            syncStateRepository = DataStoreSyncStateRepository(context),
        )
    }
    val scope = rememberCoroutineScope()
    val syncService = remember { ApiSyncService(localStore) }

    var customFoods by remember { mutableStateOf<List<FoodItem>>(emptyList()) }
    var recipes by remember { mutableStateOf<List<FoodItem>>(emptyList()) }
    var diary by remember { mutableStateOf<List<DiaryEntry>>(emptyList()) }
    var hiddenSeedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var screen by remember { mutableStateOf(AppScreen.Diary) }
    var previousScreen by remember { mutableStateOf(AppScreen.Diary) }
    var selectedFood by remember { mutableStateOf<FoodItem?>(null) }
    var recipeDraft by remember { mutableStateOf(RecipeDraft()) }
    var parentRecipeDraft by remember { mutableStateOf<RecipeDraft?>(null) }
    var editingFood by remember { mutableStateOf<FoodItem?>(null) }
    var syncSettings by remember {
        mutableStateOf(
            SyncSettings(
                syncEnabled = false,
                backupMode = SyncSettings.BackupMode.Disabled,
            ),
        )
    }
    var pendingChangeCount by remember { mutableStateOf(0) }

    suspend fun refreshState() {
        val foodRecords = localStore.foodRepository.list().filter { it.sync.deletedAt == null }
        customFoods = foodRecords.filter { it.food.kind == FoodKind.Ingredient }.map { it.food }
        recipes = foodRecords.filter { it.food.kind == FoodKind.Recipe }.map { it.food }
        diary = localStore.diaryRepository.list().filter { it.sync.deletedAt == null }.map { it.entry }
        hiddenSeedIds = localStore.hiddenSeedIds()
        syncSettings = localStore.syncStateRepository.getSettings()
        pendingChangeCount = localStore.syncOutboxRepository.listPendingChanges().size
    }

    suspend fun ensureSeedRecipes(deviceId: String) {
        if (localStore.foodRepository.list().any { it.food.kind == FoodKind.Recipe }) return
        seedRecipes().forEach { recipe ->
            localStore.foodRepository.save(createFoodRecord(recipe, deviceId))
        }
    }

    suspend fun saveFood(item: FoodItem) {
        val deviceId = localStore.deviceId()
        val existing = localStore.foodRepository.getById(item.id)
        val record = createFoodRecord(item, deviceId, existing)
        localStore.foodRepository.save(record)
        localStore.syncOutboxRepository.enqueue(
            createChangeEnvelope(
                entityType = SyncEntityType.FoodProduct,
                operation = SyncOperation.Upsert,
                deviceId = deviceId,
                recordId = record.sync.recordId,
                payload = record,
                baseVersion = existing?.sync?.version,
            ),
        )
        createBarcodeAliasRecord(item, deviceId)?.let { alias ->
            localStore.barcodeAliasRepository.save(alias)
            localStore.syncOutboxRepository.enqueue(
                createChangeEnvelope(
                    entityType = SyncEntityType.BarcodeAlias,
                    operation = SyncOperation.Upsert,
                    deviceId = deviceId,
                    recordId = alias.sync.recordId,
                    payload = alias,
                ),
            )
        }
    }

    suspend fun deleteFood(item: FoodItem) {
        val existing = localStore.foodRepository.getById(item.id) ?: return
        val deletedAt = nowIsoString()
        val deletedRecord = existing.copy(
            sync = existing.sync.copy(
                version = existing.sync.version + 1,
                updatedAt = deletedAt,
                deletedAt = deletedAt,
            ),
        )
        localStore.foodRepository.save(deletedRecord)
        localStore.syncOutboxRepository.enqueue(
            createChangeEnvelope(
                entityType = SyncEntityType.FoodProduct,
                operation = SyncOperation.Delete,
                deviceId = deletedRecord.sync.originDeviceId,
                recordId = deletedRecord.sync.recordId,
                payload = deletedRecord,
                baseVersion = existing.sync.version,
            ),
        )
    }

    suspend fun saveDiaryEntry(entry: DiaryEntry) {
        val deviceId = localStore.deviceId()
        val existing = localStore.diaryRepository.getById(entry.id)
        val updatedAt = nowIsoString()
        val record = existing?.copy(
            entry = entry,
            sync = existing.sync.copy(
                version = existing.sync.version + 1,
                updatedAt = updatedAt,
            ),
        ) ?: DiaryEntryRecord(
            entry = entry,
            sync = createSyncMetadata(
                recordId = entry.id,
                deviceId = deviceId,
                updatedAt = updatedAt,
            ).copy(syncStatus = com.philipcosgrave.calorietracker.model.SyncStatus.PendingPush),
        )
        localStore.diaryRepository.save(record)
        localStore.syncOutboxRepository.enqueue(
            createChangeEnvelope(
                entityType = SyncEntityType.DiaryEntry,
                operation = SyncOperation.Upsert,
                deviceId = record.sync.originDeviceId,
                recordId = record.sync.recordId,
                payload = record,
                baseVersion = existing?.sync?.version,
            ),
        )
    }

    suspend fun deleteDiaryEntry(entry: DiaryEntry) {
        val existing = localStore.diaryRepository.getById(entry.id) ?: return
        val deletedAt = nowIsoString()
        val deletedRecord = existing.copy(
            sync = existing.sync.copy(
                version = existing.sync.version + 1,
                updatedAt = deletedAt,
                deletedAt = deletedAt,
            ),
        )
        localStore.diaryRepository.save(deletedRecord)
        localStore.syncOutboxRepository.enqueue(
            createChangeEnvelope(
                entityType = SyncEntityType.DiaryEntry,
                operation = SyncOperation.Delete,
                deviceId = deletedRecord.sync.originDeviceId,
                recordId = deletedRecord.sync.recordId,
                payload = deletedRecord,
                baseVersion = existing.sync.version,
            ),
        )
    }

    LaunchedEffect(Unit) {
        localStore.migrateLegacyIfNeeded(
            readLegacyFoods = ::readFoodItems,
            readLegacyDiary = ::readDiaryEntries,
            readLegacyStrings = ::readStringList,
        )
        ensureSeedRecipes(localStore.deviceId())
        refreshState()
        if (syncSettings.syncEnabled && syncSettings.backupMode == SyncSettings.BackupMode.AutomaticBackup && !syncSettings.apiBaseUrl.isNullOrBlank()) {
            runCatching { syncService.syncNow() }
            refreshState()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (screen) {
            AppScreen.Diary -> DiaryScreen(
                selectedDate = selectedDate,
                entries = diary,
                onDateChange = { selectedDate = it },
                onAddFood = { screen = AppScreen.AddFood },
                onOpenSyncSettings = {
                    previousScreen = AppScreen.Diary
                    screen = AppScreen.SyncSettings
                },
                onDeleteEntry = { entry ->
                    diary = diary.filterNot { it.id == entry.id }
                    scope.launch {
                        deleteDiaryEntry(entry)
                        refreshState()
                    }
                },
                onUpdateEntry = { updated ->
                    diary = diary.map { if (it.id == updated.id) updated else it }
                    scope.launch {
                        saveDiaryEntry(updated)
                        refreshState()
                    }
                },
            )

            AppScreen.AddFood -> AddFoodScreen(
                date = selectedDate,
                foods = customFoods + recipes + seedFoods.filterNot { hiddenSeedIds.contains(it.id) },
                onBack = { screen = AppScreen.Diary },
                onOpenSyncSettings = {
                    previousScreen = AppScreen.AddFood
                    screen = AppScreen.SyncSettings
                },
                onQuickCalories = { screen = AppScreen.QuickCalories },
                onAddIngredient = {
                    editingFood = null
                    screen = AppScreen.NewIngredient
                },
                onAddRecipe = {
                    recipeDraft = RecipeDraft()
                    parentRecipeDraft = null
                    screen = AppScreen.RecipeBuilder
                },
                onSelectFood = {
                    selectedFood = it
                    screen = AppScreen.LogFood
                },
                onDeleteFood = { item ->
                    if (customFoods.any { it.id == item.id } || recipes.any { it.id == item.id }) {
                        customFoods = customFoods.filterNot { it.id == item.id }
                        recipes = recipes.filterNot { it.id == item.id }
                        scope.launch {
                            deleteFood(item)
                            refreshState()
                        }
                    } else {
                        hiddenSeedIds = hiddenSeedIds + item.id
                        scope.launch { localStore.saveHiddenSeedIds(hiddenSeedIds) }
                    }
                },
                onEditFood = { item ->
                    if (item.kind == FoodKind.Recipe) {
                        recipeDraft = item.toRecipeDraft()
                        editingFood = item
                        screen = AppScreen.RecipeBuilder
                    } else {
                        editingFood = item
                        screen = AppScreen.NewIngredient
                    }
                },
            )

            AppScreen.SyncSettings -> SyncSettingsScreen(
                settings = syncSettings,
                pendingChangeCount = pendingChangeCount,
                onBack = { screen = previousScreen },
                onSave = { settings ->
                    syncSettings = settings
                    scope.launch { localStore.syncStateRepository.saveSettings(settings); refreshState() }
                },
                onSyncNow = {
                    scope.launch {
                        syncService.syncNow()
                        refreshState()
                    }
                },
            )

            AppScreen.QuickCalories -> QuickCaloriesScreen(
                date = selectedDate,
                onBack = { screen = AppScreen.AddFood },
                onSave = { calories, meal, date ->
                    val entry = DiaryEntry(
                        id = createId("entry"),
                        food = FoodItem(
                            id = createId("quick"),
                            kind = FoodKind.Ingredient,
                            name = "Quick calories",
                            servingQuantity = 1.0,
                            servingUnit = "entry",
                            nutrients = Nutrients(calories = calories),
                        ),
                        date = date,
                        meal = meal,
                        servingMultiplier = 1.0,
                    )
                    diary = listOf(entry) + diary
                    selectedDate = date
                    scope.launch {
                        saveDiaryEntry(entry)
                        refreshState()
                    }
                    screen = AppScreen.Diary
                },
            )

            AppScreen.NewIngredient -> NewIngredientScreen(
                existing = editingFood,
                onBack = { screen = AppScreen.AddFood },
                onSave = { item ->
                    if (item.kind == FoodKind.Recipe) {
                        recipes = listOf(item) + recipes.filterNot { it.id == item.id }
                    } else {
                        customFoods = listOf(item) + customFoods.filterNot { it.id == item.id }
                    }
                    editingFood = null
                    scope.launch {
                        saveFood(item)
                        refreshState()
                    }
                    screen = if (parentRecipeDraft != null) AppScreen.RecipeBuilder else AppScreen.AddFood
                },
            )

            AppScreen.RecipeBuilder -> RecipeBuilderScreen(
                draft = recipeDraft,
                foods = customFoods + recipes.filter { it.id != editingFood?.id } + seedFoods,
                onDraftChange = { recipeDraft = it },
                onBack = {
                    parentRecipeDraft = null
                    editingFood = null
                    screen = AppScreen.AddFood
                },
                onAddIngredient = {
                    editingFood = null
                    screen = AppScreen.NewIngredient
                },
                onStartNestedRecipe = {
                    parentRecipeDraft = recipeDraft
                    recipeDraft = RecipeDraft()
                },
                onSave = { draft ->
                    val recipe = draft.toFoodItem(editingFood?.id)
                    recipes = listOf(recipe) + recipes.filterNot { it.id == recipe.id }
                    val parent = parentRecipeDraft
                    scope.launch {
                        saveFood(recipe)
                        refreshState()
                    }

                    if (parent != null) {
                        recipeDraft = parent.copy(
                            components = parent.components + RecipeComponent(recipe, recipe.servingQuantity, recipe.servingUnit),
                        )
                        parentRecipeDraft = null
                    } else {
                        recipeDraft = RecipeDraft()
                        editingFood = null
                        screen = AppScreen.AddFood
                    }
                },
            )

            AppScreen.LogFood -> selectedFood?.let { food ->
                LogFoodScreen(
                    food = food,
                    date = selectedDate,
                    onBack = { screen = AppScreen.AddFood },
                    onLog = { meal, date, amount, addMore ->
                        val multiplier = amount / food.servingQuantity.coerceAtLeast(0.1)
                        val entry = DiaryEntry(
                            id = createId("entry"),
                            food = food,
                            date = date,
                            meal = meal,
                            servingMultiplier = multiplier,
                        )
                        diary = listOf(entry) + diary
                        selectedDate = date
                        scope.launch {
                            saveDiaryEntry(entry)
                            refreshState()
                        }
                        screen = if (addMore) AppScreen.AddFood else AppScreen.Diary
                    },
                )
            } ?: run {
                screen = AppScreen.AddFood
            }
        }
    }
}
