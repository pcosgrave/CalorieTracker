package com.philipcosgrave.calorietracker.ui

import com.philipcosgrave.calorietracker.domain.formatNumber

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.philipcosgrave.calorietracker.ui.components.*
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.room.withTransaction
import com.philipcosgrave.calorietracker.domain.Leftover
import com.philipcosgrave.calorietracker.domain.splitLeftover
import com.philipcosgrave.calorietracker.domain.leftoverDiaryEntries
import com.philipcosgrave.calorietracker.data.local.toJsonString
import com.philipcosgrave.calorietracker.data.local.diaryEntryFromJsonString
import com.philipcosgrave.calorietracker.data.local.toRecord
import com.philipcosgrave.calorietracker.data.local.toLeftover
import com.philipcosgrave.calorietracker.ui.screens.DraftsSaver
import com.philipcosgrave.calorietracker.ui.screens.SpeakFoodScreen
import com.philipcosgrave.calorietracker.domain.PhotoFoodDraft
import com.philipcosgrave.calorietracker.domain.voiceReviewDrafts
import com.philipcosgrave.calorietracker.ui.screens.PhotoFoodScreen
import com.philipcosgrave.calorietracker.ui.screens.ManageFoodsScreen
import com.philipcosgrave.calorietracker.domain.foodTitle
import com.philipcosgrave.calorietracker.BuildConfig
import com.philipcosgrave.calorietracker.data.auth.CognitoAuthRepository
import com.philipcosgrave.calorietracker.data.health.HealthConnectAvailability
import com.philipcosgrave.calorietracker.data.health.HealthConnectNutritionExporter
import com.philipcosgrave.calorietracker.data.readDiaryEntries
import com.philipcosgrave.calorietracker.data.readFoodItems
import com.philipcosgrave.calorietracker.data.readStringList
import com.philipcosgrave.calorietracker.data.remote.CanadianNutrientFileLookupService
import com.philipcosgrave.calorietracker.data.remote.CloudFoodCatalogService
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
import com.philipcosgrave.calorietracker.domain.convertAmount
import com.philipcosgrave.calorietracker.domain.inferMealForTime
import com.philipcosgrave.calorietracker.domain.normalizeVoiceSearchQuery
import com.philipcosgrave.calorietracker.domain.normalizeVoiceTranscript
import com.philipcosgrave.calorietracker.domain.nowIsoString
import com.philipcosgrave.calorietracker.domain.parseVoiceFoodCommand
import com.philipcosgrave.calorietracker.domain.parseVoiceMealCopyCommand
import com.philipcosgrave.calorietracker.domain.toFoodItem
import com.philipcosgrave.calorietracker.domain.toRecipeDraft
import com.philipcosgrave.calorietracker.domain.VoiceFoodCommand
import com.philipcosgrave.calorietracker.model.DiaryEntry
import com.philipcosgrave.calorietracker.model.DiaryEntryRecord
import com.philipcosgrave.calorietracker.model.FoodItem
import com.philipcosgrave.calorietracker.model.FoodKind
import com.philipcosgrave.calorietracker.model.HealthDashboardMetrics
import com.philipcosgrave.calorietracker.model.Meal
import com.philipcosgrave.calorietracker.model.MealCopyOptions
import com.philipcosgrave.calorietracker.model.Nutrients
import com.philipcosgrave.calorietracker.model.RecipeComponent
import com.philipcosgrave.calorietracker.model.RecipeDraft
import com.philipcosgrave.calorietracker.model.AuthSession
import com.philipcosgrave.calorietracker.model.SyncEntityType
import com.philipcosgrave.calorietracker.model.SyncOperation
import com.philipcosgrave.calorietracker.model.SyncSettings
import com.philipcosgrave.calorietracker.model.WeightEntry
import com.philipcosgrave.calorietracker.model.WeightEntryRecord
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

private const val AUTO_SYNC_INTERVAL_MILLIS = 5 * 60 * 1000L
private const val AUTO_HEALTH_CONNECT_SYNC_INTERVAL_MILLIS = 5 * 60 * 1000L

private enum class VoiceLogPhase {
    Idle,
    Listening,
    Heard,
    Parsing,
    Success,
    Failed,
    NeedsChoice,
}

private data class VoiceLogFeedback(
    val phase: VoiceLogPhase = VoiceLogPhase.Idle,
    val transcript: String? = null,
    val query: String? = null,
    val message: String? = null,
    val candidateMatches: List<FoodItem> = emptyList(),
)

private data class VoiceIngredientScore(
    val food: FoodItem,
    val score: Int,
)

private fun mergeSearchFoods(
    localFoods: List<FoodItem>,
    personalCloudFoods: List<FoodItem>,
): List<FoodItem> {
    val merged = LinkedHashMap<String, FoodItem>()
    localFoods.forEach { merged[it.id] = it }
    personalCloudFoods.forEach { cloudFood ->
        val duplicateLocal = localFoods.firstOrNull { localFood ->
            localFood.id == cloudFood.id ||
                (cloudFood.barcode.isNotBlank() && localFood.barcode == cloudFood.barcode) ||
                (
                    localFood.name.equals(cloudFood.name, ignoreCase = true) &&
                        localFood.brand.equals(cloudFood.brand, ignoreCase = true)
                )
        }
        merged[duplicateLocal?.id ?: cloudFood.id] = duplicateLocal ?: cloudFood
    }
    return merged.values.toList()
}

