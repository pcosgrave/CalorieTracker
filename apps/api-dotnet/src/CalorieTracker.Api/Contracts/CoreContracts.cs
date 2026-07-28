namespace CalorieTracker.Api.Contracts;

public enum Visibility
{
    Private,
    Shared,
    Global,
}

public enum MealType
{
    Breakfast,
    Lunch,
    Dinner,
    Snack,
}

public sealed record Nutrients(
    double Calories,
    double ProteinGrams,
    double CarbohydrateGrams,
    double FatGrams,
    double? FiberGrams,
    double? SugarGrams,
    double? SodiumMilligrams);

public sealed record Serving(
    string Label,
    double Quantity,
    string Unit,
    double? Grams);
