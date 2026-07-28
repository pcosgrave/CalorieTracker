namespace CalorieTracker.Api.Contracts;

public sealed record BarcodeLookupResponse(
    bool Found,
    FoodProduct? Product);

public sealed record FoodSearchResponse(
    IReadOnlyList<FoodProduct> Products);

public sealed record PublishCommunityFoodResponse(
    FoodProduct Product,
    bool Existed);

public sealed record AiParsedFoodEntry(
    string FoodName,
    string Brand,
    AiMealType? Meal,
    double? Quantity,
    string Unit,
    string Notes);

public sealed record AiFoodLogParseResponse(
    IReadOnlyList<AiParsedFoodEntry> Entries,
    IReadOnlyList<FoodProduct> CreatedFoods);
