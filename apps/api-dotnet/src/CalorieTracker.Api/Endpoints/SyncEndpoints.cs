using CalorieTracker.Api.Contracts;
using CalorieTracker.Api.Handlers.Sync;

namespace CalorieTracker.Api.Endpoints;

public static class SyncEndpoints
{
    public static IEndpointRouteBuilder MapSyncEndpoints(this IEndpointRouteBuilder app)
    {
        var sync = app.MapGroup("/sync").WithTags("Sync");

        sync.MapPost("/push", SyncHandlers.PushAsync);
        sync.MapPost("/pull", SyncHandlers.PullAsync);

        return app;
    }
}
