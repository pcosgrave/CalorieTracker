namespace CalorieTracker.Api.Services.Foods;

public sealed class FoodProductNotFoundException(string productId) : Exception($"Product not found: {productId}")
{
    public string ProductId { get; } = productId;
}
