# CalorieTracker Android Test Suite

This directory contains comprehensive unit and integration tests for the CalorieTracker Android application.

## Test Framework

- **JUnit 4**: Primary testing framework
- **Mockito**: Mocking framework for dependencies
- **Coroutines Test**: Testing asynchronous code with `runBlocking`
- **Truth**: Additional assertions (optional)
- **Room Testing**: Database tests with in-memory databases
- **Espresso**: UI component testing

## Test Structure

```
app/src/test/
├── java/com/philipcosgrave/calorietracker/
│   ├── domain/
│   │   ├── CalorieTrackerMathTest.kt    # Mathematical calculations tests
│   │   ├── VoiceFoodLoggingTest.kt       # Voice command parsing tests
│   │   ├── SyncDomainTest.kt             # Sync metadata tests
│   │   └── RepositoryContractsTest.kt    # Repository interface tests
│   ├── model/
│   │   ├── AppModelsTest.kt              # Model class tests
│   │   └── SyncModelsTest.kt             # Sync model tests
│   ├── data/
│   │   └── LocalRecordEntitiesTest.kt    # Database entity tests
│   └── repository/
│       ├── FoodRepositoryTest.kt         # Food data repository tests
│       ├── DiaryRepositoryTest.kt        # Diary entry repository tests
│       ├── WeightRepositoryTest.kt       # Weight tracking repository tests
│       ├── BarcodeAliasRepositoryTest.kt # Barcode alias repository tests
│       └── SyncOutboxRepositoryTest.kt   # Sync outbox tests
├── kotlin/
│   └── test/
│       ├── MockRepository.kt             # Mock implementations for testing
│       ├── PreviewData.kt                # Test fixtures and preview data
│       └── ...
└── resources/
    └── test.properties                   # Test configuration
```

## Running Tests

### Run all tests:
```bash
./gradlew test
```

### Run tests with logcat output:
```bash
./gradlew testDebug --stacktrace
```

### Run specific test class:
```bash
./gradlew test --tests com.philipcosgrave.calorietracker.domain.CalorieTrackerMathTest
```

### Run with coverage (requires jacoco):
```bash
./gradlew jacocoTestReport
./gradlew jacocoTestCoverageVerification
```

## Test Coverage Areas

### Domain Layer Tests
- `CalorieTrackerMathTest`: All mathematical operations (unit conversion, nutrient scaling, totals calculation)
- `VoiceFoodLoggingTest`: Voice command parsing and meal inference
- `SyncDomainTest`: Sync metadata and change envelope creation

### Model Layer Tests
- `AppModelsTest`: Model data class creation and defaults
- `SyncModelsTest`: Sync metadata, records, and envelopes

### Database Layer Tests
- `LocalRecordEntitiesTest`: Room entity creation and JSON serialization
- `FoodRepositoryTest`: Food CRUD operations
- `DiaryRepositoryTest`: Diary entry CRUD and date range queries
- `WeightRepositoryTest`: Weight tracking operations
- `BarcodeAliasRepositoryTest`: Barcode alias management
- `SyncOutboxRepositoryTest`: Sync outbox queue management

### Repository Layer Tests
- `RepositoryContractsTest`: Repository interface implementations
- Mock repositories for testing with controlled data

## Test Utilities

### MockRepository
Provides mock implementations of repositories for unit testing:
- `MockFoodRepository`
- `MockDiaryRepository`
- `MockBarcodeAliasRepository`
- `MockWeightRepository`
- `MockSyncRepository`

### PreviewData
Test fixtures for UI previews:
- Sample food items
- Sample diary entries
- Sample sync settings
- Sample weight entries

### Configuration
Test configuration in `test.properties`:
- Mock API base URLs
- Feature flags for testing

## Writing New Tests

1. Create a new test class in the appropriate package
2. Use `@Test` annotations from JUnit
3. Use `runBlocking` for suspend functions
4. Use `@Before` and `@After` for setup/teardown
5. Keep tests isolated and deterministic

### Example:
```kotlin
@org.junit.Test
fun test_example() = runBlocking {
    // Your test code
}
```

## Continuous Integration

Tests run automatically on:
- Pull request creation
- Every push to main branch
- Nightly builds

## Test Quality Standards

- All new features must include tests
- Tests must be isolated (no shared state)
- Mock only external dependencies
- Use meaningful test names
- Prefer parameterized tests for variations

## Troubleshooting

### Test fails due to Android resource issues:
```bash
./gradlew cleanTest
./gradlew test --info
```

### Run test in debugger:
```bash
./gradlew testDebug -Dorg.gradle.debug=true
```

### View test results:
- Check `build/reports/tests/test/` for HTML reports
- Use `--info` flag for detailed output
- Check logcat for Android-specific errors

## Dependencies

Tests use the following key dependencies:
- JUnit 4.13.2
- Mockito 5.15.2
- Coroutines Test 1.9.0
- Room Testing 2.6.1
- Espresso 3.6.1
- AndroidX Core Testing 2.2.0

## Contributing

When adding tests:
1. Add test implementation first
2. Verify tests pass
3. Update this README if adding new test utilities
4. Run full test suite before committing

## License

Part of CalorieTracker - See LICENSE file for details.
