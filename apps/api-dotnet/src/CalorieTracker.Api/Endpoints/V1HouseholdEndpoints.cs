using CalorieTracker.Api.Contracts;
using CalorieTracker.Api.Services.Households;

namespace CalorieTracker.Api.Endpoints;

public static class V1HouseholdEndpoints
{
    public static IEndpointRouteBuilder MapV1HouseholdEndpoints(this IEndpointRouteBuilder app)
    {
        // API Gateway's Cognito authorizer supplies HttpContext.User. Do not use
        // ASP.NET RequireAuthorization here: this Lambda does not register an
        // ASP.NET authentication scheme, and that gate rejects every request
        // before UserIdentity can read the verified gateway claims.
        var households = app.MapGroup("/v1/households").WithTags("Households");

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
        households.MapPost("/join", async (JoinHouseholdRequest request, HttpContext context, HouseholdService service, CancellationToken cancellationToken) =>
        {
            if (!UserIdentity.TryRequireUserId(context, out var userId, out var unauthorized)) return unauthorized!;
            var result = await service.JoinAsync(userId, request.HouseholdId, request.Name, cancellationToken);
            return result is null ? Results.NotFound() : Results.Ok(ToResponse(result));
        });

        households.MapGet("/{householdId:guid}", async (Guid householdId, HttpContext context, HouseholdService service, CancellationToken cancellationToken) =>
        {
            if (!UserIdentity.TryRequireUserId(context, out var userId, out var unauthorized)) return unauthorized!;
            var result = await service.GetAsync(userId, householdId, cancellationToken);
            return result is null ? Results.NotFound() : Results.Ok(ToResponse(result));
        });
        households.MapGet("/{householdId:guid}/members", async (Guid householdId, HttpContext context, HouseholdService service, CancellationToken cancellationToken) => UserIdentity.TryRequireUserId(context, out var userId, out var unauthorized) ? Results.Ok(await service.MembersAsync(userId, householdId, cancellationToken) ?? []) : unauthorized!);
        households.MapPost("/{householdId:guid}/members", async (Guid householdId, AddHouseholdMemberRequest request, HttpContext context, HouseholdService service, CancellationToken cancellationToken) => UserIdentity.TryRequireUserId(context, out var userId, out var unauthorized) ? Results.Ok(await service.AddMemberAsync(userId, householdId, request.Email, request.Name, cancellationToken)) : unauthorized!);
        households.MapDelete("/{householdId:guid}/members/{memberId}", async (Guid householdId, string memberId, HttpContext context, HouseholdService service, CancellationToken cancellationToken) => UserIdentity.TryRequireUserId(context, out var userId, out var unauthorized) ? (await service.RemoveMemberAsync(userId, householdId, memberId, cancellationToken) ? Results.NoContent() : Results.NotFound()) : unauthorized!);
        households.MapDelete("/{householdId:guid}", async (Guid householdId, HttpContext context, HouseholdService service, CancellationToken cancellationToken) => UserIdentity.TryRequireUserId(context, out var userId, out var unauthorized) ? (await service.DeleteAsync(userId, householdId, cancellationToken) ? Results.NoContent() : Results.NotFound()) : unauthorized!);
        households.MapPost("/{householdId:guid}/leave", async (Guid householdId, HttpContext context, HouseholdService service, CancellationToken cancellationToken) => UserIdentity.TryRequireUserId(context, out var userId, out var unauthorized) ? (await service.LeaveAsync(userId, householdId, cancellationToken) ? Results.NoContent() : Results.NotFound()) : unauthorized!);

        return app;
    }

    private static HouseholdResponse ToResponse(HouseholdSummary summary) => new(summary.HouseholdId, summary.Name, summary.Role);
}
