using Amazon.DynamoDBv2.Model;
using CalorieTracker.Api.Contracts;
using System.Globalization;
using System.Text.Json;

namespace CalorieTracker.Api.Infrastructure;

internal static class DynamoDbSerialization
{
    public static Dictionary<string, AttributeValue> ToAttributeMap(FoodProduct product)
    {
        var item = new Dictionary<string, AttributeValue>
        {
            ["productId"] = String(product.ProductId),
            ["ownerUserId"] = String(product.OwnerUserId),
            ["visibility"] = String(ToWireValue(product.Visibility)),
            ["name"] = String(product.Name),
            ["serving"] = new AttributeValue
            {
                M = new Dictionary<string, AttributeValue>
                {
                    ["label"] = String(product.Serving.Label),
                    ["quantity"] = Number(product.Serving.Quantity),
                    ["unit"] = String(product.Serving.Unit),
                },
            },
            ["nutrients"] = new AttributeValue
            {
                M = new Dictionary<string, AttributeValue>
                {
                    ["calories"] = Number(product.Nutrients.Calories),
                    ["proteinGrams"] = Number(product.Nutrients.ProteinGrams),
                    ["carbohydrateGrams"] = Number(product.Nutrients.CarbohydrateGrams),
                    ["fatGrams"] = Number(product.Nutrients.FatGrams),
                },
            },
            ["createdAt"] = String(product.CreatedAt.ToString("O")),
            ["updatedAt"] = String(product.UpdatedAt.ToString("O")),
        };

        AddString(item, "barcode", product.Barcode);
        AddString(item, "brand", product.Brand);
        AddNumber(item, "frequency", product.Frequency);
        AddNumber(item, "breakfastFrequency", product.BreakfastFrequency);
        AddNumber(item, "lunchFrequency", product.LunchFrequency);
        AddNumber(item, "dinnerFrequency", product.DinnerFrequency);
        AddNumber(item, "snackFrequency", product.SnackFrequency);
        AddDateTime(item, "lastUsedAt", product.LastUsedAt);

        if (product.Serving.Grams is double grams)
        {
            item["serving"].M["grams"] = Number(grams);
        }

        if (product.Nutrients.FiberGrams is double fiber)
        {
            item["nutrients"].M["fiberGrams"] = Number(fiber);
        }

        if (product.Nutrients.SugarGrams is double sugar)
        {
            item["nutrients"].M["sugarGrams"] = Number(sugar);
        }

        if (product.Nutrients.SodiumMilligrams is double sodium)
        {
            item["nutrients"].M["sodiumMilligrams"] = Number(sodium);
        }

        return item;
    }

    public static FoodProduct MapFoodProduct(IReadOnlyDictionary<string, AttributeValue> item)
    {
        var serving = item["serving"].M;
        var nutrients = item["nutrients"].M;

        return new FoodProduct(
            ProductId: item["productId"].S,
            OwnerUserId: item["ownerUserId"].S,
            Visibility: ParseVisibility(GetString(item, "visibility") ?? "private"),
            Barcode: GetString(item, "barcode"),
            Name: item["name"].S,
            Brand: GetString(item, "brand"),
            Serving: new Serving(
                Label: serving["label"].S,
                Quantity: ParseDouble(serving["quantity"].N),
                Unit: serving["unit"].S,
                Grams: GetDouble(serving, "grams")),
            Nutrients: new Nutrients(
                Calories: ParseDouble(nutrients["calories"].N),
                ProteinGrams: ParseDouble(nutrients["proteinGrams"].N),
                CarbohydrateGrams: ParseDouble(nutrients["carbohydrateGrams"].N),
                FatGrams: ParseDouble(nutrients["fatGrams"].N),
                FiberGrams: GetDouble(nutrients, "fiberGrams"),
                SugarGrams: GetDouble(nutrients, "sugarGrams"),
                SodiumMilligrams: GetDouble(nutrients, "sodiumMilligrams")),
            Frequency: GetInt(item, "frequency"),
            BreakfastFrequency: GetInt(item, "breakfastFrequency"),
            LunchFrequency: GetInt(item, "lunchFrequency"),
            DinnerFrequency: GetInt(item, "dinnerFrequency"),
            SnackFrequency: GetInt(item, "snackFrequency"),
            LastUsedAt: GetDateTime(item, "lastUsedAt"),
            CreatedAt: ParseDateTime(item["createdAt"].S),
            UpdatedAt: ParseDateTime(item["updatedAt"].S));
    }

    public static Dictionary<string, AttributeValue> ToAttributeMap(BarcodeAlias alias) =>
        new()
        {
            ["barcode"] = String(alias.Barcode),
            ["ownerUserId"] = String(alias.OwnerUserId),
            ["productId"] = String(alias.ProductId),
            ["visibility"] = String(ToWireValue(alias.Visibility)),
            ["createdAt"] = String(alias.CreatedAt.ToString("O")),
        };

