using CalorieTracker.Api.Contracts;

namespace CalorieTracker.Api.Endpoints;

public static class FoodEndpoints
{
    public static IEndpointRouteBuilder MapFoodEndpoints(this IEndpointRouteBuilder app)
    {
        var foods = app.MapGroup("/foods").WithTags("Foods");

        foods.MapGet("/", () => EndpointResponses.NotImplemented("List foods"));
        foods.MapPost("/", (CreateFoodProductRequest _) => EndpointResponses.NotImplemented("Create food"));
        foods.MapPut("/{productId}", (string productId, UpdateFoodProductRequest _) => EndpointResponses.NotImplemented($"Update food {productId}"));
        foods.MapDelete("/{productId}", (string productId) => EndpointResponses.NotImplemented($"Delete food {productId}"));
        foods.MapGet("/barcode/{barcode}", (string barcode) => EndpointResponses.NotImplemented($"Lookup barcode {barcode}"));
        foods.MapGet("/search", (string? query) => EndpointResponses.NotImplemented($"Search foods with query '{query}'"));

        var community = foods.MapGroup("/community");
        community.MapGet("/barcode/{barcode}", (string barcode) => EndpointResponses.NotImplemented($"Lookup community barcode {barcode}"));
        community.MapGet("/search", (string? query) => EndpointResponses.NotImplemented($"Search community foods with query '{query}'"));
        community.MapPost("/", (PublishCommunityFoodRequest _) => EndpointResponses.NotImplemented("Publish community food"));

        return app;
    }
}
