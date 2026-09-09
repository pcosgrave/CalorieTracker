using CalorieTracker.Api.Contracts;
using CalorieTracker.Api.Handlers.Diary;

namespace CalorieTracker.Api.Endpoints;

public static class DiaryEndpoints
{
    public static IEndpointRouteBuilder MapDiaryEndpoints(this IEndpointRouteBuilder app)
    {
        var diary = app.MapGroup("/diary").WithTags("Diary");

        diary.MapPost("/", DiaryHandlers.CreateAsync);
        diary.MapPut("/{entryId}", DiaryHandlers.UpdateAsync);
        diary.MapDelete("/{entryId}", DiaryHandlers.DeleteAsync);

        return app;
    }
}
