using CalorieTracker.Api.Contracts;

namespace CalorieTracker.Api.Endpoints;

public static class WeightEndpoints
{
    public static IEndpointRouteBuilder MapWeightEndpoints(this IEndpointRouteBuilder app)
    {
        var weights = app.MapGroup("/weights").WithTags("Weights");

        weights.MapGet("/", () => EndpointResponses.NotImplemented("List weights"));
        weights.MapPost("/", (CreateWeightEntryRequest _) => EndpointResponses.NotImplemented("Create weight entry"));
        weights.MapPut("/{entryId}", (string entryId, UpdateWeightEntryRequest _) => EndpointResponses.NotImplemented($"Update weight entry {entryId}"));
        weights.MapDelete("/{entryId}", (string entryId) => EndpointResponses.NotImplemented($"Delete weight entry {entryId}"));

        return app;
    }
}
