using CalorieTracker.Api.Contracts;

namespace CalorieTracker.Api.Endpoints;

public static class DiaryEndpoints
{
    public static IEndpointRouteBuilder MapDiaryEndpoints(this IEndpointRouteBuilder app)
    {
        var diary = app.MapGroup("/diary").WithTags("Diary");

        diary.MapPost("/", (CreateDiaryEntryRequest _) => EndpointResponses.NotImplemented("Create diary entry"));
        diary.MapPut("/{entryId}", (string entryId, UpdateDiaryEntryRequest _) => EndpointResponses.NotImplemented($"Update diary entry {entryId}"));
        diary.MapDelete("/{entryId}", (string entryId) => EndpointResponses.NotImplemented($"Delete diary entry {entryId}"));

        return app;
    }
}
