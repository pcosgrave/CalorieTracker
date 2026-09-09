using CalorieTracker.Api.Contracts;
using CalorieTracker.Api.Handlers.Ai;

namespace CalorieTracker.Api.Endpoints;

public static class AiEndpoints
{
    public static IEndpointRouteBuilder MapAiEndpoints(this IEndpointRouteBuilder app)
    {
        var ai = app.MapGroup("/ai").WithTags("AI");

        ai.MapPost("/parse-food-log", AiHandlers.ParseFoodLogAsync);

        return app;
    }
}
