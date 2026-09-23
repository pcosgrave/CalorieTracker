using System.Security.Claims;

namespace CalorieTracker.Api.Endpoints;

internal static class UserIdentity
{
    public static string? TryGetUserId(HttpContext context)
    {
        var user = context.User;
        var claim = user.FindFirstValue("sub")
            ?? user.FindFirstValue(ClaimTypes.NameIdentifier)
            ?? user.FindFirstValue("username");

        if (!string.IsNullOrWhiteSpace(claim))
        {
            return claim;
        }

        return null;
    }

    public static IResult? RequireUserId(HttpContext context, out string userId)
    {
        userId = TryGetUserId(context) ?? string.Empty;
        if (!string.IsNullOrWhiteSpace(userId))
        {
            return null;
        }

        return Results.Json(new { message = "Authentication is required" }, statusCode: StatusCodes.Status401Unauthorized);
    }

    public static bool TryRequireUserId(HttpContext context, out string userId, out IResult? unauthorizedResult)
    {
        unauthorizedResult = RequireUserId(context, out userId);
        return unauthorizedResult is null;
    }
}
