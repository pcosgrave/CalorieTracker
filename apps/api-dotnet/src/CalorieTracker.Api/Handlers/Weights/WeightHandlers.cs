using CalorieTracker.Api.Contracts;
using CalorieTracker.Api.Endpoints;
using CalorieTracker.Api.Services.Weights;

namespace CalorieTracker.Api.Handlers.Weights;

internal static class WeightHandlers
{
    public static async Task<IResult> CreateAsync(HttpContext context, CreateWeightEntryRequest request, WeightService service, CancellationToken cancellationToken)
    {
        if (!UserIdentity.TryRequireUserId(context, out var userId, out var unauthorized)) return unauthorized!;
        var entry = await service.CreateWeightEntryAsync(userId, request, cancellationToken);
        return Results.Json(new { entry }, statusCode: StatusCodes.Status201Created);
    }

    public static async Task<IResult> ListAsync(HttpContext context, WeightService service, CancellationToken cancellationToken)
    {
        if (!UserIdentity.TryRequireUserId(context, out var userId, out var unauthorized)) return unauthorized!;
        var entries = await service.ListWeightEntriesAsync(userId, cancellationToken);
        return Results.Ok(new { entries });
    }

    public static async Task<IResult> UpdateAsync(HttpContext context, string entryId, UpdateWeightEntryRequest request, WeightService service, CancellationToken cancellationToken)
    {
        if (!UserIdentity.TryRequireUserId(context, out var userId, out var unauthorized)) return unauthorized!;
        if (string.IsNullOrWhiteSpace(entryId))
        {
            return Results.Json(new { message = "Entry ID is required" }, statusCode: StatusCodes.Status400BadRequest);
        }

        try
        {
            var entry = await service.UpdateWeightEntryAsync(userId, entryId, request, cancellationToken);
            return Results.Ok(new { entry });
        }
        catch (WeightEntryNotFoundException)
        {
            return Results.Json(new { message = "Weight entry not found" }, statusCode: StatusCodes.Status404NotFound);
        }
    }

    public static async Task<IResult> DeleteAsync(HttpContext context, string entryId, WeightService service, CancellationToken cancellationToken)
    {
        if (!UserIdentity.TryRequireUserId(context, out var userId, out var unauthorized)) return unauthorized!;
        if (string.IsNullOrWhiteSpace(entryId))
        {
            return Results.Json(new { message = "Entry ID is required" }, statusCode: StatusCodes.Status400BadRequest);
        }

        await service.DeleteWeightEntryAsync(userId, entryId, cancellationToken);
        return Results.Json(new { }, statusCode: StatusCodes.Status204NoContent);
    }
}
