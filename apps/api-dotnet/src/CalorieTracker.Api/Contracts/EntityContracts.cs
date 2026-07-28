namespace CalorieTracker.Api.Contracts;

public sealed record FoodProduct(
    string ProductId,
    string OwnerUserId,
    Visibility Visibility,
    string? Barcode,
    string Name,
    string? Brand,
    Serving Serving,
    Nutrients Nutrients,
    int? Frequency,
    int? BreakfastFrequency,
    int? LunchFrequency,
    int? DinnerFrequency,
    int? SnackFrequency,
    DateTimeOffset? LastUsedAt,
    DateTimeOffset CreatedAt,
    DateTimeOffset UpdatedAt);

public sealed record BarcodeAlias(
    string Barcode,
    string OwnerUserId,
    string ProductId,
    Visibility Visibility,
    DateTimeOffset CreatedAt);

public sealed record DiaryEntry(
    string EntryId,
    string OwnerUserId,
    string ProductId,
    DateTimeOffset LoggedAt,
    MealType Meal,
    double ServingMultiplier,
    double? LoggedAmount,
    string? LoggedUnit,
    FoodProduct ProductSnapshot,
    DateTimeOffset CreatedAt,
    DateTimeOffset UpdatedAt);

public sealed record WeightEntry(
    string EntryId,
    string OwnerUserId,
    DateTimeOffset LoggedAt,
    double WeightKg,
    string Source,
    DateTimeOffset CreatedAt,
    DateTimeOffset UpdatedAt);
