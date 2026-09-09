using Amazon.SecretsManager;
using Amazon.SecretsManager.Model;
using CalorieTracker.Api.Configuration;
using CalorieTracker.Api.Contracts;
using CalorieTracker.Api.Infrastructure;
using CalorieTracker.Api.Services.Foods;
using System.Net.Http.Json;
using System.Text.Json;

namespace CalorieTracker.Api.Services.Ai;

public sealed class AiFoodLogService(
    HttpClient httpClient,
    IAmazonSecretsManager secretsManager,
    AiOptions options,
    IFoodRepository foodRepository)
{
    private const string ModelName = "gemini-2.0-flash";
    private static readonly Uri GeminiUri = new($"https://generativelanguage.googleapis.com/v1beta/models/{ModelName}:generateContent");
    private string? _cachedApiKey;

    public async Task<AiFoodLogParseResponse> ParseFoodLogAsync(string userId, AiFoodLogParseRequest request, CancellationToken cancellationToken)
    {
        var foods = await foodRepository.ListProductsAsync(userId, cancellationToken);
        var parsed = await ParseWithGeminiAsync(request, cancellationToken);

        var entries = parsed
            .Select(entry =>
            {
                var matchedFood = MatchParsedFood(entry, foods);
                return new AiParsedFoodLogEntry(
                    FoodName: entry.FoodName,
                    Brand: entry.Brand ?? string.Empty,
                    Meal: entry.Meal ?? request.FallbackMeal,
                    Quantity: entry.Quantity,
                    Unit: entry.Unit ?? string.Empty,
                    MatchStatus: matchedFood is null ? "Unresolved" : "Matched",
                    MatchedFoodId: matchedFood?.ProductId,
                    Notes: entry.Notes ?? (matchedFood is null ? "Needs review" : "Matched to an existing food"));
            })
            .ToArray();

        return new AiFoodLogParseResponse(entries, []);
    }

    private async Task<IReadOnlyList<GeminiParsedFoodItem>> ParseWithGeminiAsync(AiFoodLogParseRequest request, CancellationToken cancellationToken)
    {
        var apiKey = await LoadApiKeyAsync(cancellationToken);
        var response = await httpClient.PostAsJsonAsync(
            new Uri($"{GeminiUri}?key={Uri.EscapeDataString(apiKey)}"),
            new
            {
                contents = new[]
                {
                    new
                    {
                        role = "user",
                        parts = new[] { new { text = BuildPrompt(request) } },
                    },
                },
                generationConfig = new
                {
                    temperature = 0.1,
                    responseMimeType = "application/json",
                },
            },
            cancellationToken);

        response.EnsureSuccessStatusCode();

        using var payload = JsonDocument.Parse(await response.Content.ReadAsStringAsync(cancellationToken));
        var text = string.Concat(
            payload.RootElement
                .GetProperty("candidates")[0]
                .GetProperty("content")
                .GetProperty("parts")
                .EnumerateArray()
                .Select(part => part.TryGetProperty("text", out var textElement) ? textElement.GetString() : string.Empty));

        if (string.IsNullOrWhiteSpace(text))
        {
            throw new InvalidOperationException("Gemini returned an empty response");
        }

        using var parsedDocument = JsonDocument.Parse(text);
        var entries = parsedDocument.RootElement.GetProperty("entries")
            .EnumerateArray()
            .Select(element => AppJson.Deserialize<GeminiParsedFoodItem>(element)!)
            .Where(item => item is not null)
            .ToArray();

        return entries;
    }

    private async Task<string> LoadApiKeyAsync(CancellationToken cancellationToken)
    {
        if (!string.IsNullOrWhiteSpace(_cachedApiKey))
        {
            return _cachedApiKey!;
        }

        if (string.IsNullOrWhiteSpace(options.GeminiApiSecretArn))
        {
            throw new InvalidOperationException("Missing Gemini API secret ARN");
        }

        var response = await secretsManager.GetSecretValueAsync(
            new GetSecretValueRequest
            {
                SecretId = options.GeminiApiSecretArn,
            },
            cancellationToken);

        var secretString = response.SecretString?.Trim();
        if (string.IsNullOrWhiteSpace(secretString))
        {
            throw new InvalidOperationException("Gemini API secret is empty");
        }

        _cachedApiKey = secretString.StartsWith("{", StringComparison.Ordinal)
            ? ExtractApiKeyFromJson(secretString)
            : secretString;

        return _cachedApiKey!;
    }

    private static string ExtractApiKeyFromJson(string secretString)
    {
        using var document = JsonDocument.Parse(secretString);
        foreach (var propertyName in new[] { "apiKey", "key", "GEMINI_API_KEY", "geminiApiKey" })
        {
            if (document.RootElement.TryGetProperty(propertyName, out var value) && value.ValueKind == JsonValueKind.String)
            {
                return value.GetString()!.Trim();
            }
        }

        throw new InvalidOperationException("Gemini API secret JSON must contain an API key string");
    }

    private static string BuildPrompt(AiFoodLogParseRequest request) =>
        string.Join(
            "\n",
            "Parse the following food log transcript into structured food entries.",
            "Return JSON only.",
            $"Default date: {request.Date:yyyy-MM-dd}",
            $"Fallback meal: {request.FallbackMeal}",
            "For each entry, capture:",
            "- foodName: the item name",
            "- brand: optional brand if clearly stated",
            "- meal: Breakfast, Lunch, Dinner, or Snack if stated or strongly implied; otherwise use the fallback meal",
            "- quantity: numeric quantity if present",
            "- unit: serving unit if present",
            "- date: use the explicit date if stated, otherwise the default date",
            "- notes: optional short clarification only when needed",
            "Do not infer calories or nutrition.",
            "Do not invent foods that are not mentioned.",
            "Transcript:",
            request.Transcript.Trim());

    private static FoodProduct? MatchParsedFood(GeminiParsedFoodItem parsed, IReadOnlyList<FoodProduct> foods)
    {
        var normalizedFoodName = NormalizeText(parsed.FoodName);
        var normalizedBrand = NormalizeText(parsed.Brand ?? string.Empty);

        return foods
            .Select(food => new { Food = food, Score = ScoreFoodMatch(food, normalizedFoodName, normalizedBrand) })
            .Where(candidate => candidate.Score >= 70)
            .OrderByDescending(candidate => candidate.Score)
            .Select(candidate => candidate.Food)
            .FirstOrDefault();
    }

    private static int ScoreFoodMatch(FoodProduct food, string normalizedFoodName, string normalizedBrand)
    {
        var normalizedName = NormalizeText(food.Name);
        var candidateBrand = NormalizeText(food.Brand ?? string.Empty);

        if (string.IsNullOrWhiteSpace(normalizedFoodName))
        {
            return -1;
        }

        var score = -1;
        if (normalizedName == normalizedFoodName)
        {
            score = 100;
        }
        else if (normalizedName.Contains(normalizedFoodName, StringComparison.Ordinal) ||
                 normalizedFoodName.Contains(normalizedName, StringComparison.Ordinal))
        {
            score = 85;
        }
        else
        {
            var overlap = OverlapScore(normalizedName, normalizedFoodName);
            if (overlap >= 0.75) score = 70;
            else if (overlap >= 0.5) score = 55;
        }

        if (score < 0) return score;

        if (!string.IsNullOrWhiteSpace(normalizedBrand))
        {
            if (candidateBrand == normalizedBrand) score += 15;
            else if (!string.IsNullOrWhiteSpace(candidateBrand) &&
                     (candidateBrand.Contains(normalizedBrand, StringComparison.Ordinal) ||
                      normalizedBrand.Contains(candidateBrand, StringComparison.Ordinal)))
            {
                score += 8;
            }
            else
            {
                score -= 10;
            }
        }

        return score;
    }

    private static double OverlapScore(string left, string right)
    {
        var leftTokens = left.Split(' ', StringSplitOptions.RemoveEmptyEntries).ToHashSet(StringComparer.Ordinal);
        var rightTokens = right.Split(' ', StringSplitOptions.RemoveEmptyEntries).ToHashSet(StringComparer.Ordinal);
        if (leftTokens.Count == 0 || rightTokens.Count == 0) return 0;
        var matches = leftTokens.Count(rightTokens.Contains);
        return matches / (double)Math.Max(leftTokens.Count, rightTokens.Count);
    }

    private static string NormalizeText(string value) =>
        string.Join(
            " ",
            value.Trim()
                .ToLowerInvariant()
                .Select(character => char.IsLetterOrDigit(character) ? character : ' ')
                .ToArray())
        .Replace("  ", " ", StringComparison.Ordinal)
        .Trim();

    private sealed record GeminiParsedFoodItem(
        string FoodName,
        string? Brand,
        string? Meal,
        double? Quantity,
        string? Unit,
        string? Date,
        string? Notes);
}
