using System.ComponentModel.DataAnnotations;
using CalorieTracker.Api.Infrastructure;

namespace CalorieTracker.Api.Endpoints;

public static class V1Endpoints
{
    public static void MapV1Endpoints(this WebApplication app)
    {
        var v1 = app.MapGroup("/v1").AddEndpointFilter(async (context, next) =>
        {
            if (!UserIdentity.TryRequireUserId(context.HttpContext, out _, out var unauthorized)) return unauthorized!;
            return await next(context);
        });

        v1.MapGet("/me", (HttpContext context) => Results.Ok(new
        {
            userId = UserIdentity.TryGetUserId(context),
            capabilities = new[] { "foods", "diary-entries", "weight-entries", "sync" },
            apiVersion = "v1"
        }));

        v1.MapPost("/devices", (DeviceRequest request, HttpContext context) =>
        {
            if (!Validator.TryValidateObject(request, new ValidationContext(request), new List<ValidationResult>(), true))
                return Results.ValidationProblem(new Dictionary<string, string[]> { ["deviceId"] = ["A device ID is required."] }, statusCode: 422);
            return Results.Created($"/v1/devices/{request.DeviceId}", new { deviceId = request.DeviceId, registeredAt = DateTimeOffset.UtcNow, syncCursor = (string?)null });
        });

        v1.MapGet("/devices", (HttpContext context) => Results.Ok(Array.Empty<object>()));
        v1.MapMethods("/recipes", ["GET", "POST", "PUT", "DELETE"], () => EndpointResponses.NotImplemented("Recipes"));
        v1.MapMethods("/meals", ["GET", "POST", "PUT", "DELETE"], () => EndpointResponses.NotImplemented("Meals"));
        v1.MapMethods("/households", ["GET", "POST", "PUT", "DELETE"], () => EndpointResponses.NotImplemented("Households"));
        v1.MapMethods("/leftovers", ["GET", "POST", "PUT", "DELETE"], () => EndpointResponses.NotImplemented("Leftovers"));
        v1.MapMethods("/pantry", ["GET", "POST", "PUT", "DELETE"], () => EndpointResponses.NotImplemented("Pantry"));
    }

    public sealed record DeviceRequest([property: Required] string DeviceId, string? Platform, string? AppVersion);
}
