using CalorieTracker.Api.Contracts;
using CalorieTracker.Api.Services.Households;

namespace CalorieTracker.Api.Endpoints;

public static class V1HouseholdEndpoints
{
    public static IEndpointRouteBuilder MapV1HouseholdEndpoints(this IEndpointRouteBuilder app)
    {
        var households = app.MapGroup("/v1/households").WithTags("Households").RequireAuthorization();

        households.MapGet("/", async (HttpContext context, HouseholdService service, CancellationToken cancellationToken) =>
        {
            if (!UserIdentity.TryRequireUserId(context, out var userId, out var unauthorized)) return unauthorized!;
            var result = await service.ListAsync(userId, cancellationToken);
            return Results.Ok(result.Select(ToResponse));
        });

        households.MapPost("/", async (CreateHouseholdRequest request, HttpContext context, HouseholdService service, CancellationToken cancellationToken) =>
        {
            if (!UserIdentity.TryRequireUserId(context, out var userId, out var unauthorized)) return unauthorized!;
            if (string.IsNullOrWhiteSpace(request.Name) || request.Name.Trim().Length > 120)
                return Results.ValidationProblem(new Dictionary<string, string[]> { ["name"] = ["Name is required and must be 120 characters or fewer."] });
            var result = await service.CreateAsync(userId, request.Name, cancellationToken);
            return Results.Created($"/v1/households/{result.HouseholdId}", ToResponse(result));
        });

        households.MapGet("/{householdId:guid}", async (Guid householdId, HttpContext context, HouseholdService service, CancellationToken cancellationToken) =>
        {
            if (!UserIdentity.TryRequireUserId(context, out var userId, out var unauthorized)) return unauthorized!;
            var result = await service.GetAsync(userId, householdId, cancellationToken);
            return result is null ? Results.NotFound() : Results.Ok(ToResponse(result));
        });

        return app;
    }

    private static HouseholdResponse ToResponse(HouseholdSummary summary) => new(summary.HouseholdId, summary.Name, summary.Role);
}
