using CalorieTracker.Api.Contracts;

namespace CalorieTracker.Api.Endpoints;

public static class AiEndpoints
{
    public static IEndpointRouteBuilder MapAiEndpoints(this IEndpointRouteBuilder app)
    {
        var ai = app.MapGroup("/ai").WithTags("AI");

        ai.MapPost("/parse-food-log", (AiFoodLogParseRequest _) => EndpointResponses.NotImplemented("Parse AI food log"));

        return app;
    }
}
