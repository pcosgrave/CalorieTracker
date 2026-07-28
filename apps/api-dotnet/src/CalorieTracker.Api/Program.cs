using CalorieTracker.Api.Configuration;
using CalorieTracker.Api.Endpoints;
using System.Text.Json.Serialization;

var builder = WebApplication.CreateBuilder(args);

builder.Configuration.AddEnvironmentVariables();

builder.Services
    .AddProblemDetails()
    .AddEndpointsApiExplorer()
    .ConfigureHttpJsonOptions(options =>
    {
        options.SerializerOptions.Converters.Add(new JsonStringEnumConverter(namingPolicy: System.Text.Json.JsonNamingPolicy.CamelCase));
    })
    .AddApiConfiguration(builder.Configuration);

var app = builder.Build();

app.UseExceptionHandler();

app.MapGet("/", () => Results.Ok(new
{
    service = "calorie-tracker-api-dotnet",
    status = "ok",
    timestamp = DateTimeOffset.UtcNow,
}));

app.MapGet("/health", (IConfiguration configuration) =>
{
    var requiredEnvironment = ApiEnvironment.RequiredVariableNames
        .ToDictionary(
            keySelector: name => name,
            elementSelector: configuration.GetValue<string>);

    return Results.Ok(new
    {
        service = "calorie-tracker-api-dotnet",
        status = "healthy",
        timestamp = DateTimeOffset.UtcNow,
        requiredEnvironment,
    });
});

app.MapFoodEndpoints();
app.MapDiaryEndpoints();
app.MapWeightEndpoints();
app.MapSyncEndpoints();
app.MapAiEndpoints();
app.MapAccountEndpoints();

app.Run();
