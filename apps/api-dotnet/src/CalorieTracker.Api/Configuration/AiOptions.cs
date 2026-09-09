namespace CalorieTracker.Api.Configuration;

public sealed class AiOptions
{
    public const string SectionName = "Ai";

    public string? GeminiApiSecretArn { get; set; }
}
