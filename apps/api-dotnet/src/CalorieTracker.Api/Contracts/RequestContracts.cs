namespace CalorieTracker.Api.Contracts;

public sealed record CreateFoodProductRequest(
    string? Barcode,
    string Name,
    string? Brand,
    Serving Serving,
    Nutrients Nutrients);

public sealed record UpdateFoodProductRequest(
    string? Barcode,
    string Name,
    string? Brand,
    Serving Serving,
    Nutrients Nutrients);

public sealed record PublishCommunityFoodRequest(
    string? ProductId,
    string? Barcode,
    string Name,
    string? Brand,
    Serving Serving,
    Nutrients Nutrients);

public sealed record CreateDiaryEntryRequest(
    string ProductId,
    DateTimeOffset LoggedAt,
    MealType Meal,
    double ServingMultiplier,
    double? LoggedAmount,
    string? LoggedUnit);

public sealed record UpdateDiaryEntryRequest(
    string ProductId,
    DateTimeOffset LoggedAt,
    MealType Meal,
    double ServingMultiplier,
    double? LoggedAmount,
    string? LoggedUnit);

public sealed record CreateWeightEntryRequest(
    DateTimeOffset LoggedAt,
    double WeightKg,
    string? Source);

public sealed record UpdateWeightEntryRequest(
    DateTimeOffset LoggedAt,
    double WeightKg,
    string? Source);

public sealed record AiFoodLogParseRequest(
    string Transcript,
    DateOnly Date,
    string FallbackMeal);
