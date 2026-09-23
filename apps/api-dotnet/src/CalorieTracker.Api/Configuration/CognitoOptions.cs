using System.ComponentModel.DataAnnotations;

namespace CalorieTracker.Api.Configuration;

public sealed class CognitoOptions
{
    public const string SectionName = "Cognito";

    [Required]
    public string? UserPoolId { get; set; }

    public string? Region { get; set; }

    public string? ClientId { get; set; }
}
