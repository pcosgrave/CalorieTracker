package com.philipcosgrave.calorietracker.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.philipcosgrave.calorietracker.BuildConfig
import com.philipcosgrave.calorietracker.data.auth.CognitoAuthRepository
import com.philipcosgrave.calorietracker.data.health.HealthConnectAvailability
import com.philipcosgrave.calorietracker.data.health.HealthConnectNutritionExporter
import com.philipcosgrave.calorietracker.data.readDiaryEntries
import com.philipcosgrave.calorietracker.data.readFoodItems
import com.philipcosgrave.calorietracker.data.readStringList
import com.philipcosgrave.calorietracker.data.remote.CanadianNutrientFileLookupService
import com.philipcosgrave.calorietracker.data.remote.OpenFoodFactsLookupService
import com.philipcosgrave.calorietracker.data.repository.AndroidLocalStore
import com.philipcosgrave.calorietracker.data.repository.DataStoreSyncStateRepository
import com.philipcosgrave.calorietracker.data.repository.LocalRepositoryFactory
import com.philipcosgrave.calorietracker.data.repository.RoomBarcodeAliasRepository
import com.philipcosgrave.calorietracker.data.repository.RoomDiaryRepository
import com.philipcosgrave.calorietracker.data.repository.RoomFoodRepository
import com.philipcosgrave.calorietracker.data.repository.RoomSyncOutboxRepository
import com.philipcosgrave.calorietracker.data.repository.RoomWeightRepository
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
import com.philipcosgrave.calorietracker.model.HealthDashboardMetrics
import com.philipcosgrave.calorietracker.model.Meal
import com.philipcosgrave.calorietracker.model.Nutrients
import com.philipcosgrave.calorietracker.model.RecipeComponent
import com.philipcosgrave.calorietracker.model.RecipeDraft
import com.philipcosgrave.calorietracker.model.AuthSession
import com.philipcosgrave.calorietracker.model.SyncEntityType
import com.philipcosgrave.calorietracker.model.SyncOperation
import com.philipcosgrave.calorietracker.model.SyncSettings
import com.philipcosgrave.calorietracker.model.WeightEntry
import com.philipcosgrave.calorietracker.ui.screens.SearchFoodScreen
import com.philipcosgrave.calorietracker.ui.screens.BarcodeScannerScreen
import com.philipcosgrave.calorietracker.ui.screens.DiaryScreen
import com.philipcosgrave.calorietracker.ui.screens.HomeScreen
import com.philipcosgrave.calorietracker.ui.screens.LogFoodScreen
import com.philipcosgrave.calorietracker.ui.screens.LogWeightScreen
import com.philipcosgrave.calorietracker.ui.screens.AddIngredientScreen
import com.philipcosgrave.calorietracker.ui.screens.QuickCaloriesScreen
import com.philipcosgrave.calorietracker.ui.screens.RecipeBuilderScreen
import com.philipcosgrave.calorietracker.ui.screens.SyncSettingsScreen
import com.philipcosgrave.calorietracker.ui.screens.WeightScreen
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun CalorieTrackerApp(
    authCallbackUri: String? = null,
    onAuthCallbackConsumed: () -> Unit = {},
    openExternalUri: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val database = remember { LocalRepositoryFactory.database(context) }
    val authRepository = remember { CognitoAuthRepository(context) }
    val localStore = remember {
        AndroidLocalStore(
            context = context,
            foodRepository = RoomFoodRepository(database.foodRecordDao()),
            barcodeAliasRepository = RoomBarcodeAliasRepository(database.barcodeAliasDao()),
            diaryRepository = RoomDiaryRepository(database.diaryRecordDao()),
            weightRepository = RoomWeightRepository(database.weightRecordDao()),
            syncOutboxRepository = RoomSyncOutboxRepository(database.syncOutboxDao()),
            syncStateRepository = DataStoreSyncStateRepository(context),
            authRepository = authRepository,
        )
    }
    val scope = rememberCoroutineScope()
    val syncService = remember { ApiSyncService(localStore) }
    val openFoodFactsLookupService = remember { OpenFoodFactsLookupService() }
    val canadianNutrientFileLookupService = remember { CanadianNutrientFileLookupService() }
    val healthConnectExporter = remember { HealthConnectNutritionExporter(context) }

    var customFoods by remember { mutableStateOf<List<FoodItem>>(emptyList()) }
    var recipes by remember { mutableStateOf<List<FoodItem>>(emptyList()) }
    var diary by remember { mutableStateOf<List<DiaryEntry>>(emptyList()) }
    var weights by remember { mutableStateOf<List<WeightEntry>>(emptyList()) }
    var hiddenSeedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var screen by remember { mutableStateOf(AppScreen.Home) }
    var previousScreen by remember { mutableStateOf(AppScreen.Home) }
    var selectedFood by remember { mutableStateOf<FoodItem?>(null) }
    var recipeDraft by remember { mutableStateOf(RecipeDraft()) }
    var parentRecipeDraft by remember { mutableStateOf<RecipeDraft?>(null) }
    var returnToRecipeAfterIngredientSave by remember { mutableStateOf(false) }
    var editingFood by remember { mutableStateOf<FoodItem?>(null) }
    var editingWeight by remember { mutableStateOf<WeightEntry?>(null) }
    var syncSettings by remember {
        mutableStateOf(
            SyncSettings(
                syncEnabled = false,
                backupMode = SyncSettings.BackupMode.Disabled,
                apiBaseUrl = BuildConfig.SYNC_API_BASE_URL,
            ),
        )
    }
    var pendingChangeCount by remember { mutableStateOf(0) }
    var authSession by remember { mutableStateOf<AuthSession?>(null) }
    var healthConnectAvailability by remember { mutableStateOf(HealthConnectAvailability.Unavailable) }
    var healthConnectPermissionGranted by remember { mutableStateOf(false) }
    var healthConnectExportEnabled by remember { mutableStateOf(false) }
    var healthMetrics by remember { mutableStateOf(HealthDashboardMetrics()) }
    var remoteSearchResults by remember { mutableStateOf<List<FoodItem>>(emptyList()) }
    var remoteSearchQuery by remember { mutableStateOf("") }
    var isSearchingRemote by remember { mutableStateOf(false) }

    suspend fun refreshState() {
        val foodRecords = localStore.foodRepository.list().filter { it.sync.deletedAt == null }
        customFoods = foodRecords.filter { it.food.kind == FoodKind.Ingredient }.map { it.food }
        recipes = foodRecords.filter { it.food.kind == FoodKind.Recipe }.map { it.food }
        diary = localStore.diaryRepository.list().filter { it.sync.deletedAt == null }.map { it.entry }
        weights = localStore.weightRepository.list(localStore.currentOwnerUserId())
        hiddenSeedIds = localStore.hiddenSeedIds()
        syncSettings = localStore.syncStateRepository.getSettings()
        pendingChangeCount = localStore.syncOutboxRepository.listPendingChanges().size
        authSession = localStore.currentAuthSession()
        healthConnectAvailability = healthConnectExporter.availability()
        healthConnectPermissionGranted =
            healthConnectAvailability == HealthConnectAvailability.Available &&
                healthConnectExporter.hasRequestedPermissions()
        healthConnectExportEnabled = localStore.isHealthConnectExportEnabled()
        healthMetrics =
            if (healthConnectPermissionGranted) healthConnectExporter.readTodayMetrics()
            else HealthDashboardMetrics()
    }

    val healthPermissionsLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) { grantedPermissions ->
        scope.launch {
            val granted =
                HealthConnectNutritionExporter.requestedPermissions.all { it in grantedPermissions }
            if (granted) {
                localStore.setHealthConnectExportEnabled(true)
            }
            refreshState()
        }
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
        createBarcodeAliasRecord(item, deviceId, localStore.currentOwnerUserId())?.let { alias ->
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
        if (healthConnectAvailability == HealthConnectAvailability.Available &&
            healthConnectPermissionGranted &&
            healthConnectExportEnabled
        ) {
            runCatching { healthConnectExporter.exportEntry(record) }
        }
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
        if (healthConnectAvailability == HealthConnectAvailability.Available &&
            healthConnectPermissionGranted &&
            healthConnectExportEnabled
        ) {
            runCatching { healthConnectExporter.deleteEntry(entry.id) }
        }
    }

    suspend fun saveWeightEntry(entry: WeightEntry) {
        val weightEntry = entry.copy(id = entry.id.ifBlank { createId("weight") })
        localStore.weightRepository.save(
            localStore.currentOwnerUserId(),
            weightEntry,
        )
        if (healthConnectAvailability == HealthConnectAvailability.Available &&
            healthConnectPermissionGranted &&
            healthConnectExportEnabled
        ) {
            runCatching { healthConnectExporter.exportWeightEntry(weightEntry) }
        }
    }

    suspend fun deleteWeightEntry(entry: WeightEntry) {
        localStore.weightRepository.delete(
            localStore.currentOwnerUserId(),
            entry.id,
        )
        if (healthConnectAvailability == HealthConnectAvailability.Available &&
            healthConnectPermissionGranted &&
            healthConnectExportEnabled
        ) {
            runCatching { healthConnectExporter.deleteWeightEntry(entry.id) }
        }
    }

    suspend fun importWeightHistoryFromHealthConnect() {
        val ownerUserId = localStore.currentOwnerUserId()
        val existing = localStore.weightRepository.list(ownerUserId)
        val existingKeys = existing.map { "${it.date}:${it.weightKg}" }.toSet()
        healthConnectExporter.importWeightEntries()
            .filterNot { "${it.date}:${it.weightKg}" in existingKeys }
            .forEach { entry ->
                localStore.weightRepository.save(ownerUserId, entry)
            }
    }

    suspend fun importNutritionHistoryFromHealthConnect() {
        val importedEntries = healthConnectExporter.importNutritionEntries()
        val existingIds = localStore.diaryRepository.list().map { it.entry.id }.toSet()
        importedEntries
            .filterNot { it.id in existingIds }
            .forEach { entry ->
                val deviceId = localStore.deviceId()
                val updatedAt = nowIsoString()
                val record = DiaryEntryRecord(
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
                    ),
                )
            }
    }

    suspend fun findFoodByBarcode(barcode: String): FoodItem? {
        localStore.foodRepository.getByBarcode(barcode)?.food?.let { return it }
        localStore.barcodeAliasRepository.getByBarcode(barcode)?.let { alias ->
            localStore.foodRepository.getById(alias.productId)?.food?.let { return it }
        }
        return seedFoods.firstOrNull { it.barcode == barcode }
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

    LaunchedEffect(authCallbackUri) {
        val callback = authCallbackUri ?: return@LaunchedEffect
        if (callback.startsWith(BuildConfig.COGNITO_ANDROID_REDIRECT_URI)) {
            runCatching {
                authRepository.completeSignIn(android.net.Uri.parse(callback))
            }
        }
        refreshState()
        onAuthCallbackConsumed()
    }

    LaunchedEffect(screen) {
        if (screen == AppScreen.Home) {
            refreshState()
        }
    }

    androidx.compose.runtime.DisposableEffect(lifecycleOwner, screen) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && screen == AppScreen.Home) {
                scope.launch { refreshState() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        color = MaterialTheme.colorScheme.background,
    ) {
        when (screen) {
            AppScreen.Home -> HomeScreen(
                caloriesLogged = diary.filter { it.date == selectedDate }.sumOf {
                    it.food.nutrients.calories * it.servingMultiplier
                },
                healthMetrics = healthMetrics,
                latestWeightKg = weights.maxByOrNull { it.date }?.weightKg,
                weightUnit = syncSettings.weightUnit,
                onOpenFoodLog = { screen = AppScreen.Diary },
                onOpenWeight = { screen = AppScreen.Weight },
                onOpenSyncSettings = {
                    previousScreen = AppScreen.Home
                    screen = AppScreen.SyncSettings
                },
            )

            AppScreen.Diary -> DiaryScreen(
                selectedDate = selectedDate,
                entries = diary,
                targetRangeMin = syncSettings.calorieTargetMin,
                targetRangeMax = syncSettings.calorieTargetMax,
                onDateChange = { selectedDate = it },
                onBack = { screen = AppScreen.Home },
                onAddFood = { screen = AppScreen.SearchFood },
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

            AppScreen.Weight -> WeightScreen(
                weights = weights,
                weightUnit = syncSettings.weightUnit,
                goalWeightKg = syncSettings.goalWeightKg,
                onBack = { screen = AppScreen.Home },
                onLogWeight = {
                    editingWeight = null
                    screen = AppScreen.LogWeight
                },
                onEditWeight = { entry ->
                    editingWeight = entry
                    selectedDate = entry.date
                    screen = AppScreen.LogWeight
                },
                onDeleteWeight = { entry ->
                    weights = weights.filterNot { it.id == entry.id }
                    scope.launch {
                        deleteWeightEntry(entry)
                        refreshState()
                    }
                },
            )

            AppScreen.LogWeight -> LogWeightScreen(
                initialDate = selectedDate,
                weightUnit = syncSettings.weightUnit,
                existingEntry = editingWeight,
                onBack = {
                    editingWeight = null
                    screen = AppScreen.Weight
                },
                onSave = { entry ->
                    selectedDate = entry.date
                    scope.launch {
                        saveWeightEntry(entry)
                        refreshState()
                    }
                    editingWeight = null
                    screen = AppScreen.Weight
                },
            )

            AppScreen.SearchFood -> SearchFoodScreen(
                date = selectedDate,
                foods = customFoods + recipes + seedFoods.filterNot { hiddenSeedIds.contains(it.id) },
                onBack = { screen = AppScreen.Diary },
                onOpenSyncSettings = {
                    previousScreen = AppScreen.SearchFood
                    screen = AppScreen.SyncSettings
                },
                onQuickCalories = { screen = AppScreen.QuickCalories },
                onScanBarcode = {
                    screen = AppScreen.BarcodeScanner
                },
                onAddIngredient = {
                    editingFood = null
                    returnToRecipeAfterIngredientSave = false
                    screen = AppScreen.AddIngredient
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
                onQuickLogFood = { food ->
                    val today = LocalDate.now()
                    val entry = DiaryEntry(
                        id = createId("entry"),
                        food = food,
                        date = today,
                        meal = Meal.Snack,
                        servingMultiplier = 1.0,
                    )
                    diary = listOf(entry) + diary
                    selectedDate = today
                    scope.launch {
                        saveDiaryEntry(entry)
                        refreshState()
                    }
                },
                remoteSearchResults = remoteSearchResults,
                remoteSearchQuery = remoteSearchQuery,
                isSearchingRemote = isSearchingRemote,
                onSearchCanadianNutrientFile = { query ->
                    remoteSearchQuery = query.trim()
                    isSearchingRemote = true
                    scope.launch {
                        remoteSearchResults = canadianNutrientFileLookupService.searchFoodsByName(remoteSearchQuery)
                        isSearchingRemote = false
                    }
                },
                onImportRemoteFood = { item ->
                    scope.launch {
                        saveFood(item)
                        refreshState()
                        remoteSearchResults = remoteSearchResults.map { remote ->
                            if (remote.barcode.isNotBlank() && remote.barcode == item.barcode) item
                            else if (remote.name == item.name && remote.brand == item.brand) item
                            else remote
                        }
                        selectedFood = item
                        screen = AppScreen.LogFood
                    }
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
                        screen = AppScreen.AddIngredient
                    }
                },
            )

            AppScreen.BarcodeScanner -> BarcodeScannerScreen(
                onBack = { screen = AppScreen.SearchFood },
                onBarcodeDetected = { barcode ->
                    scope.launch {
                        val found = findFoodByBarcode(barcode)
                        if (found != null) {
                            selectedFood = found
                            screen = AppScreen.LogFood
                        } else {
                            val remoteFood = openFoodFactsLookupService.lookupFoodByBarcode(barcode)
                            if (remoteFood != null) {
                                saveFood(remoteFood)
                                refreshState()
                                selectedFood = remoteFood
                                screen = AppScreen.LogFood
                            } else {
                                editingFood = FoodItem(
                                    id = createId("custom"),
                                    kind = FoodKind.Ingredient,
                                    name = "",
                                    brand = "",
                                    barcode = barcode,
                                    servingQuantity = 1.0,
                                    servingUnit = "serving",
                                    nutrients = Nutrients(calories = 0.0),
                                )
                                screen = AppScreen.AddIngredient
                            }
                        }
                    }
                },
            )

            AppScreen.SyncSettings -> SyncSettingsScreen(
                settings = syncSettings,
                pendingChangeCount = pendingChangeCount,
                authSession = authSession,
                healthConnectAvailability = healthConnectAvailability,
                healthConnectPermissionGranted = healthConnectPermissionGranted,
                healthConnectExportEnabled = healthConnectExportEnabled,
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
                onSignIn = {
                    scope.launch {
                        val uri = authRepository.beginSignIn()
                        openExternalUri(uri.toString())
                    }
                },
                onSignOut = {
                    scope.launch {
                        val uri = authRepository.signOut()
                        refreshState()
                        openExternalUri(uri.toString())
                    }
                },
                onConnectHealthConnect = {
                    when (healthConnectAvailability) {
                        HealthConnectAvailability.Available -> {
                            healthPermissionsLauncher.launch(HealthConnectNutritionExporter.requestedPermissions)
                        }

                        HealthConnectAvailability.UpdateRequired -> {
                            openExternalUri(HealthConnectNutritionExporter.onboardingUri().toString())
                        }

                        HealthConnectAvailability.Unavailable -> Unit
                    }
                },
                onSetHealthConnectExportEnabled = { enabled ->
                    scope.launch {
                        localStore.setHealthConnectExportEnabled(enabled)
                        refreshState()
                    }
                },
                onImportWeightHistory = {
                    scope.launch {
                        importWeightHistoryFromHealthConnect()
                        refreshState()
                    }
                },
                onImportNutritionHistory = {
                    scope.launch {
                        importNutritionHistoryFromHealthConnect()
                        refreshState()
                    }
                },
            )

            AppScreen.QuickCalories -> QuickCaloriesScreen(
                date = selectedDate,
                onBack = { screen = AppScreen.SearchFood },
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

            AppScreen.AddIngredient -> AddIngredientScreen(
                existing = editingFood,
                onBack = {
                    editingFood = null
                    val nextScreen = if (returnToRecipeAfterIngredientSave) AppScreen.RecipeBuilder else AppScreen.SearchFood
                    returnToRecipeAfterIngredientSave = false
                    screen = nextScreen
                },
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
                    editingFood = null
                    if (returnToRecipeAfterIngredientSave && item.kind == FoodKind.Ingredient) {
                        recipeDraft = recipeDraft.copy(
                            components = recipeDraft.components + RecipeComponent(item, item.servingQuantity, item.servingUnit),
                        )
                        returnToRecipeAfterIngredientSave = false
                        screen = AppScreen.RecipeBuilder
                    } else {
                        returnToRecipeAfterIngredientSave = false
                        screen = if (parentRecipeDraft != null) AppScreen.RecipeBuilder else AppScreen.SearchFood
                    }
                },
            )

            AppScreen.RecipeBuilder -> RecipeBuilderScreen(
                draft = recipeDraft,
                foods = customFoods + recipes.filter { it.id != editingFood?.id } + seedFoods,
                onDraftChange = { recipeDraft = it },
                onBack = {
                    parentRecipeDraft = null
                    returnToRecipeAfterIngredientSave = false
                    editingFood = null
                    screen = AppScreen.SearchFood
                },
                onAddIngredient = {
                    editingFood = null
                    returnToRecipeAfterIngredientSave = true
                    screen = AppScreen.AddIngredient
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
                        screen = AppScreen.SearchFood
                    }
                },
            )

            AppScreen.LogFood -> selectedFood?.let { food ->
                LogFoodScreen(
                    food = food,
                    date = selectedDate,
                onBack = { screen = AppScreen.SearchFood },
                    onLog = { meal, date, loggedFood, amount, addMore ->
                        val multiplier = amount / loggedFood.servingQuantity.coerceAtLeast(0.1)
                        val entry = DiaryEntry(
                            id = createId("entry"),
                            food = loggedFood,
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
                        screen = if (addMore) AppScreen.SearchFood else AppScreen.Diary
                    },
                )
            } ?: run {
                screen = AppScreen.SearchFood
            }
        }
    }
}
