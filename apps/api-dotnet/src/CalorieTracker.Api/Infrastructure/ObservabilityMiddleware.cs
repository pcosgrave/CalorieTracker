using System.Diagnostics;
using System.Text.Json;

namespace CalorieTracker.Api.Infrastructure;

public sealed class ObservabilityMiddleware(RequestDelegate next, ILogger<ObservabilityMiddleware> logger)
{
    public async Task InvokeAsync(HttpContext context)
    {
        var correlationId = context.Request.Headers["X-Correlation-Id"].FirstOrDefault();
        if (string.IsNullOrWhiteSpace(correlationId) || correlationId.Length > 128)
            correlationId = context.TraceIdentifier;

        context.Items["CorrelationId"] = correlationId;
        context.Response.Headers["X-Correlation-Id"] = correlationId;
        var stopwatch = Stopwatch.StartNew();

        using (logger.BeginScope(new Dictionary<string, object>
        {
            ["correlationId"] = correlationId,
            ["requestId"] = context.TraceIdentifier,
            ["method"] = context.Request.Method,
            ["path"] = context.Request.Path.ToString(),
        }))
        {
            try
            {
                await next(context);
                logger.LogInformation("HTTP {Method} {Path} completed with {StatusCode} in {DurationMs}ms",
                    context.Request.Method, context.Request.Path, context.Response.StatusCode, stopwatch.ElapsedMilliseconds);
            }
            catch (Exception exception)
            {
                logger.LogError(exception, "HTTP {Method} {Path} failed after {DurationMs}ms",
                    context.Request.Method, context.Request.Path, stopwatch.ElapsedMilliseconds);
                throw;
            }
        }
    }
}

public static class ObservabilityLogging
{
    public static void LogEvent(this ILogger logger, string eventName, LogLevel level = LogLevel.Information, Exception? exception = null, object? properties = null)
    {
        logger.Log(level, exception, "Application event {EventName} {Properties}", eventName,
            properties is null ? "{}" : JsonSerializer.Serialize(properties));
    }
}