    public static BarcodeAlias MapBarcodeAlias(IReadOnlyDictionary<string, AttributeValue> item) =>
        new(
            Barcode: item["barcode"].S,
            OwnerUserId: item["ownerUserId"].S,
            ProductId: item["productId"].S,
            Visibility: ParseVisibility(GetString(item, "visibility") ?? "private"),
            CreatedAt: ParseDateTime(item["createdAt"].S));

    public static Dictionary<string, AttributeValue> ToAttributeMap(DiaryEntry entry)
    {
        var item = new Dictionary<string, AttributeValue>
        {
            ["entryId"] = String(entry.EntryId),
            ["ownerUserId"] = String(entry.OwnerUserId),
            ["productId"] = String(entry.ProductId),
            ["loggedAt"] = String(entry.LoggedAt.ToString("O")),
            ["meal"] = String(ToWireValue(entry.Meal)),
            ["servingMultiplier"] = Number(entry.ServingMultiplier),
            ["productSnapshot"] = new AttributeValue { M = ToAttributeMap(entry.ProductSnapshot) },
            ["createdAt"] = String(entry.CreatedAt.ToString("O")),
            ["updatedAt"] = String(entry.UpdatedAt.ToString("O")),
        };

        AddDouble(item, "loggedAmount", entry.LoggedAmount);
        AddString(item, "loggedUnit", entry.LoggedUnit);
        return item;
    }

    public static DiaryEntry MapDiaryEntry(IReadOnlyDictionary<string, AttributeValue> item) =>
        new(
            EntryId: item["entryId"].S,
            OwnerUserId: item["ownerUserId"].S,
            ProductId: item["productId"].S,
            LoggedAt: ParseDateTime(item["loggedAt"].S),
            Meal: ParseMeal(item["meal"].S),
            ServingMultiplier: ParseDouble(item["servingMultiplier"].N),
            LoggedAmount: GetDouble(item, "loggedAmount"),
            LoggedUnit: GetString(item, "loggedUnit"),
            ProductSnapshot: MapFoodProduct(item["productSnapshot"].M),
            CreatedAt: ParseDateTime(item["createdAt"].S),
            UpdatedAt: ParseDateTime(item["updatedAt"].S));

    public static Dictionary<string, AttributeValue> ToAttributeMap(WeightEntry entry)
    {
        var item = new Dictionary<string, AttributeValue>
        {
            ["entryId"] = String(entry.EntryId),
            ["ownerUserId"] = String(entry.OwnerUserId),
            ["loggedAt"] = String(entry.LoggedAt.ToString("O")),
            ["weightKg"] = Number(entry.WeightKg),
            ["source"] = String(entry.Source),
            ["createdAt"] = String(entry.CreatedAt.ToString("O")),
            ["updatedAt"] = String(entry.UpdatedAt.ToString("O")),
        };

        return item;
    }

    public static WeightEntry MapWeightEntry(IReadOnlyDictionary<string, AttributeValue> item) =>
        new(
            EntryId: item["entryId"].S,
            OwnerUserId: item["ownerUserId"].S,
            LoggedAt: ParseDateTime(item["loggedAt"].S),
            WeightKg: ParseDouble(item["weightKg"].N),
            Source: item["source"].S,
            CreatedAt: ParseDateTime(item["createdAt"].S),
            UpdatedAt: ParseDateTime(item["updatedAt"].S));

    public static Dictionary<string, AttributeValue> ToAttributeMap(SyncChange change)
    {
        var item = new Dictionary<string, AttributeValue>
        {
            ["changeId"] = String(change.ChangeId),
            ["entityType"] = String(change.EntityType),
            ["recordId"] = String(change.RecordId),
            ["operation"] = String(change.Operation),
            ["changedAt"] = String(change.ChangedAt.ToString("O")),
            ["deviceId"] = String(change.DeviceId),
        };

        if (change.BaseVersion is int baseVersion)
        {
            item["baseVersion"] = new AttributeValue { N = baseVersion.ToString(CultureInfo.InvariantCulture) };
        }

        if (change.Payload is JsonElement payload)
        {
            item["payload"] = ToAttributeValue(payload);
        }

        return item;
    }

    public static SyncChange MapSyncChange(IReadOnlyDictionary<string, AttributeValue> item) =>
        new(
            ChangeId: item["changeId"].S,
            EntityType: item["entityType"].S,
            RecordId: item["recordId"].S,
            Operation: item["operation"].S,
            ChangedAt: ParseDateTime(item["changedAt"].S),
            DeviceId: item["deviceId"].S,
            BaseVersion: GetInt(item, "baseVersion"),
            Payload: item.TryGetValue("payload", out var payload) ? ToJsonElement(payload) : null);

