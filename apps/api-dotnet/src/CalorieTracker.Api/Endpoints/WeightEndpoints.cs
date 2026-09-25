using CalorieTracker.Api.Contracts;
using CalorieTracker.Api.Handlers.Weights;

namespace CalorieTracker.Api.Endpoints;

public static class WeightEndpoints
{
    public static IEndpointRouteBuilder MapWeightEndpoints(this IEndpointRouteBuilder app)
    {
        var weights = app.MapGroup("/weights").WithTags("Weights");

        weights.MapGet("/", WeightHandlers.ListAsync);
        weights.MapPost("/", WeightHandlers.CreateAsync);
        weights.MapPut("/{entryId}", WeightHandlers.UpdateAsync);
        weights.MapDelete("/{entryId}", WeightHandlers.DeleteAsync);

        return app;
    }
}
