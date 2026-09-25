using CalorieTracker.Api.Endpoints;
using CalorieTracker.Api.Services.Account;

namespace CalorieTracker.Api.Handlers.Account;

internal static class AccountHandlers
{
    public static async Task<IResult> DeleteAsync(HttpContext context, AccountService service, CancellationToken cancellationToken)
    {
        if (!UserIdentity.TryRequireUserId(context, out var userId, out var unauthorized)) return unauthorized!;
        await service.DeleteAccountAsync(userId, cancellationToken);
        return Results.Ok(new { deleted = true });
    }
}
