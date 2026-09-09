namespace CalorieTracker.Api.Endpoints;

internal static class EndpointResponses
{
    public static IResult NotImplemented(string capability) =>
        Results.Json(
            new
            {
                message = $"{capability} is not implemented in the .NET backend yet.",
                phase = "phase-1-foundation",
            },
            statusCode: StatusCodes.Status501NotImplemented);
}
