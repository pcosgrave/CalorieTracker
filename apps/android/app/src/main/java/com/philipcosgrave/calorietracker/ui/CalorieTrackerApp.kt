package com.philipcosgrave.calorietracker.ui

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
import com.philipcosgrave.calorietracker.domain.toFoodItem
import com.philipcosgrave.calorietracker.domain.toRecipeDraft
import com.philipcosgrave.calorietracker.domain.VoiceFoodCommand
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
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

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
    val canadianNutrientFileLookupService = remember { CanadianNutrientFileLookupService() }
    val cloudFoodCatalogService = remember { CloudFoodCatalogService(localStore) }
    val openFoodFactsLookupService = remember { OpenFoodFactsLookupService() }
    val healthConnectExporter = remember { HealthConnectNutritionExporter(context) }
    val textToSpeech = remember(context) { TextToSpeech(context, null) }

    var customFoods by remember { mutableStateOf<List<FoodItem>>(emptyList()) }
    var recipes by remember { mutableStateOf<List<FoodItem>>(emptyList()) }
    var diary by remember { mutableStateOf<List<DiaryEntry>>(emptyList()) }
    var weights by remember { mutableStateOf<List<WeightEntry>>(emptyList()) }
    var hiddenSeedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val screenStack = remember { mutableStateListOf(AppScreen.Home) }
    val screen = screenStack.last()
    var selectedFood by remember { mutableStateOf<FoodItem?>(null) }
    var recipeDraft by remember { mutableStateOf(RecipeDraft()) }
    var parentRecipeDraft by remember { mutableStateOf<RecipeDraft?>(null) }
    var returnToRecipeAfterIngredientSave by remember { mutableStateOf(false) }
    var editingFood by remember { mutableStateOf<FoodItem?>(null) }
    var editingDiaryEntry by remember { mutableStateOf<DiaryEntry?>(null) }
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
    var remoteSearchQuery by remember { mutableStateOf("") }
    var isSearchingRemote by remember { mutableStateOf(false) }
    var searchFoodInitialQuery by remember { mutableStateOf("") }
    var voiceFeedback by remember { mutableStateOf(VoiceLogFeedback()) }
    var pendingVoiceCommand by remember { mutableStateOf<VoiceFoodCommand?>(null) }

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

    suspend fun saveDiaryEntry(entry: DiaryEntry) {
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
        if (healthConnectAvailability == HealthConnectAvailability.Available &&
            healthConnectPermissionGranted &&
            healthConnectExportEnabled
        ) {
            runCatching { healthConnectExporter.exportEntry(record) }
        }
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
        diary = listOf(entry) + diary
        selectedDate = today
        saveDiaryEntry(entry)
        refreshState()
        voiceFeedback = VoiceLogFeedback(
            phase = VoiceLogPhase.Success,
            transcript = transcript,
            query = command.ingredientQuery,
            message = "Added ${food.name} to ${meal.label.lowercase(Locale.CANADA)}.",
        )
        pendingVoiceCommand = null
    }

    suspend fun handleVoiceFoodTranscript(transcript: String) {
        voiceFeedback = VoiceLogFeedback(
            phase = VoiceLogPhase.Heard,
            transcript = transcript,
            message = "Heard that. Matching it now.",
        )
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
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.CANADA.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Say something like: Add 30 grams onion")
        }
        speechRecognitionLauncher.launch(intent)
    }

    val launchVoiceRecognition = {
        voiceFeedback = VoiceLogFeedback(
            phase = VoiceLogPhase.Listening,
            message = "Listening. Try saying: add 30 grams onion.",
        )
        recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
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

        if (existing == null && item.isUserCreated && authSession != null) {
            runCatching { cloudFoodCatalogService.publishCommunityFood(item) }
        }
    }

    fun beginImportRemoteFood(item: FoodItem, openLogAfterSave: Boolean) {
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

        val community = if (signedIn) runCatching {
            cloudFoodCatalogService.searchCommunityFoods(remoteSearchQuery)
        }.getOrDefault(emptyList()) else emptyList()

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
        if (healthConnectAvailability == HealthConnectAvailability.Available && healthConnectExportEnabled) {
            runCatching { healthConnectExporter.exportWeightEntry(weightEntry) }
                .onFailure { error ->
                    Log.e("HealthConnectWeight", "Failed to export weight entry ${weightEntry.id}", error)
                }
        }
    }

    suspend fun deleteWeightEntry(entry: WeightEntry) {
        localStore.weightRepository.delete(
            localStore.currentOwnerUserId(),
            entry.id,
        )
        if (healthConnectAvailability == HealthConnectAvailability.Available && healthConnectExportEnabled) {
            runCatching { healthConnectExporter.deleteWeightEntry(entry.id) }
                .onFailure { error ->
                    Log.e("HealthConnectWeight", "Failed to delete weight entry ${entry.id}", error)
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
                scope.launch { refreshState() }
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
                onOpenFoodLog = { navigateTo(AppScreen.Diary) },
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
                onAddFood = {
                    searchFoodInitialQuery = ""
                    clearVoiceFeedback()
                    navigateTo(AppScreen.SearchFood)
                },
                onVoiceLog = launchVoiceRecognition,
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
                    editingDiaryEntry = entry
                    selectedFood = entry.food
                    navigateTo(AppScreen.LogFood)
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
                        saveWeightEntry(entry)
                        refreshState()
                    }
                    editingWeight = null
                    popScreen()
                },
            )

            AppScreen.SearchFood -> SearchFoodScreen(
                date = selectedDate,
                foods = customFoods + recipes + seedFoods.filterNot { hiddenSeedIds.contains(it.id) },
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
                    editingFood = null
                    returnToRecipeAfterIngredientSave = false
                    navigateTo(AppScreen.AddIngredient)
                },
                onAddRecipe = {
                    recipeDraft = RecipeDraft()
                    parentRecipeDraft = null
                    navigateTo(AppScreen.RecipeBuilder)
                },
                onSelectFood = {
                    searchFoodInitialQuery = ""
                    clearVoiceFeedback()
                    selectedFood = it
                    navigateTo(AppScreen.LogFood)
                },
                onQuickLogFood = { food ->
                    clearVoiceFeedback()
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
                        navigateTo(AppScreen.RecipeBuilder)
                    } else {
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

            AppScreen.BarcodeScanner -> BarcodeScannerScreen(
                onBack = { popScreen() },
                onBarcodeDetected = { barcode ->
                    scope.launch {
                        val found = findFoodByBarcode(barcode)
                        if (found != null) {
                            selectedFood = found
                            navigateTo(AppScreen.LogFood)
                        } else {
                            val personalCloudFood = if (authSession != null) {
                                runCatching { cloudFoodCatalogService.lookupPersonalBarcode(barcode) }.getOrNull()
                            } else {
                                null
                            }
                            val communityFood = if (personalCloudFood == null && authSession != null) {
                                runCatching { cloudFoodCatalogService.lookupCommunityBarcode(barcode) }.getOrNull()
                            } else {
                                null
                            }
                            val openFoodFactsFood = if (personalCloudFood == null && communityFood == null) {
                                runCatching { openFoodFactsLookupService.lookupFoodByBarcode(barcode) }.getOrNull()
                            } else {
                                null
                            }

                            when {
                                personalCloudFood != null -> {
                                    beginImportRemoteFood(personalCloudFood, openLogAfterSave = true)
                                }
                                communityFood != null -> {
                                    beginImportRemoteFood(communityFood, openLogAfterSave = true)
                                }
                                openFoodFactsFood != null -> {
                                    beginImportRemoteFood(openFoodFactsFood, openLogAfterSave = true)
                                }
                                else -> {
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
                                    openLogAfterIngredientSave = true
                                    navigateTo(AppScreen.AddIngredient)
                                }
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
                date = selectedDate,
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
                    diary = listOf(entry) + diary
                    selectedDate = date
                    scope.launch {
                        saveDiaryEntry(entry)
                        refreshState()
                    }
                    resetTo(AppScreen.Diary)
                },
            )

            AppScreen.AddIngredient -> AddIngredientScreen(
                existing = editingFood,
                onBack = {
                    editingFood = null
                    openLogAfterIngredientSave = false
                    returnToRecipeAfterIngredientSave = false
                    popScreen()
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
                        openLogAfterIngredientSave = false
                        popScreen()
                    } else if (openLogAfterIngredientSave && item.kind == FoodKind.Ingredient) {
                        selectedFood = item
                        openLogAfterIngredientSave = false
                        editingFood = null
                        popScreen()
                        if (screen == AppScreen.BarcodeScanner) {
                            popScreen()
                        }
                        if (screen != AppScreen.SearchFood) {
                            navigateTo(AppScreen.SearchFood)
                        }
                        navigateTo(AppScreen.LogFood)
                    } else {
                        returnToRecipeAfterIngredientSave = false
                        openLogAfterIngredientSave = false
                        popScreen()
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
                onAddIngredient = {
                    editingFood = null
                    returnToRecipeAfterIngredientSave = true
                    navigateTo(AppScreen.AddIngredient)
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
                        popScreen()
                    }
                },
            )

            AppScreen.LogFood -> selectedFood?.let { food ->
                LogFoodScreen(
                    food = food,
                    date = selectedDate,
                    existingEntry = editingDiaryEntry,
                    onBack = {
                        editingDiaryEntry = null
                        popScreen()
                    },
                    onLog = { entry, addMore ->
                        diary = listOf(entry) + diary.filterNot { it.id == entry.id }
                        selectedDate = entry.date
                        scope.launch {
                            saveDiaryEntry(entry)
                            refreshState()
                        }
                        editingDiaryEntry = null
                        if (addMore) {
                            popScreen()
                        } else {
                            resetTo(AppScreen.Diary)
                        }
                    },
                )
            } ?: run {
                resetTo(AppScreen.SearchFood)
            }
        }
    }
}
