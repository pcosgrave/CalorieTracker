using System.Text.Json;
using System.Text.Json.Serialization;

namespace CalorieTracker.Api.Infrastructure;

internal static class AppJson
{
    public static readonly JsonSerializerOptions SerializerOptions = new()
    {
        PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
        DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull,
    };

    static AppJson()
    {
        SerializerOptions.Converters.Add(new JsonStringEnumConverter(JsonNamingPolicy.CamelCase));
    }

    public static JsonElement ToJsonElement<T>(T value)
    {
        var bytes = JsonSerializer.SerializeToUtf8Bytes(value, SerializerOptions);
        using var document = JsonDocument.Parse(bytes);
        return document.RootElement.Clone();
    }

    public static T? Deserialize<T>(JsonElement? element)
    {
        if (element is null)
        {
            return default;
        }

        return JsonSerializer.Deserialize<T>(element.Value.GetRawText(), SerializerOptions);
    }
}
