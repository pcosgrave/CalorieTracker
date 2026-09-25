using CalorieTracker.Api.Contracts;
using CalorieTracker.Api.Handlers.Foods;

namespace CalorieTracker.Api.Endpoints;

public static class FoodEndpoints
{
    public static IEndpointRouteBuilder MapFoodEndpoints(this IEndpointRouteBuilder app)
    {
        var foods = app.MapGroup("/foods").WithTags("Foods");

        foods.MapGet("/", FoodHandlers.ListFoodsAsync);
        foods.MapPost("/", FoodHandlers.CreateFoodAsync);
        foods.MapPut("/{productId}", FoodHandlers.UpdateFoodAsync);
        foods.MapDelete("/{productId}", FoodHandlers.DeleteFoodAsync);
        foods.MapGet("/barcode/{barcode}", FoodHandlers.LookupBarcodeAsync);
        foods.MapGet("/search", FoodHandlers.SearchFoodsAsync);

        var community = foods.MapGroup("/community");
        community.MapGet("/barcode/{barcode}", FoodHandlers.LookupCommunityBarcodeAsync);
        community.MapGet("/search", FoodHandlers.SearchCommunityFoodsAsync);
        community.MapPost("/", FoodHandlers.PublishCommunityFoodAsync);

        return app;
    }
}
