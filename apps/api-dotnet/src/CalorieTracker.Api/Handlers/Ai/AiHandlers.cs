using CalorieTracker.Api.Contracts;
using CalorieTracker.Api.Endpoints;
using CalorieTracker.Api.Services.Ai;

namespace CalorieTracker.Api.Handlers.Ai;

internal static class AiHandlers
{
    public static async Task<IResult> ParseFoodLogAsync(HttpContext context, AiFoodLogParseRequest request, AiFoodLogService service, CancellationToken cancellationToken)
    {
        if (!UserIdentity.TryRequireUserId(context, out var userId, out var unauthorized)) return unauthorized!;
        if (string.IsNullOrWhiteSpace(request.Transcript))
        {
            return Results.Json(new { message = "Transcript is required" }, statusCode: StatusCodes.Status400BadRequest);
        }

        if (!IsValidMeal(request.FallbackMeal))
        {
            return Results.Json(new { message = "Fallback meal is invalid" }, statusCode: StatusCodes.Status400BadRequest);
        }

        var response = await service.ParseFoodLogAsync(userId, request with { FallbackMeal = NormalizeMeal(request.FallbackMeal) }, cancellationToken);
        return Results.Ok(response);
    }

    private static bool IsValidMeal(string value) =>
        value is "Breakfast" or "Lunch" or "Dinner" or "Snack";

    private static string NormalizeMeal(string value) =>
        char.ToUpperInvariant(value[0]) + value[1..].ToLowerInvariant();
}
