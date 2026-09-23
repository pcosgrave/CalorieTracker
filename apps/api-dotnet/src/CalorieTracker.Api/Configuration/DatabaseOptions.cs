namespace CalorieTracker.Api.Configuration;

public sealed class DatabaseOptions
{
    public const string SectionName = "Database";

    public string? ConnectionString { get; set; }
    public string? SecretArn { get; set; }
}
