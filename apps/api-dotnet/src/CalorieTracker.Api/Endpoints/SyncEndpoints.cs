using CalorieTracker.Api.Contracts;

namespace CalorieTracker.Api.Endpoints;

public static class SyncEndpoints
{
    public static IEndpointRouteBuilder MapSyncEndpoints(this IEndpointRouteBuilder app)
    {
        var sync = app.MapGroup("/sync").WithTags("Sync");

        sync.MapPost("/push", (SyncPushRequest _) => EndpointResponses.NotImplemented("Push sync changes"));
        sync.MapPost("/pull", (SyncPullRequest _) => EndpointResponses.NotImplemented("Pull sync changes"));

        return app;
    }
}
