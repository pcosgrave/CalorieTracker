namespace CalorieTracker.Api.Contracts;

public sealed record BarcodeLookupResponse(
    bool Found,
    FoodProduct? Product);

public sealed record FoodSearchResponse(
    IReadOnlyList<FoodProduct> Products);

public sealed record PublishCommunityFoodResponse(
    FoodProduct Product,
    bool Existed);

public sealed record AiParsedFoodLogEntry(
    string FoodName,
    string Brand,
    string Meal,
    double? Quantity,
    string Unit,
    string MatchStatus,
    string? MatchedFoodId,
    string Notes);

public sealed record AiFoodLogParseResponse(
    IReadOnlyList<AiParsedFoodLogEntry> Entries,
    IReadOnlyList<FoodProduct> CreatedFoods);