    public static AttributeValue ToAttributeValue(JsonElement element) =>
        element.ValueKind switch
        {
            JsonValueKind.Object => new AttributeValue
            {
                M = element.EnumerateObject().ToDictionary(
                    property => property.Name,
                    property => ToAttributeValue(property.Value)),
            },
            JsonValueKind.Array => new AttributeValue
            {
                L = element.EnumerateArray().Select(ToAttributeValue).ToList(),
            },
            JsonValueKind.String => String(element.GetString() ?? string.Empty),
            JsonValueKind.Number => Number(element.GetDouble()),
            JsonValueKind.True => new AttributeValue { BOOL = true },
            JsonValueKind.False => new AttributeValue { BOOL = false },
            JsonValueKind.Null => new AttributeValue { NULL = true },
            _ => new AttributeValue { NULL = true },
        };

    public static JsonElement ToJsonElement(AttributeValue value)
    {
        object? node = FromAttributeValue(value);
        return AppJson.ToJsonElement(node);
    }

    private static object? FromAttributeValue(AttributeValue value)
    {
        if (value.NULL == true)
        {
            return null;
        }

        if (value.M.Count > 0)
        {
            return value.M.ToDictionary(pair => pair.Key, pair => FromAttributeValue(pair.Value));
        }

        if (value.L.Count > 0)
        {
            return value.L.Select(FromAttributeValue).ToArray();
        }

        if (value.S is not null)
        {
            return value.S;
        }

        if (value.N is not null)
        {
            return double.TryParse(value.N, CultureInfo.InvariantCulture, out var number)
                ? number
                : value.N;
        }

        if (value.BOOL.HasValue)
        {
            return value.BOOL.Value;
        }

        return null;
    }

    public static MealType ParseMeal(string value) =>
        value.ToLowerInvariant() switch
        {
            "lunch" => MealType.Lunch,
            "dinner" => MealType.Dinner,
            "snack" => MealType.Snack,
            _ => MealType.Breakfast,
        };

    public static string ToWireValue(MealType meal) =>
        meal switch
        {
            MealType.Lunch => "lunch",
            MealType.Dinner => "dinner",
            MealType.Snack => "snack",
            _ => "breakfast",
        };

    public static Visibility ParseVisibility(string value) =>
        value.ToLowerInvariant() switch
        {
            "shared" => Visibility.Shared,
            "global" => Visibility.Global,
            _ => Visibility.Private,
        };

    public static string ToWireValue(Visibility visibility) =>
        visibility switch
        {
            Visibility.Shared => "shared",
            Visibility.Global => "global",
            _ => "private",
        };

    public static double ParseDouble(string value) =>
        double.Parse(value, CultureInfo.InvariantCulture);

    public static DateTimeOffset ParseDateTime(string value) =>
        DateTimeOffset.Parse(value, CultureInfo.InvariantCulture, DateTimeStyles.RoundtripKind);

    public static string? GetString(IReadOnlyDictionary<string, AttributeValue> item, string key) =>
        item.TryGetValue(key, out var value) ? value.S : null;

    public static double? GetDouble(IReadOnlyDictionary<string, AttributeValue> item, string key) =>
        item.TryGetValue(key, out var value) && !string.IsNullOrWhiteSpace(value.N)
            ? ParseDouble(value.N)
            : null;

    public static int? GetInt(IReadOnlyDictionary<string, AttributeValue> item, string key) =>
        item.TryGetValue(key, out var value) && !string.IsNullOrWhiteSpace(value.N)
            ? int.Parse(value.N, CultureInfo.InvariantCulture)
            : null;

    public static DateTimeOffset? GetDateTime(IReadOnlyDictionary<string, AttributeValue> item, string key) =>
        item.TryGetValue(key, out var value) && !string.IsNullOrWhiteSpace(value.S)
            ? ParseDateTime(value.S)
            : null;

    public static AttributeValue Number(double value) =>
        new() { N = value.ToString("G17", CultureInfo.InvariantCulture) };

    public static AttributeValue String(string value) =>
        new(value);

    public static void AddString(IDictionary<string, AttributeValue> item, string key, string? value)
    {
        if (!string.IsNullOrWhiteSpace(value))
        {
            item[key] = String(value);
        }
    }

    public static void AddNumber(IDictionary<string, AttributeValue> item, string key, int? value)
    {
        if (value is int number)
        {
            item[key] = new AttributeValue { N = number.ToString(CultureInfo.InvariantCulture) };
        }
    }

    public static void AddDouble(IDictionary<string, AttributeValue> item, string key, double? value)
    {
        if (value is double number)
        {
            item[key] = Number(number);
        }
    }

    public static void AddDateTime(IDictionary<string, AttributeValue> item, string key, DateTimeOffset? value)
    {
        if (value is DateTimeOffset timestamp)
        {
            item[key] = String(timestamp.ToString("O"));
        }
    }
}
