using CalorieTracker.Api.Contracts;
using CalorieTracker.Api.Endpoints;
using CalorieTracker.Api.Services.Sync;

namespace CalorieTracker.Api.Handlers.Sync;

internal static class SyncHandlers
{
    public static async Task<IResult> PushAsync(HttpContext context, SyncPushRequest request, SyncService service, CancellationToken cancellationToken)
    {
        if (!UserIdentity.TryRequireUserId(context, out var userId, out var unauthorized)) return unauthorized!;
        var response = await service.PushChangesAsync(userId, request, cancellationToken);
        return Results.Ok(response);
    }

    public static async Task<IResult> PullAsync(HttpContext context, SyncPullRequest request, SyncService service, CancellationToken cancellationToken)
    {
        if (!UserIdentity.TryRequireUserId(context, out var userId, out var unauthorized)) return unauthorized!;
        var response = await service.PullChangesAsync(userId, request, cancellationToken);
        return Results.Ok(response);
    }
}
