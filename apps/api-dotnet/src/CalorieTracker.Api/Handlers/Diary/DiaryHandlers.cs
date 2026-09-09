using CalorieTracker.Api.Contracts;
using CalorieTracker.Api.Endpoints;
using CalorieTracker.Api.Services.Diary;
using CalorieTracker.Api.Services.Foods;

namespace CalorieTracker.Api.Handlers.Diary;

internal static class DiaryHandlers
{
    public static async Task<IResult> CreateAsync(HttpContext context, CreateDiaryEntryRequest request, DiaryService service, CancellationToken cancellationToken)
    {
        if (!UserIdentity.TryRequireUserId(context, out var userId, out var unauthorized)) return unauthorized!;
        if (string.IsNullOrWhiteSpace(request.ProductId))
        {
            return Results.Json(new { message = "Product ID is required" }, statusCode: StatusCodes.Status400BadRequest);
        }

        try
        {
            var entry = await service.CreateDiaryEntryAsync(userId, request, cancellationToken);
            return Results.Json(new { entry }, statusCode: StatusCodes.Status201Created);
        }
        catch (FoodProductNotFoundException)
        {
            return Results.Json(new { message = "Product not found" }, statusCode: StatusCodes.Status404NotFound);
        }
    }

    public static async Task<IResult> UpdateAsync(HttpContext context, string entryId, UpdateDiaryEntryRequest request, DiaryService service, CancellationToken cancellationToken)
    {
        if (!UserIdentity.TryRequireUserId(context, out var userId, out var unauthorized)) return unauthorized!;
        if (string.IsNullOrWhiteSpace(entryId))
        {
            return Results.Json(new { message = "Entry ID is required" }, statusCode: StatusCodes.Status400BadRequest);
        }

        try
        {
            var entry = await service.UpdateDiaryEntryAsync(userId, entryId, request, cancellationToken);
            return Results.Ok(new { entry });
        }
        catch (DiaryEntryNotFoundException)
        {
            return Results.Json(new { message = "Diary entry not found" }, statusCode: StatusCodes.Status404NotFound);
        }
        catch (FoodProductNotFoundException)
        {
            return Results.Json(new { message = "Product not found" }, statusCode: StatusCodes.Status404NotFound);
        }
    }

    public static async Task<IResult> DeleteAsync(HttpContext context, string entryId, DiaryService service, CancellationToken cancellationToken)
    {
        if (!UserIdentity.TryRequireUserId(context, out var userId, out var unauthorized)) return unauthorized!;
        if (string.IsNullOrWhiteSpace(entryId))
        {
            return Results.Json(new { message = "Entry ID is required" }, statusCode: StatusCodes.Status400BadRequest);
        }

        await service.DeleteDiaryEntryAsync(userId, entryId, cancellationToken);
        return Results.Json(new { }, statusCode: StatusCodes.Status204NoContent);
    }
}
