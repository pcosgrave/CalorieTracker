namespace CalorieTracker.Api.Contracts;

public sealed record BarcodeLookupResponse(
    bool Found,
    FoodProduct? Product);

public sealed record FoodSearchResponse(
    IReadOnlyList<FoodProduct> Products);

public sealed record PublishCommunityFoodResponse(
    FoodProduct Product,
    bool Existed);