@Composable
fun CalorieTrackerApp(
    authCallbackUri: String? = null,
    onAuthCallbackConsumed: () -> Unit = {},
    openExternalUri: (String) -> Unit = {},
) {
    val localOnly = true // REST integration is deferred for the UI refactor.
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val database = remember { LocalRepositoryFactory.database(context) }
    val authRepository = remember { CognitoAuthRepository(context) }
    val localStore = remember {
        AndroidLocalStore(
            context = context,
            database = database,
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
    val photoAnalyzer = remember { com.philipcosgrave.calorietracker.data.local.LocalPhotoFoodAnalyzer() }
    val canadianNutrientFileLookupService = remember { CanadianNutrientFileLookupService() }
    val cloudFoodCatalogService = remember { CloudFoodCatalogService(localStore) }
    val openFoodFactsLookupService = remember { OpenFoodFactsLookupService() }
    val healthConnectExporter = remember { HealthConnectNutritionExporter(context) }
    val textToSpeech = remember(context) { TextToSpeech(context, null) }

    var customFoods by remember { mutableStateOf<List<FoodItem>>(emptyList()) }
    var recipes by remember { mutableStateOf<List<FoodItem>>(emptyList()) }
    var leftovers by remember { mutableStateOf<List<Leftover>>(emptyList()) }
    var diary by remember { mutableStateOf<List<DiaryEntry>>(emptyList()) }
    var weights by remember { mutableStateOf<List<WeightEntry>>(emptyList()) }
    var hiddenSeedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedDate by rememberSaveable(stateSaver = androidx.compose.runtime.saveable.Saver<LocalDate, String>(save = { it.toString() }, restore = { LocalDate.parse(it) })) { mutableStateOf(LocalDate.now()) }
    val screenStack = rememberSaveable(saver = listSaver<SnapshotStateList<AppScreen>, String>(
        save = { stack -> stack.map { it.name } },
        restore = { names -> names.map { AppScreen.valueOf(it) }.toMutableStateList() },
    )) { mutableStateListOf(AppScreen.Home) }
    val screen = screenStack.last()
    var lastAddedEntries by rememberSaveable(stateSaver = listSaver<List<DiaryEntry>, String>(
        save = { entries -> entries.map { it.toJsonString() } },
        restore = { entries -> entries.map { com.philipcosgrave.calorietracker.data.local.diaryEntryFromJsonString(it) } },
    )) { mutableStateOf(emptyList()) }
    var selectedFood by rememberSaveable(stateSaver = SelectedFoodSaver) { mutableStateOf<FoodItem?>(null) }
    var recipeEditorId by rememberSaveable { mutableStateOf<String?>(null) }
    var recipeDraft by rememberSaveable(stateSaver = RecipeDraftSaver) { mutableStateOf(RecipeDraft()) }
    var parentRecipeDraft by remember { mutableStateOf<RecipeDraft?>(null) }
    var returnToRecipeAfterIngredientSave by remember { mutableStateOf(false) }
    var editingFood by rememberSaveable(stateSaver = SelectedFoodSaver) { mutableStateOf<FoodItem?>(null) }
    var addIngredientBarcodeLookupResult by remember { mutableStateOf<FoodItem?>(null) }
    var isLookingUpAddIngredientBarcode by remember { mutableStateOf(false) }
    var editingDiaryEntry by rememberSaveable(stateSaver = DiaryEntrySaver) { mutableStateOf<DiaryEntry?>(null) }
    var editingWeight by remember { mutableStateOf<WeightEntry?>(null) }
    var openLogAfterIngredientSave by remember { mutableStateOf(false) }
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
    var personalOnlineResults by remember { mutableStateOf<List<FoodItem>>(emptyList()) }
    var communityResults by remember { mutableStateOf<List<FoodItem>>(emptyList()) }
    var canadianResults by remember { mutableStateOf<List<FoodItem>>(emptyList()) }
    var personalCloudFoods by remember { mutableStateOf<List<FoodItem>>(emptyList()) }
    var remoteSearchQuery by remember { mutableStateOf("") }
    var isSearchingRemote by remember { mutableStateOf(false) }
    var searchFoodInitialQuery by remember { mutableStateOf("") }
    var voiceFeedback by remember { mutableStateOf(VoiceLogFeedback()) }
    var pendingVoiceCommand by remember { mutableStateOf<VoiceFoodCommand?>(null) }
    var voiceReviewEntries by rememberSaveable(stateSaver = listSaver<List<DiaryEntry>, String>(save = { it.map { entry -> entry.toJsonString() } }, restore = { it.map(::diaryEntryFromJsonString) })) { mutableStateOf<List<DiaryEntry>>(emptyList()) }
    var capturedVoiceDrafts by rememberSaveable(stateSaver = DraftsSaver) { mutableStateOf<List<PhotoFoodDraft>>(emptyList()) }
    var spokenText by rememberSaveable { mutableStateOf("") }
    var acquisition by rememberSaveable(stateSaver = AcquisitionSaver) { mutableStateOf(AcquisitionDestination(selectedDate, Meal.Lunch)) }
    var acquisitionSaving by remember { mutableStateOf(false) }
    var acquisitionError by remember { mutableStateOf<String?>(null) }
    var acquisitionActive by rememberSaveable { mutableStateOf(false) }
    var acquisitionRecipeId by rememberSaveable { mutableStateOf<String?>(null) }
    var showAddSheet by rememberSaveable { mutableStateOf(false) }
    fun beginAcquisition(meal: Meal, recipe: Boolean = false) {
        lastAddedEntries = emptyList()
        editingDiaryEntry = null
        acquisitionError = null
        acquisition = AcquisitionDestination(selectedDate, meal, recipe)
        acquisitionRecipeId = if (recipe) recipeEditorId else null
        acquisitionActive = true
        showAddSheet = true
    }


    fun navigateTo(target: AppScreen) {
        if (screen != target) {
            screenStack.add(target)
        }
    }

    fun popScreen(): Boolean {
        if (screenStack.size > 1) {
            screenStack.removeAt(screenStack.lastIndex)
            return true
        }

        return false
    }

    fun resetTo(target: AppScreen) {
        screenStack.clear()
        screenStack.add(target)
    }

    fun clearVoiceFeedback() {
        voiceFeedback = VoiceLogFeedback()
        pendingVoiceCommand = null
    }

    fun openVoiceFallbackSearch(query: String, transcript: String, message: String) {
        searchFoodInitialQuery = query
        remoteSearchQuery = ""
        personalOnlineResults = emptyList()
        communityResults = emptyList()
        canadianResults = emptyList()
        isSearchingRemote = false
        voiceFeedback = VoiceLogFeedback(
            phase = VoiceLogPhase.Failed,
            transcript = transcript,
            query = query,
            message = message,
        )
        navigateTo(AppScreen.SearchFood)
    }

    suspend fun refreshState() {
        leftovers = database.leftoverDao().list(localStore.currentOwnerUserId()).map { it.toLeftover() }
        val foodRecords = localStore.foodRepository.list().filter { it.sync.deletedAt == null }
        customFoods = foodRecords.filter { it.food.kind == FoodKind.Ingredient }.map { it.food }
        recipes = foodRecords.filter { it.food.kind == FoodKind.Recipe }.map { it.food }
        diary = localStore.diaryRepository.list().filter { it.sync.deletedAt == null }.map { it.entry }
        weights = localStore.weightRepository.list(localStore.currentOwnerUserId())
        hiddenSeedIds = localStore.hiddenSeedIds()
        syncSettings = localStore.syncStateRepository.getSettings()
        pendingChangeCount = localStore.syncOutboxRepository.listPendingChanges().size
        authSession = localStore.currentAuthSession()
        personalCloudFoods =
            if (!localOnly && authSession != null) {
                runCatching { cloudFoodCatalogService.listPersonalFoods() }.getOrDefault(emptyList())
            } else {
                emptyList()
            }
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
                healthConnectExporter.requestedPermissions().all { it in grantedPermissions }
            if (granted) {
                localStore.setHealthConnectExportEnabled(true)
            }
            refreshState()
        }
    }

    fun levenshteinDistance(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }

        for (i in 0..a.length) {
            dp[i][0] = i
        }

        for (j in 0..b.length) {
            dp[0][j] = j
        }

        for (i in 1..a.length) {
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1

                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }

        return dp[a.length][b.length]
    }

    fun normalizeVoiceCandidatePhrase(value: String): String =
        normalizeVoiceSearchQuery(normalizeVoiceTranscript(value))

    fun voiceMatchPhrases(food: FoodItem): Set<String> {
        val phrases = linkedSetOf<String>()
        phrases += normalizeVoiceCandidatePhrase(food.name)
        if (food.brand.isNotBlank()) {
            phrases += normalizeVoiceCandidatePhrase("${food.brand} ${food.name}")
            phrases += normalizeVoiceCandidatePhrase("${food.name} ${food.brand}")
            phrases += normalizeVoiceCandidatePhrase(food.brand)
        }
        food.components.forEach { component ->
            phrases += normalizeVoiceCandidatePhrase(component.item.name)
            if (component.item.brand.isNotBlank()) {
                phrases += normalizeVoiceCandidatePhrase("${component.item.brand} ${component.item.name}")
            }
        }
        return phrases.filter { it.isNotBlank() }.toSet()
    }

    fun fuzzyVoiceThreshold(query: String): Int =
        when {
            query.length <= 4 -> 1
            query.length <= 7 -> 2
            query.length <= 12 -> 3
            else -> 4
        }

    fun scoreVoiceIngredient(food: FoodItem, normalizedQuery: String): Int? {
        val phrases = voiceMatchPhrases(food)
        if (phrases.any { it == normalizedQuery }) return 0
        if (phrases.any { it.contains(normalizedQuery) || normalizedQuery.contains(it) }) {
            return 20 + phrases.minOf { kotlin.math.abs(it.length - normalizedQuery.length) }
        }

        val threshold = fuzzyVoiceThreshold(normalizedQuery)
        val fuzzyDistance = phrases.minOfOrNull { phrase -> levenshteinDistance(normalizedQuery, phrase) } ?: return null
        return if (fuzzyDistance <= threshold) 100 + fuzzyDistance else null
    }

    fun findVoiceIngredientMatches(query: String): Pair<FoodItem?, List<FoodItem>> {
        val normalizedQuery = normalizeVoiceSearchQuery(query)
        val candidates = (customFoods + seedFoods).filter { it.kind == FoodKind.Ingredient }
        val scoredMatches = candidates
            .mapNotNull { food ->
                scoreVoiceIngredient(food, normalizedQuery)?.let { score ->
                    VoiceIngredientScore(food = food, score = score)
                }
            }
            .sortedWith(compareBy<VoiceIngredientScore> { it.score }.thenBy { it.food.name })

        if (scoredMatches.isEmpty()) return null to emptyList()

        val best = scoredMatches.first()
        val runnerUp = scoredMatches.getOrNull(1)
        val ambiguous =
            runnerUp != null &&
                (
                    runnerUp.score == best.score ||
                        (best.score < 100 && runnerUp.score - best.score <= 2) ||
                        (best.score >= 100 && runnerUp.score - best.score <= 1)
                )

        return if (ambiguous) {
            null to scoredMatches.take(3).map { it.food }
        } else {
            best.food to emptyList()
        }
    }

    fun mealCopyOptionsForDate(
        entries: List<DiaryEntry>,
        targetDate: LocalDate,
        meal: Meal,
    ): MealCopyOptions {
        val sourceDates = entries
            .asSequence()
            .filter { it.meal == meal && it.date.isBefore(targetDate) }
            .map { it.date }
            .distinct()
            .sortedDescending()
            .toList()

        return MealCopyOptions(
            previousDate = sourceDates.firstOrNull(),
            yesterdayDate = targetDate.minusDays(1).takeIf { it in sourceDates },
            selectableDates = sourceDates.take(10),
        )
    }

    suspend fun touchFoodUsage(foodId: String, meal: Meal) {
        val deviceId = localStore.deviceId()
        val existing = localStore.foodRepository.getById(foodId) ?: return
        val updatedFood = existing.food.copy(
            frequency = existing.food.frequency + 1,
            breakfastFrequency = existing.food.breakfastFrequency + if (meal == Meal.Breakfast) 1 else 0,
            lunchFrequency = existing.food.lunchFrequency + if (meal == Meal.Lunch) 1 else 0,
            dinnerFrequency = existing.food.dinnerFrequency + if (meal == Meal.Dinner) 1 else 0,
            snackFrequency = existing.food.snackFrequency + if (meal == Meal.Snack) 1 else 0,
            lastUsedAt = nowIsoString(),
            lastUsedDaysAgo = 0,
        )
        val updatedRecord = createFoodRecord(updatedFood, deviceId, existing)
        localStore.foodRepository.save(updatedRecord)
        localStore.syncOutboxRepository.enqueue(
            createChangeEnvelope(
                entityType = SyncEntityType.FoodProduct,
                operation = SyncOperation.Upsert,
                deviceId = deviceId,
                recordId = updatedRecord.sync.recordId,
                payload = updatedRecord,
                baseVersion = existing.sync.version,
            ),
        )
    }

    suspend fun saveDiaryEntry(entry: DiaryEntry, exportHealth: Boolean = true) {
        val deviceId = localStore.deviceId()
        val existing = localStore.diaryRepository.getById(entry.id)
        if (existing == null) {
            touchFoodUsage(entry.food.id, entry.meal)
        }
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
        if (exportHealth && healthConnectAvailability == HealthConnectAvailability.Available &&
            healthConnectPermissionGranted &&
            healthConnectExportEnabled
        ) {
            runCatching { healthConnectExporter.exportEntry(record) }
        }
    }

    suspend fun copyMealEntries(
        meal: Meal,
        sourceDate: LocalDate,
        targetDate: LocalDate,
    ): Int {
        val sourceEntries = diary
            .filter { it.meal == meal && it.date == sourceDate }
            .sortedBy { it.food.name }

        sourceEntries.forEach { sourceEntry ->
            saveDiaryEntry(
                sourceEntry.copy(
                    id = createId("entry"),
                    date = targetDate,
                    meal = meal,
                ),
            )
        }
        refreshState()
        return sourceEntries.size
    }

    suspend fun acceptAcquiredFoods(results: List<DiaryEntry>) {
        if (acquisitionActive && acquisition.recipe) {
            recipeDraft = recipeDraft.copy(components = recipeDraft.components + acquisitionIngredients(results))
        } else {
            val entries = results
            database.withTransaction { entries.forEach { saveDiaryEntry(it, exportHealth = false) } }
            lastAddedEntries = entries
            diary = entries + diary.filterNot { old -> entries.any { it.id == old.id } }
            selectedDate = entries.first().date
            scope.launch { entries.forEach { entry -> localStore.diaryRepository.getById(entry.id)?.let { record ->
                if (healthConnectPermissionGranted && healthConnectExportEnabled) runCatching { healthConnectExporter.exportEntry(record) }
            } } }
        }
    }
    fun finishAcquisition() {
        clearVoiceFeedback()
        if (acquisitionActive && acquisition.recipe) {
            while (screenStack.size > 1 && screenStack.last() != AppScreen.RecipeBuilder) screenStack.removeAt(screenStack.lastIndex)
            editingFood = recipes.firstOrNull { it.id == acquisitionRecipeId }
        } else resetTo(if (lastAddedEntries.isEmpty()) AppScreen.Diary else AppScreen.AddedFood)
        acquisitionActive = false
    }

    suspend fun logVoiceMatch(food: FoodItem, command: VoiceFoodCommand, transcript: String) {
        val servingQuantity = food.servingQuantity.coerceAtLeast(0.1)
        val normalizedAmount = when {
            command.amount == null && command.unit == null -> food.servingQuantity
            command.amount != null && command.unit == null -> command.amount * food.servingQuantity
            command.amount != null && command.unit != null -> {
                convertAmount(command.amount, command.unit, food.servingUnit)
                    ?: if (command.unit == food.servingUnit) command.amount else null
            }
            else -> null
        }

        if (normalizedAmount == null) {
            voiceFeedback = VoiceLogFeedback(
                phase = VoiceLogPhase.Failed,
                transcript = transcript,
                query = command.ingredientQuery,
                message = "I heard the food, but I couldn't convert that unit.",
            )
            return
        }

        val loggedAmount =
            when {
                command.amount == null && command.unit == null -> food.servingQuantity
                command.amount != null && command.unit == null -> normalizedAmount
                else -> command.amount ?: food.servingQuantity
            }
        val loggedUnit = command.unit ?: food.servingUnit
        val meal = command.mealOverride ?: inferMealForTime(LocalTime.now())
        val today = LocalDate.now()
        val entry = DiaryEntry(
            id = createId("entry"),
            food = food,
            date = today,
            meal = meal,
            servingMultiplier = normalizedAmount / servingQuantity,
            loggedAmount = loggedAmount,
            loggedUnit = loggedUnit,
        )
        voiceReviewEntries = listOf(entry)
        selectedDate = today
        clearVoiceFeedback()
        navigateTo(AppScreen.VoiceReview)
    }

    suspend fun handleVoiceMealCopyTranscript(transcript: String): Boolean {
        val command = parseVoiceMealCopyCommand(transcript, selectedDate) ?: return false
        val sourceDate = command.sourceDate ?: mealCopyOptionsForDate(diary, selectedDate, command.meal).previousDate
        if (sourceDate == null) {
            voiceFeedback = VoiceLogFeedback(
                phase = VoiceLogPhase.Failed,
                transcript = transcript,
                message = "I couldn't find a previous ${command.meal.label.lowercase(Locale.CANADA)} to copy.",
            )
            return true
        }
        val copiedCount = copyMealEntries(command.meal, sourceDate, selectedDate)
        voiceFeedback = VoiceLogFeedback(
            phase = VoiceLogPhase.Success,
            transcript = transcript,
            message = if (copiedCount > 0) {
                "Copied $copiedCount item${if (copiedCount == 1) "" else "s"} from ${command.meal.label.lowercase(Locale.CANADA)} on ${sourceDate.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${sourceDate.dayOfMonth}."
            } else {
                "I couldn't find any items in ${command.meal.label.lowercase(Locale.CANADA)} on ${sourceDate.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${sourceDate.dayOfMonth}."
            },
        )
        pendingVoiceCommand = null
        return true
    }

    suspend fun handleVoiceFoodTranscript(transcript: String) {
        if (acquisitionActive) {
            spokenText = transcript
            capturedVoiceDrafts = voiceReviewDrafts(transcript, customFoods + recipes + seedFoods)
            navigateTo(AppScreen.VoiceReview)
            return
        }
        voiceFeedback = VoiceLogFeedback(
            phase = VoiceLogPhase.Heard,
            transcript = transcript,
            message = "Heard that. Matching it now.",
        )
        if (handleVoiceMealCopyTranscript(transcript)) {
            return
        }
        val command = parseVoiceFoodCommand(transcript)
        if (command == null) {
            openVoiceFallbackSearch(
                query = normalizeVoiceTranscript(transcript),
                transcript = transcript,
                message = "I couldn't fully parse that phrase, so I opened food search with what I heard.",
            )
            return
        }

        voiceFeedback = VoiceLogFeedback(
            phase = VoiceLogPhase.Parsing,
            transcript = transcript,
            query = command.ingredientQuery,
            message = "Looking for ${command.ingredientQuery}.",
        )

        val (ingredient, candidateMatches) = findVoiceIngredientMatches(command.ingredientQuery)
        when {
            ingredient != null -> {
                pendingVoiceCommand = command
                logVoiceMatch(ingredient, command, transcript)
            }
            candidateMatches.isNotEmpty() -> {
                pendingVoiceCommand = command
                voiceFeedback = VoiceLogFeedback(
                    phase = VoiceLogPhase.NeedsChoice,
                    transcript = transcript,
                    query = command.ingredientQuery,
                    message = "I found a few close matches.",
                    candidateMatches = candidateMatches,
                )
            }
            else -> {
                textToSpeech.speak("Unknown ingredient", TextToSpeech.QUEUE_FLUSH, null, "unknown-ingredient")
                openVoiceFallbackSearch(
                    query = command.ingredientQuery,
                    transcript = transcript,
                    message = "Unknown ingredient. I opened search with what I heard.",
                )
            }
        }
    }

    val speechRecognitionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            voiceFeedback = VoiceLogFeedback(
                phase = VoiceLogPhase.Failed,
                message = "Voice capture was canceled.",
            )
            return@rememberLauncherForActivityResult
        }
        val spokenText = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            .orEmpty()
        if (spokenText.isBlank()) {
            voiceFeedback = VoiceLogFeedback(
                phase = VoiceLogPhase.Failed,
                message = "I didn't catch that. Try again or cancel.",
            )
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            handleVoiceFoodTranscript(spokenText)
        }
    }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (!granted) {
            voiceFeedback = VoiceLogFeedback(
                phase = VoiceLogPhase.Failed,
                message = "Microphone permission is required for voice logging.",
            )
            return@rememberLauncherForActivityResult
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.CANADA.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say something like: add 30 grams onion or repeat breakfast")
        }
        speechRecognitionLauncher.launch(intent)
    }

    val launchVoiceRecognition = {
        voiceFeedback = VoiceLogFeedback(
            phase = VoiceLogPhase.Listening,
            message = "Listening. Try saying: add 30 grams onion or repeat breakfast.",
        )
        recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    suspend fun ensureSeedRecipes(deviceId: String) {
        if (localStore.foodRepository.list().any { it.food.kind == FoodKind.Recipe }) return
        seedRecipes().forEach { recipe ->
            localStore.foodRepository.save(createFoodRecord(recipe, deviceId))
        }
    }

    suspend fun saveFood(rawItem: FoodItem, publishCommunity: Boolean = true) {
        val item = rawItem.copy(name = foodTitle(rawItem.name))
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

        if (!localOnly && publishCommunity && existing == null && item.isUserCreated) {
            runCatching { cloudFoodCatalogService.publishCommunityFood(item) }
        }
    }

    fun beginImportRemoteFood(item: FoodItem, openLogAfterSave: Boolean) {
        addIngredientBarcodeLookupResult = null
        isLookingUpAddIngredientBarcode = false
        editingFood = item.copy(id = createId("custom"))
        openLogAfterIngredientSave = openLogAfterSave
        navigateTo(AppScreen.AddIngredient)
    }

    suspend fun searchOnlineFoods(query: String) {
        remoteSearchQuery = query.trim()
        if (remoteSearchQuery.length < 2) {
            personalOnlineResults = emptyList()
            communityResults = emptyList()
            canadianResults = emptyList()
            return
        }

        isSearchingRemote = true
        val signedIn = authSession != null

        val personal = if (signedIn) runCatching {
            cloudFoodCatalogService.searchPersonalFoods(remoteSearchQuery)
        }.getOrDefault(emptyList()) else emptyList()

        val community = runCatching {
            cloudFoodCatalogService.searchCommunityFoods(remoteSearchQuery)
        }.getOrDefault(emptyList())

        val canadian = if (personal.isEmpty() && community.isEmpty()) {
            canadianNutrientFileLookupService.searchFoodsByName(remoteSearchQuery)
        } else {
            emptyList()
        }

        personalOnlineResults = personal
        communityResults = community
        canadianResults = canadian
        isSearchingRemote = false
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

    suspend fun deleteDiaryEntry(entry: DiaryEntry, exportHealth: Boolean = true) {
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
        if (exportHealth && healthConnectAvailability == HealthConnectAvailability.Available &&
            healthConnectPermissionGranted &&
            healthConnectExportEnabled
        ) {
            runCatching { healthConnectExporter.deleteEntry(entry.id) }
        }
    }

    fun exportLeftoverChanges(updated: List<DiaryEntry>, removed: List<DiaryEntry> = emptyList()) {
        scope.launch {
            if (healthConnectAvailability == HealthConnectAvailability.Available && healthConnectPermissionGranted && healthConnectExportEnabled) {
                removed.forEach { runCatching { healthConnectExporter.deleteEntry(it.id) } }
                updated.forEach { entry ->
                    localStore.diaryRepository.getById(entry.id)?.let { record -> runCatching { healthConnectExporter.exportEntry(record) } }
                }
            }
        }
    }

    suspend fun createLeftover(selected: List<DiaryEntry>, percentage: Double, name: String) {
        val split = splitLeftover(selected, percentage, name)
        val owner = localStore.currentOwnerUserId()
        val removed = if (split.remaining.isEmpty()) selected else emptyList()
        database.withTransaction {
            selected.forEach { expected ->
                val current = localStore.diaryRepository.getById(expected.id)
                check(current != null && current.sync.deletedAt == null && current.entry == expected) {
                    "The meal changed. Close this dialog and select the foods again."
                }
            }
            database.leftoverDao().insert(split.leftover.toRecord(owner))
            split.remaining.forEach { saveDiaryEntry(it, exportHealth = false) }
            removed.forEach { deleteDiaryEntry(it, exportHealth = false) }
        }
        val selectedIds = selected.map { it.id }.toSet()
        diary = diary.filterNot { it.id in selectedIds } + split.remaining
        leftovers = listOf(split.leftover) + leftovers
        exportLeftoverChanges(split.remaining, removed)
    }

    suspend fun useLeftover(leftover: Leftover, date: LocalDate, meal: Meal) {
        val owner = localStore.currentOwnerUserId()
        val added = database.withTransaction {
            val stored = checkNotNull(database.leftoverDao().get(leftover.id, owner)) { "This leftover has already been used." }.toLeftover()
            val entries = leftoverDiaryEntries(stored, date, meal)
            entries.forEach { saveDiaryEntry(it, exportHealth = false) }
            check(database.leftoverDao().delete(stored.id, owner) == 1)
            entries
        }
        lastAddedEntries = added
        diary = added + diary
        leftovers = leftovers.filterNot { it.id == leftover.id }
        selectedDate = date
        exportLeftoverChanges(added)
    }

    suspend fun saveWeightEntry(entry: WeightEntry): String? {
        val deviceId = localStore.deviceId()
        val weightEntry = entry.copy(id = entry.id.ifBlank { createId("weight") })
        val existing = localStore.weightRepository.getById(localStore.currentOwnerUserId(), weightEntry.id)
        localStore.weightRepository.save(
            localStore.currentOwnerUserId(),
            weightEntry,
        )
        val updatedAt = nowIsoString()
        val weightRecord = WeightEntryRecord(
            entry = weightEntry,
            sync = createSyncMetadata(
                recordId = weightEntry.id,
                deviceId = deviceId,
                updatedAt = updatedAt,
                version = existing?.let { 2 } ?: 1,
            ).copy(syncStatus = com.philipcosgrave.calorietracker.model.SyncStatus.PendingPush),
        )
        localStore.syncOutboxRepository.enqueue(
            createChangeEnvelope(
                entityType = SyncEntityType.WeightEntry,
                operation = SyncOperation.Upsert,
                deviceId = deviceId,
                recordId = weightRecord.sync.recordId,
                payload = weightRecord,
                baseVersion = if (existing != null) 1 else null,
            ),
        )
        if (healthConnectAvailability == HealthConnectAvailability.Available && healthConnectExportEnabled) {
            val hasWriteWeightPermission = healthConnectExporter.hasWriteWeightPermission()
            if (hasWriteWeightPermission) {
                val exportResult = runCatching { healthConnectExporter.exportWeightEntry(weightEntry) }
                exportResult.exceptionOrNull()?.let { error ->
                    Log.e("HealthConnectWeight", "Failed to export weight entry ${weightEntry.id}", error)
                    return "Weight saved locally. Health Connect sync failed."
                }
                return null
            }
            Log.w(
                "HealthConnectWeight",
                "Skipped weight export for ${weightEntry.id} because WRITE_WEIGHT permission is not granted",
            )
            return "Weight saved locally. Reconnect Health Connect to sync it."
        }
        return null
    }

    suspend fun deleteWeightEntry(entry: WeightEntry) {
        val existing = localStore.weightRepository.getById(localStore.currentOwnerUserId(), entry.id) ?: return
        val deletedAt = nowIsoString()
        localStore.weightRepository.delete(
            localStore.currentOwnerUserId(),
            entry.id,
        )
        localStore.syncOutboxRepository.enqueue(
            createChangeEnvelope(
                entityType = SyncEntityType.WeightEntry,
                operation = SyncOperation.Delete,
                deviceId = localStore.deviceId(),
                recordId = entry.id,
                payload = WeightEntryRecord(
                    entry = existing,
                    sync = createSyncMetadata(
                        recordId = entry.id,
                        deviceId = localStore.deviceId(),
                        updatedAt = deletedAt,
                        version = 2,
                        deletedAt = deletedAt,
                    ).copy(syncStatus = com.philipcosgrave.calorietracker.model.SyncStatus.PendingPush),
                ),
                baseVersion = 1,
            ),
        )
        if (healthConnectAvailability == HealthConnectAvailability.Available && healthConnectExportEnabled) {
            if (healthConnectExporter.hasWriteWeightPermission()) {
                runCatching { healthConnectExporter.deleteWeightEntry(entry.id) }
                    .onFailure { error ->
                        Log.e("HealthConnectWeight", "Failed to delete weight entry ${entry.id}", error)
                    }
            } else {
                Log.w(
                    "HealthConnectWeight",
                    "Skipped deleting weight entry ${entry.id} from Health Connect because WRITE_WEIGHT permission is not granted",
                )
            }
        }
    }

    suspend fun importWeightHistoryFromHealthConnect(): Pair<Int, Int> {
        val ownerUserId = localStore.currentOwnerUserId()
        val existing = localStore.weightRepository.list(ownerUserId)
        val existingKeys = existing.map { "${it.date}:${it.weightKg}" }.toSet()
        val importedEntries = healthConnectExporter.importWeightEntries()
        val newEntries = importedEntries.filterNot { "${it.date}:${it.weightKg}" in existingKeys }
        newEntries.forEach { entry ->
                localStore.weightRepository.save(ownerUserId, entry)
        }
        return newEntries.size to (importedEntries.size - newEntries.size)
    }

    suspend fun importNutritionHistoryFromHealthConnect(): Pair<Int, Int> {
        val importedEntries = healthConnectExporter.importNutritionEntries()
        val existingIds = localStore.diaryRepository.list().map { it.entry.id }.toSet()
        val newEntries = importedEntries.filterNot { it.id in existingIds }
        newEntries.forEach { entry ->
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
        return newEntries.size to (importedEntries.size - newEntries.size)
    }

    suspend fun findFoodByBarcode(barcode: String): FoodItem? {
        localStore.foodRepository.getByBarcode(barcode)?.food?.let { return it }
        localStore.barcodeAliasRepository.getByBarcode(barcode)?.let { alias ->
            localStore.foodRepository.getById(alias.productId)?.food?.let { return it }
        }
        return seedFoods.firstOrNull { it.barcode == barcode }
    }

    suspend fun lookupFoodForBarcode(barcode: String): FoodItem? {
        if (localOnly) return (customFoods + recipes + seedFoods).firstOrNull { it.barcode == barcode }
        val normalizedBarcode = barcode.trim()
        if (normalizedBarcode.isBlank()) return null
        return findFoodByBarcode(normalizedBarcode)
            ?: if (authSession != null) {
                runCatching { cloudFoodCatalogService.lookupPersonalBarcode(normalizedBarcode) }.getOrNull()
            } else {
                null
            }
            ?: runCatching { cloudFoodCatalogService.lookupCommunityBarcode(normalizedBarcode) }.getOrNull()
            ?: runCatching { openFoodFactsLookupService.lookupFoodByBarcode(normalizedBarcode) }.getOrNull()
    }

    suspend fun autoSyncIfNeeded() {
        if (localOnly) return
        val latestSettings = localStore.syncStateRepository.getSettings()
        val hasSession = localStore.currentAuthSession() != null
        val hasPendingChanges = localStore.syncOutboxRepository.listPendingChanges().isNotEmpty()
        if (
            latestSettings.syncEnabled &&
            latestSettings.backupMode == SyncSettings.BackupMode.AutomaticBackup &&
            !latestSettings.apiBaseUrl.isNullOrBlank() &&
            hasSession &&
            hasPendingChanges
        ) {
            runCatching { syncService.syncNow() }
            refreshState()
        }
    }

    suspend fun autoHealthConnectSyncIfNeeded() {
        val latestAvailability = healthConnectExporter.availability()
        val latestPermissionsGranted =
            latestAvailability == HealthConnectAvailability.Available &&
                healthConnectExporter.hasRequestedPermissions()
        val latestExportEnabled = localStore.isHealthConnectExportEnabled()

        if (
            latestAvailability != HealthConnectAvailability.Available ||
            !latestPermissionsGranted ||
            !latestExportEnabled
        ) {
            return
        }

        val (importedWeights, _) = importWeightHistoryFromHealthConnect()
        val (importedNutrition, _) = importNutritionHistoryFromHealthConnect()

        if (importedWeights > 0 || importedNutrition > 0 || screen == AppScreen.Home) {
            refreshState()
        }
    }

    LaunchedEffect(Unit) {
        localStore.migrateLegacyIfNeeded(
            readLegacyFoods = ::readFoodItems,
            readLegacyDiary = ::readDiaryEntries,
            readLegacyStrings = ::readStringList,
        )
        ensureSeedRecipes(localStore.deviceId())
        refreshState()
        autoHealthConnectSyncIfNeeded()
        autoSyncIfNeeded()
    }

    LaunchedEffect(syncSettings.syncEnabled, syncSettings.backupMode, syncSettings.apiBaseUrl, authSession?.userSub) {
        while (true) {
            delay(AUTO_SYNC_INTERVAL_MILLIS)
            autoSyncIfNeeded()
        }
    }

    LaunchedEffect(healthConnectAvailability, healthConnectPermissionGranted, healthConnectExportEnabled) {
        while (true) {
            delay(AUTO_HEALTH_CONNECT_SYNC_INTERVAL_MILLIS)
            autoHealthConnectSyncIfNeeded()
        }
    }

    LaunchedEffect(authCallbackUri) {
        val callback = authCallbackUri ?: return@LaunchedEffect
        if (callback.startsWith(BuildConfig.COGNITO_ANDROID_REDIRECT_URI)) {
            runCatching {
                authRepository.completeSignIn(android.net.Uri.parse(callback))
            }.onSuccess {
                resetTo(AppScreen.SyncSettings)
            }.onFailure { error ->
                Log.e("AuthCallback", "Sign in failed for callback: $callback", error)
                Toast.makeText(
                    context,
                    error.message ?: "Sign in failed",
                    Toast.LENGTH_LONG,
                ).show()
                navigateTo(AppScreen.SyncSettings)
            }
        } else if (callback.startsWith(BuildConfig.COGNITO_ANDROID_LOGOUT_URI)) {
            resetTo(AppScreen.SyncSettings)
        }
        refreshState()
        onAuthCallbackConsumed()
    }

    LaunchedEffect(screen) {
        if (screen == AppScreen.Home) {
            refreshState()
        }
    }

    LaunchedEffect(voiceFeedback.phase, voiceFeedback.transcript, voiceFeedback.message) {
        if (voiceFeedback.phase == VoiceLogPhase.Success) {
            delay(5000)
            if (voiceFeedback.phase == VoiceLogPhase.Success) {
                clearVoiceFeedback()
            }
        }
    }

    androidx.compose.runtime.DisposableEffect(lifecycleOwner, screen) {
        textToSpeech.language = Locale.CANADA
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && screen == AppScreen.Home) {
                scope.launch {
                    refreshState()
                    autoHealthConnectSyncIfNeeded()
                }
            } else if (event == Lifecycle.Event.ON_STOP) {
                scope.launch {
                    autoHealthConnectSyncIfNeeded()
                    autoSyncIfNeeded()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }

    BackHandler(enabled = screenStack.size > 1) {
        when (screen) {
            AppScreen.LogWeight -> editingWeight = null
            AppScreen.AddIngredient -> {
                editingFood = null
                returnToRecipeAfterIngredientSave = false
            }
            AppScreen.RecipeBuilder -> {
                parentRecipeDraft = null
                returnToRecipeAfterIngredientSave = false
                editingFood = null
            }
            else -> Unit
        }

        popScreen()
    }

    val mealCopyOptionsByMeal = remember(diary, selectedDate) {
        Meal.entries.associateWith { meal ->
            mealCopyOptionsForDate(diary, selectedDate, meal)
        }
    }

    if (showAddSheet) AddFoodBottomSheet(
        context = if (acquisition.recipe) "Recipe ingredient" else "${acquisition.meal.label} · ${acquisition.date}",
        onDismiss = { showAddSheet = false; acquisitionActive = false },
        onSearch = { showAddSheet = false; navigateTo(AppScreen.SearchFood) },
        onManual = { showAddSheet = false; navigateTo(AppScreen.QuickCalories) },
        onCamera = { showAddSheet = false; navigateTo(AppScreen.PhotoFood) },
        onVoice = { showAddSheet = false; spokenText = ""; navigateTo(AppScreen.SpeakFood) },
    )

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        color = MaterialTheme.colorScheme.background,
    ) {
        val today = LocalDate.now()
        Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
        when (screen) {
            AppScreen.AddedFood -> com.philipcosgrave.calorietracker.ui.screens.FoodConfirmationScreen(
                title = "Added to ${lastAddedEntries.firstOrNull()?.meal?.label ?: "meal"}",
                foods = lastAddedEntries.map { it.food to "${formatNumber(it.loggedAmount)} ${it.loggedUnit} · ${formatNumber(it.food.nutrients.calories * it.servingMultiplier)} kcal" },
                primaryLabel = "Add Another Item", onPrimary = { lastAddedEntries = emptyList(); resetTo(AppScreen.Diary); beginAcquisition(inferMealForTime(LocalTime.now())) },
                onDone = { lastAddedEntries = emptyList(); resetTo(AppScreen.Diary) },
            )
            AppScreen.FoodCreated -> selectedFood?.let { food -> com.philipcosgrave.calorietracker.ui.screens.FoodConfirmationScreen(
                title = if (food.kind == FoodKind.Recipe) "Recipe saved!" else "Food created",
                foods = listOf(food to "Saved to your ${if (food.kind == FoodKind.Recipe) "recipes" else "foods"}"),
                primaryLabel = if (acquisitionActive && acquisition.recipe) "Add to Recipe" else "Add to a Meal",
                onPrimary = { editingDiaryEntry = null; navigateTo(AppScreen.LogFood) },
                secondaryLabel = if (food.kind == FoodKind.Recipe) "View Recipe" else "Done",
                onDone = { if (food.kind == FoodKind.Recipe) navigateTo(AppScreen.FoodDetails) else if (acquisitionActive && acquisition.recipe) finishAcquisition() else { acquisitionActive = false; popScreen() } },
            ) }
            AppScreen.CopyMeals -> com.philipcosgrave.calorietracker.ui.screens.CopyMealsScreen(
                targetDate = selectedDate, entries = diary, onBack = { popScreen() },
                onCopy = { sourceDate, selectedMeals ->
                    val copied = diary.filter { it.date == sourceDate && it.meal in selectedMeals }.map { it.copy(id = createId("entry"), date = selectedDate) }
                    database.withTransaction { copied.forEach { saveDiaryEntry(it, exportHealth = false) } }
                    diary = copied + diary; exportLeftoverChanges(copied); popScreen()
                },
            )
            AppScreen.SpeakFood -> SpeakFoodScreen(
                onReview = { transcript -> scope.launch { handleVoiceFoodTranscript(transcript) } },
                onBack = { popScreen() },
            )
            AppScreen.Scan -> Page {
                Text("Scan", style = MaterialTheme.typography.headlineLarge)
                AppCardContainer { Text("Shop with confidence", style = MaterialTheme.typography.headlineSmall); Text("Product and ingredient suitability checks are coming later. To log a meal, use Camera from Add Food in the Log tab.") }
            }
            AppScreen.Insights -> Page {
                Text("Insights", style = MaterialTheme.typography.headlineLarge)
                Text("Your recorded nutrition and weight")
                DatePillsRow(selectedDate, today, { selectedDate = it })
                TotalsGrid(com.philipcosgrave.calorietracker.domain.totalsForEntries(diary.filter { it.date == selectedDate }))
                TextButton(onClick = { navigateTo(AppScreen.Weight) }) { Text("View weight trends") }
            }
            AppScreen.More -> Page {
                Text("More", style = MaterialTheme.typography.headlineLarge)
                AppPrimaryButton("Settings", { navigateTo(AppScreen.SyncSettings) }, Modifier.fillMaxWidth())
                AppPrimaryButton("Foods & recipes", { navigateTo(AppScreen.ManageFoods) }, Modifier.fillMaxWidth())
                AppPrimaryButton("Leftovers", { navigateTo(AppScreen.Leftovers) }, Modifier.fillMaxWidth())
            }
            AppScreen.Home -> HomeScreen(
                caloriesLogged = diary.filter { it.date == selectedDate }.sumOf {
                    it.food.nutrients.calories * it.servingMultiplier
                },
                selectedDate = selectedDate, onDateChange = { selectedDate = it }, calorieTarget = syncSettings.calorieTargetMax,
                healthMetrics = if (selectedDate == today) healthMetrics else HealthDashboardMetrics(),
                goalWeightKg = syncSettings.goalWeightKg, dailyStepGoal = syncSettings.dailyStepGoal,
                startWeightKg = weights.filter { !it.date.isAfter(selectedDate) }.minByOrNull { it.date }?.weightKg,
                latestWeightKg = weights.filter { !it.date.isAfter(selectedDate) }.maxByOrNull { it.date }?.weightKg,
                weightUnit = syncSettings.weightUnit,
                onOpenFoodLog = {
                    navigateTo(AppScreen.Diary)
                },
                onOpenWeight = { navigateTo(AppScreen.Weight) },
                onOpenSyncSettings = {
                    navigateTo(AppScreen.SyncSettings)
                },
            )

            AppScreen.Diary -> DiaryScreen(
                selectedDate = selectedDate,
                entries = diary,
                targetRangeMin = syncSettings.calorieTargetMin,
                targetRangeMax = syncSettings.calorieTargetMax,
                onDateChange = { selectedDate = it },
                onBack = { popScreen() },
                onAddFood = { clearVoiceFeedback(); beginAcquisition(inferMealForTime(LocalTime.now())) },
                onOpenCopyMeals = { navigateTo(AppScreen.CopyMeals) },
                onAddMealFood = { clearVoiceFeedback(); beginAcquisition(it) },
                onCopyDay = { sourceDate ->
                    val copied = diary.filter { it.date == sourceDate }.map { it.copy(id = createId("entry"), date = selectedDate) }
                    database.withTransaction { copied.forEach { saveDiaryEntry(it, exportHealth = false) } }
                    diary = copied + diary
                    exportLeftoverChanges(copied)
                },
                onVoiceLog = launchVoiceRecognition,
                onPhotoLog = { navigateTo(AppScreen.PhotoFood) },
                onOpenLeftovers = { navigateTo(AppScreen.Leftovers) },
                leftoverCount = leftovers.size,
                onCreateLeftover = { entries, percent, name -> createLeftover(entries, percent, name) },
                onOpenSyncSettings = {
                    navigateTo(AppScreen.SyncSettings)
                },
                onDeleteEntry = { entry ->
                    diary = diary.filterNot { it.id == entry.id }
                    scope.launch {
                        deleteDiaryEntry(entry)
                        refreshState()
                    }
                },
                onEditEntry = { entry ->
                    acquisitionActive = false
                    editingDiaryEntry = entry
                    selectedFood = entry.food
                    navigateTo(AppScreen.LogFood)
                },
                mealCopyOptions = mealCopyOptionsByMeal,
                onCopyMealFromDate = { meal, sourceDate ->
                    scope.launch {
                        val copiedCount = copyMealEntries(meal, sourceDate, selectedDate)
                        Toast.makeText(
                            context,
                            if (copiedCount > 0) {
                                "Copied $copiedCount item${if (copiedCount == 1) "" else "s"} from ${meal.label.lowercase(Locale.CANADA)}."
                            } else {
                                "No items found to copy from that ${meal.label.lowercase(Locale.CANADA)}."
                            },
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                },
                onCopyMealToToday = { meal ->
                    scope.launch {
                        val copiedCount = copyMealEntries(meal, selectedDate, LocalDate.now())
                        Toast.makeText(
                            context,
                            if (copiedCount > 0) {
                                "Copied $copiedCount item${if (copiedCount == 1) "" else "s"} to today’s ${meal.label.lowercase(Locale.CANADA)}."
                            } else {
                                "No items found to copy from that ${meal.label.lowercase(Locale.CANADA)}."
                            },
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                },
                voiceTranscript = voiceFeedback.transcript,
                voiceStatusLabel = when (voiceFeedback.phase) {
                    VoiceLogPhase.Listening -> "Listening"
                    VoiceLogPhase.Heard -> "Heard"
                    VoiceLogPhase.Parsing -> "Parsing"
                    VoiceLogPhase.Success -> "Logged"
                    VoiceLogPhase.Failed -> "Couldn't log that"
                    VoiceLogPhase.NeedsChoice -> "Need a match"
                    VoiceLogPhase.Idle -> null
                },
                voiceStatusMessage = voiceFeedback.message,
                voiceRetryVisible = voiceFeedback.phase == VoiceLogPhase.Failed,
                voiceCandidateMatches = voiceFeedback.candidateMatches,
                onRetryVoiceLog = launchVoiceRecognition,
                onDismissVoiceFeedback = ::clearVoiceFeedback,
                onSelectVoiceCandidate = { food ->
                    val command = pendingVoiceCommand
                    if (command != null) {
                        scope.launch { logVoiceMatch(food, command, voiceFeedback.transcript.orEmpty()) }
                    } else {
                        clearVoiceFeedback()
                    }
                },
            )

            AppScreen.Weight -> WeightScreen(
                weights = weights,
                weightUnit = syncSettings.weightUnit,
                goalWeightKg = syncSettings.goalWeightKg,
                onBack = { popScreen() },
                onLogWeight = {
                    editingWeight = null
                    navigateTo(AppScreen.LogWeight)
                },
                onEditWeight = { entry ->
                    editingWeight = entry
                    selectedDate = entry.date
                    navigateTo(AppScreen.LogWeight)
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
                    popScreen()
                },
                onSave = { entry ->
                    selectedDate = entry.date
                    scope.launch {
                        val warning = saveWeightEntry(entry)
                        refreshState()
                        warning?.let {
                            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                        }
                    }
                    editingWeight = null
                    popScreen()
                },
            )

            AppScreen.SearchFood, AppScreen.Leftovers -> SearchFoodScreen(
                leftovers = leftovers, initialLeftovers = screen == AppScreen.Leftovers,
                allowLeftovers = !(acquisitionActive && acquisition.recipe),
                destinationMeal = if (acquisitionActive) acquisition.meal else null,
                onUseLeftover = { leftover, date, meal -> useLeftover(leftover, date, meal); finishAcquisition() },
                onOpenLeftovers = { navigateTo(AppScreen.Leftovers) },
                date = selectedDate,
                foods = mergeSearchFoods(
                    localFoods = customFoods + recipes + seedFoods.filterNot { hiddenSeedIds.contains(it.id) },
                    personalCloudFoods = personalCloudFoods,
                ),
                isSignedIn = authSession != null,
                initialSearchQuery = searchFoodInitialQuery,
                voiceSearchNotice =
                    if (voiceFeedback.phase == VoiceLogPhase.Failed && !voiceFeedback.transcript.isNullOrBlank()) {
                        "Showing search results for what voice logging heard."
                    } else {
                        null
                    },
                onBack = {
                    searchFoodInitialQuery = ""
                    popScreen()
                },
                onOpenSyncSettings = {
                    navigateTo(AppScreen.SyncSettings)
                },
                onQuickCalories = { navigateTo(AppScreen.QuickCalories) },
                onScanBarcode = {
                    navigateTo(AppScreen.BarcodeScanner)
                },
                onAddIngredient = {
                    addIngredientBarcodeLookupResult = null
                    isLookingUpAddIngredientBarcode = false
                    editingFood = null
                    returnToRecipeAfterIngredientSave = false
                    navigateTo(AppScreen.AddIngredient)
                },
                onAddRecipe = {
                    editingFood = null
                    recipeDraft = RecipeDraft()
                    parentRecipeDraft = null
                    recipeEditorId = editingFood?.id; navigateTo(AppScreen.RecipeBuilder)
                },
                onImportReference = { reference ->
                    val existingReference = customFoods.firstOrNull { it.source == reference.source && it.sourceId == reference.sourceId }
                    if (existingReference != null) existingReference else {
                        val imported = reference.copy(id = com.philipcosgrave.calorietracker.domain.createId("custom"))
                        database.withTransaction { saveFood(imported, publishCommunity = false) }
                        customFoods = (listOf(imported) + customFoods).distinctBy { it.id }
                        imported
                    }
                },
                onPhoto = { navigateTo(AppScreen.PhotoFood) },
                onVoice = { navigateTo(AppScreen.SpeakFood) },
                onSelectFood = {
                    searchFoodInitialQuery = ""
                    clearVoiceFeedback()
                    selectedFood = it
                    navigateTo(AppScreen.FoodDetails)
                },
                onQuickLogFood = { food -> selectedFood = food; navigateTo(AppScreen.LogFood) },
                personalOnlineResults = personalOnlineResults,
                communityResults = communityResults,
                canadianResults = canadianResults,
                remoteSearchQuery = remoteSearchQuery,
                isSearchingRemote = isSearchingRemote,
                onSearchOnlineFoods = { query -> scope.launch { searchOnlineFoods(query) } },
                onImportRemoteFood = { item ->
                    clearVoiceFeedback()
                    beginImportRemoteFood(item, openLogAfterSave = true)
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
                        recipeEditorId = editingFood?.id; navigateTo(AppScreen.RecipeBuilder)
                    } else {
                        addIngredientBarcodeLookupResult = null
                        isLookingUpAddIngredientBarcode = false
                        editingFood = item
                        navigateTo(AppScreen.AddIngredient)
                    }
                },
                onPublishToCommunity = { item ->
                    scope.launch {
                        runCatching { cloudFoodCatalogService.publishCommunityFood(item) }
                    }
                },
            )

            AppScreen.PhotoFood, AppScreen.VoiceReview -> PhotoFoodScreen(
                onAnalyzePhoto = { bitmap, foods, status -> photoAnalyzer.analyze(bitmap, foods, status) },
                onAnalyzeLabel = { bitmap, status -> photoAnalyzer.analyzeLabel(bitmap, status) },
                date = selectedDate,
                reviewOnly = screen == AppScreen.VoiceReview,
                initialDrafts = if (screen == AppScreen.VoiceReview && acquisitionActive) capturedVoiceDrafts else emptyList(),
                initialEntries = if (screen == AppScreen.VoiceReview) voiceReviewEntries else emptyList(),
                foods = mergeSearchFoods(customFoods + recipes + seedFoods.filterNot { it.id in hiddenSeedIds }, personalCloudFoods),
                onBack = { popScreen() },
                onLookupBarcode = { barcode -> lookupFoodForBarcode(barcode) },
                onCreateFood = { food ->
                    database.withTransaction { saveFood(food, publishCommunity = false) }
                    customFoods = (listOf(food) + customFoods).distinctBy { it.id }
                },
                destinationMeal = if (acquisitionActive) acquisition.meal else null,
                recipeDestination = acquisitionActive && acquisition.recipe,
                onSave = { entries -> acceptAcquiredFoods(entries) },
                onSaved = { voiceReviewEntries = emptyList(); capturedVoiceDrafts = emptyList(); finishAcquisition() },
            )

            AppScreen.ManageFoods -> ManageFoodsScreen(
                foods = (customFoods + recipes + seedFoods.filterNot { it.id in hiddenSeedIds }).distinctBy { it.id },
                onBack = { popScreen() },
                onAddFood = {
                    editingFood = null
                    addIngredientBarcodeLookupResult = null
                    openLogAfterIngredientSave = false
                    returnToRecipeAfterIngredientSave = false
                    navigateTo(AppScreen.AddIngredient)
                },
                onAddRecipe = {
                    editingFood = null
                    recipeDraft = RecipeDraft()
                    parentRecipeDraft = null
                    recipeEditorId = editingFood?.id; navigateTo(AppScreen.RecipeBuilder)
                },
                onEdit = { food -> selectedFood = food; navigateTo(AppScreen.FoodDetails) },
                onDelete = { food ->
                    scope.launch {
                        try {
                            if (customFoods.any { it.id == food.id } || recipes.any { it.id == food.id }) deleteFood(food)
                            if (seedFoods.any { it.id == food.id }) {
                                hiddenSeedIds = hiddenSeedIds + food.id
                                localStore.saveHiddenSeedIds(hiddenSeedIds)
                            }
                            refreshState()
                        } catch (exception: Exception) {
                            Toast.makeText(context, "Could not delete the food. Please try again.", Toast.LENGTH_LONG).show()
                        }
                    }
                },
            )

            AppScreen.BarcodeScanner -> BarcodeScannerScreen(
                onBack = { popScreen() },
                onBarcodeDetected = { barcode ->
                    val found = findFoodByBarcode(barcode)
                    val remote = if (found == null) lookupFoodForBarcode(barcode) else null
                    currentCoroutineContext().ensureActive()
                    val food = found ?: remote?.copy(id = createId("custom"), barcode = barcode, isUserCreated = false)?.also { imported ->
                        database.withTransaction { saveFood(imported, publishCommunity = false) }
                        customFoods = listOf(imported) + customFoods
                    }
                    if (food != null) {
                        selectedFood = food
                        popScreen()
                        navigateTo(AppScreen.LogFood)
                    } else {
                        addIngredientBarcodeLookupResult = null
                        isLookingUpAddIngredientBarcode = false
                        editingFood = FoodItem(
                            id = createId("custom"), kind = FoodKind.Ingredient,
                            name = "", barcode = barcode, servingQuantity = 1.0,
                            servingUnit = "serving", nutrients = Nutrients(calories = 0.0),
                        )
                        openLogAfterIngredientSave = true
                        navigateTo(AppScreen.AddIngredient)
                    }
                },
            )

            AppScreen.SyncSettings -> SyncSettingsScreen(
                onManageFoods = { navigateTo(AppScreen.ManageFoods) },
                settings = syncSettings,
                pendingChangeCount = pendingChangeCount,
                authSession = authSession,
                healthConnectAvailability = healthConnectAvailability,
                healthConnectPermissionGranted = healthConnectPermissionGranted,
                healthConnectExportEnabled = healthConnectExportEnabled,
                onBack = { popScreen() },
                onSave = { settings ->
                    syncSettings = settings
                    scope.launch { localStore.syncStateRepository.saveSettings(settings); refreshState() }
                },
                onSyncNow = {
                    scope.launch {
                        runCatching { syncService.syncNow() }
                            .onFailure { error ->
                                Log.e("CloudSync", "Manual sync failed", error)
                                Toast.makeText(
                                    context,
                                    error.message ?: "Sync failed",
                                    Toast.LENGTH_LONG,
                                ).show()
                            }
                        refreshState()
                    }
                },
                onSignIn = {
                    scope.launch {
                        val uri = authRepository.beginSignIn()
                        openExternalUri(uri.toString())
                    }
                },
                onSignInWithGoogle = {
                    scope.launch {
                        val uri = authRepository.beginSignIn(provider = "Google")
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
                onDeleteAccount = {
                    scope.launch {
                        runCatching { syncService.deleteAccount() }
                            .onSuccess {
                                localStore.clearAllLocalData()
                                refreshState()
                                resetTo(AppScreen.Home)
                                Toast.makeText(context, "Account deleted", Toast.LENGTH_LONG).show()
                            }
                            .onFailure { error ->
                                Log.e("AccountDelete", "Delete account failed", error)
                                Toast.makeText(
                                    context,
                                    error.message ?: "Delete account failed",
                                    Toast.LENGTH_LONG,
                                ).show()
                            }
                    }
                },
                onConnectHealthConnect = {
                    when (healthConnectAvailability) {
                        HealthConnectAvailability.Available -> {
                            healthPermissionsLauncher.launch(healthConnectExporter.requestedPermissions())
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
                        val (importedCount, skippedCount) = importWeightHistoryFromHealthConnect()
                        refreshState()
                        Toast.makeText(
                            context,
                            "Imported $importedCount weight entr${if (importedCount == 1) "y" else "ies"}, skipped $skippedCount duplicate${if (skippedCount == 1) "" else "s"}",
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                },
                onImportNutritionHistory = {
                    scope.launch {
                        val (importedCount, skippedCount) = importNutritionHistoryFromHealthConnect()
                        refreshState()
                        Toast.makeText(
                            context,
                            "Imported $importedCount food log${if (importedCount == 1) "" else "s"}, skipped $skippedCount duplicate${if (skippedCount == 1) "" else "s"}",
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                },
            )

            AppScreen.QuickCalories -> QuickCaloriesScreen(
                date = selectedDate, destinationMeal = if (acquisitionActive) acquisition.meal else null,
                recipeDestination = acquisitionActive && acquisition.recipe,
                onBack = { popScreen() },
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
                    acceptAcquiredFoods(listOf(entry))
                    finishAcquisition()
                },
            )

            AppScreen.AddIngredient -> AddIngredientScreen(
                knownFoods = customFoods + recipes + seedFoods.filterNot { it.id in hiddenSeedIds },
                onAnalyzeLabel = { bitmap, status -> photoAnalyzer.analyzeLabel(bitmap, status) },
                isSaving = acquisitionSaving, saveError = acquisitionError,
                existing = editingFood,
                barcodeLookupResult = addIngredientBarcodeLookupResult,
                isLookingUpBarcode = isLookingUpAddIngredientBarcode,
                onBack = {
                    addIngredientBarcodeLookupResult = null
                    isLookingUpAddIngredientBarcode = false
                    editingFood = null
                    openLogAfterIngredientSave = false
                    returnToRecipeAfterIngredientSave = false
                    popScreen()
                },
                onLookupBarcode = { barcode ->
                    scope.launch {
                        isLookingUpAddIngredientBarcode = true
                        addIngredientBarcodeLookupResult = lookupFoodForBarcode(barcode)
                        isLookingUpAddIngredientBarcode = false
                        if (addIngredientBarcodeLookupResult == null) {
                            Toast.makeText(context, "No barcode match found", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                onSave = { item ->
                    if (!acquisitionSaving) {
                        acquisitionSaving = true; acquisitionError = null
                        val wasEditing = editingFood != null
                        scope.launch {
                            try {
                                database.withTransaction { saveFood(item, publishCommunity = false) }
                                customFoods = listOf(item) + customFoods.filterNot { it.id == item.id }
                                if (selectedFood?.id == item.id) selectedFood = item
                                editingFood = null; addIngredientBarcodeLookupResult = null
                                if (wasEditing) popScreen()
                                else { selectedFood = item; popScreen(); navigateTo(AppScreen.FoodCreated) }
                            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
                            catch (_: Exception) { acquisitionError = "Could not save food. Please retry." }
                            finally { acquisitionSaving = false }
                        }
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
                    popScreen()
                },
                onAddIngredient = { beginAcquisition(Meal.Lunch, recipe = true) },
                onStartNestedRecipe = {
                    parentRecipeDraft = recipeDraft
                    recipeDraft = RecipeDraft()
                },
                isSaving = acquisitionSaving, saveError = acquisitionError,
                onSaveIngredient = { food -> database.withTransaction { saveFood(food, publishCommunity = false) }; customFoods = listOf(food) + customFoods.filterNot { it.id == food.id } },
                onSave = { draft ->
                    if (!acquisitionSaving) {
                        acquisitionSaving = true; acquisitionError = null
                        val wasEditingRecipe = recipeEditorId != null
                        val item = draft.toFoodItem(recipeEditorId)
                        scope.launch {
                            try {
                                database.withTransaction { saveFood(item, publishCommunity = false) }
                                recipes = listOf(item) + recipes.filterNot { it.id == item.id }
                                selectedFood = item; recipeDraft = RecipeDraft(); editingFood = null
                                acquisitionActive = false; parentRecipeDraft = null
                                popScreen(); if (!wasEditingRecipe) navigateTo(AppScreen.FoodCreated)
                            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
                            catch (_: Exception) { acquisitionError = "Could not save recipe. Please retry." }
                            finally { acquisitionSaving = false }
                        }
                    }
                },
            )

            AppScreen.FoodDetails -> selectedFood?.let { food ->
                com.philipcosgrave.calorietracker.ui.screens.FoodDetailsScreen(food,
                    onBack = { popScreen() },
                    onEdit = {
                        editingFood = food
                        if (food.kind == FoodKind.Recipe) {
                            recipeDraft = food.toRecipeDraft(); recipeEditorId = food.id
                            navigateTo(AppScreen.RecipeBuilder)
                        } else { addIngredientBarcodeLookupResult = null; navigateTo(AppScreen.AddIngredient) }
                    },
                    onLog = { editingDiaryEntry = null; navigateTo(AppScreen.LogFood) },
                    primaryLabel = if (acquisitionActive && acquisition.recipe) "Use Ingredient" else "Add to Meal",
                    onEditIngredient = { ingredient -> editingFood = ingredient; addIngredientBarcodeLookupResult = null; navigateTo(AppScreen.AddIngredient) },
                )
            }

            AppScreen.LogFood -> selectedFood?.let { food ->
                LogFoodScreen(
                    food = food,
                    date = selectedDate,
                    existingEntry = editingDiaryEntry,
                    destinationMeal = if (acquisitionActive) acquisition.meal else null,
                    recipeDestination = acquisitionActive && acquisition.recipe,
                    onEditNutrition = {
                        editingFood = food
                        addIngredientBarcodeLookupResult = null
                        isLookingUpAddIngredientBarcode = false
                        openLogAfterIngredientSave = false
                        returnToRecipeAfterIngredientSave = false
                        navigateTo(AppScreen.AddIngredient)
                    },
                    onBack = {
                        editingDiaryEntry = null
                        popScreen()
                    },
                    onLog = { entry, _ ->
                        val wasEditing = editingDiaryEntry != null
                        acceptAcquiredFoods(listOf(entry))
                        editingDiaryEntry = null
                        if (wasEditing) { acquisitionActive = false; lastAddedEntries = emptyList(); resetTo(AppScreen.Diary) }
                        else finishAcquisition()
                    },
                )
            } ?: run {
                resetTo(AppScreen.SearchFood)
            }
        }
        }
        if (screen in listOf(AppScreen.Home, AppScreen.Diary, AppScreen.Scan, AppScreen.Insights, AppScreen.More)) {
            BiteWiseBottomNavigation(screen) { acquisitionActive = false; resetTo(it) }
        }
        }
    }
}
