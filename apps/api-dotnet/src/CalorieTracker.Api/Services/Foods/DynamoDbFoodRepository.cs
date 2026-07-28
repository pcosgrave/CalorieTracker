using Amazon.DynamoDBv2;
using Amazon.DynamoDBv2.Model;
using CalorieTracker.Api.Configuration;
using CalorieTracker.Api.Contracts;
using System.Globalization;

namespace CalorieTracker.Api.Services.Foods;

public sealed class DynamoDbFoodRepository(
    IAmazonDynamoDB dynamoDb,
    StorageOptions storageOptions) : IFoodRepository
{
    public async Task<FoodProduct?> GetProductAsync(string ownerUserId, string productId, CancellationToken cancellationToken)
    {
        var response = await dynamoDb.GetItemAsync(
            new GetItemRequest
            {
                TableName = storageOptions.ProductsTableName,
                Key = new Dictionary<string, AttributeValue>
                {
                    ["ownerUserId"] = new(ownerUserId),
                    ["productId"] = new(productId),
                },
            },
            cancellationToken);

        return response.Item.Count == 0 ? null : MapFoodProduct(response.Item);
    }

    public async Task<IReadOnlyList<FoodProduct>> ListProductsAsync(string ownerUserId, CancellationToken cancellationToken)
    {
        var response = await dynamoDb.QueryAsync(
            new QueryRequest
            {
                TableName = storageOptions.ProductsTableName,
                KeyConditionExpression = "ownerUserId = :ownerUserId",
                ExpressionAttributeValues = new Dictionary<string, AttributeValue>
                {
                    [":ownerUserId"] = new(ownerUserId),
                },
                Limit = 100,
                ScanIndexForward = false,
            },
            cancellationToken);

        return response.Items.Select(MapFoodProduct).ToArray();
    }

    public Task SaveProductAsync(FoodProduct product, CancellationToken cancellationToken) =>
        dynamoDb.PutItemAsync(
            new PutItemRequest
            {
                TableName = storageOptions.ProductsTableName,
                Item = ToAttributeMap(product),
            },
            cancellationToken);

    public Task DeleteProductAsync(string ownerUserId, string productId, CancellationToken cancellationToken) =>
        dynamoDb.DeleteItemAsync(
            new DeleteItemRequest
            {
                TableName = storageOptions.ProductsTableName,
                Key = new Dictionary<string, AttributeValue>
                {
                    ["ownerUserId"] = new(ownerUserId),
                    ["productId"] = new(productId),
                },
            },
            cancellationToken);

    public async Task<BarcodeAlias?> GetBarcodeAliasAsync(string ownerUserId, string barcode, CancellationToken cancellationToken)
    {
        var response = await dynamoDb.GetItemAsync(
            new GetItemRequest
            {
                TableName = storageOptions.BarcodeAliasesTableName,
                Key = new Dictionary<string, AttributeValue>
                {
                    ["ownerUserId"] = new(ownerUserId),
                    ["barcode"] = new(barcode),
                },
            },
            cancellationToken);

        return response.Item.Count == 0 ? null : MapBarcodeAlias(response.Item);
    }

    public Task SaveBarcodeAliasAsync(BarcodeAlias alias, CancellationToken cancellationToken) =>
        dynamoDb.PutItemAsync(
            new PutItemRequest
            {
                TableName = storageOptions.BarcodeAliasesTableName,
                Item = ToAttributeMap(alias),
            },
            cancellationToken);

    public Task DeleteBarcodeAliasAsync(string ownerUserId, string barcode, CancellationToken cancellationToken) =>
        dynamoDb.DeleteItemAsync(
            new DeleteItemRequest
            {
                TableName = storageOptions.BarcodeAliasesTableName,
                Key = new Dictionary<string, AttributeValue>
                {
                    ["ownerUserId"] = new(ownerUserId),
                    ["barcode"] = new(barcode),
                },
            },
            cancellationToken);

    private static Dictionary<string, AttributeValue> ToAttributeMap(FoodProduct product)
    {
        var item = new Dictionary<string, AttributeValue>
        {
            ["productId"] = new(product.ProductId),
            ["ownerUserId"] = new(product.OwnerUserId),
            ["visibility"] = new(ToWireValue(product.Visibility)),
            ["name"] = new(product.Name),
            ["serving"] = new AttributeValue
            {
                M = new Dictionary<string, AttributeValue>
                {
                    ["label"] = new(product.Serving.Label),
                    ["quantity"] = Number(product.Serving.Quantity),
                    ["unit"] = new(product.Serving.Unit),
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
            ["createdAt"] = new(product.CreatedAt.ToString("O")),
            ["updatedAt"] = new(product.UpdatedAt.ToString("O")),
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

        if (product.Nutrients.FiberGrams is double fiberGrams)
        {
            item["nutrients"].M["fiberGrams"] = Number(fiberGrams);
        }

        if (product.Nutrients.SugarGrams is double sugarGrams)
        {
            item["nutrients"].M["sugarGrams"] = Number(sugarGrams);
        }

        if (product.Nutrients.SodiumMilligrams is double sodiumMilligrams)
        {
            item["nutrients"].M["sodiumMilligrams"] = Number(sodiumMilligrams);
        }

        return item;
    }

    private static Dictionary<string, AttributeValue> ToAttributeMap(BarcodeAlias alias)
    {
        var item = new Dictionary<string, AttributeValue>
        {
            ["barcode"] = new(alias.Barcode),
            ["ownerUserId"] = new(alias.OwnerUserId),
            ["productId"] = new(alias.ProductId),
            ["visibility"] = new(ToWireValue(alias.Visibility)),
            ["createdAt"] = new(alias.CreatedAt.ToString("O")),
        };

        return item;
    }

    private static FoodProduct MapFoodProduct(IReadOnlyDictionary<string, AttributeValue> item)
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

    private static BarcodeAlias MapBarcodeAlias(IReadOnlyDictionary<string, AttributeValue> item) =>
        new(
            Barcode: item["barcode"].S,
            OwnerUserId: item["ownerUserId"].S,
            ProductId: item["productId"].S,
            Visibility: ParseVisibility(GetString(item, "visibility") ?? "private"),
            CreatedAt: ParseDateTime(item["createdAt"].S));

    private static void AddString(IDictionary<string, AttributeValue> item, string key, string? value)
    {
        if (!string.IsNullOrWhiteSpace(value))
        {
            item[key] = new AttributeValue(value);
        }
    }

    private static void AddNumber(IDictionary<string, AttributeValue> item, string key, int? value)
    {
        if (value is int number)
        {
            item[key] = new AttributeValue { N = number.ToString(CultureInfo.InvariantCulture) };
        }
    }

    private static void AddDateTime(IDictionary<string, AttributeValue> item, string key, DateTimeOffset? value)
    {
        if (value is DateTimeOffset timestamp)
        {
            item[key] = new AttributeValue(timestamp.ToString("O"));
        }
    }

    private static AttributeValue Number(double value) =>
        new() { N = value.ToString("G17", CultureInfo.InvariantCulture) };

    private static string? GetString(IReadOnlyDictionary<string, AttributeValue> item, string key) =>
        item.TryGetValue(key, out var value) ? value.S : null;

    private static double? GetDouble(IReadOnlyDictionary<string, AttributeValue> item, string key) =>
        item.TryGetValue(key, out var value) && !string.IsNullOrWhiteSpace(value.N)
            ? ParseDouble(value.N)
            : null;

    private static int? GetInt(IReadOnlyDictionary<string, AttributeValue> item, string key) =>
        item.TryGetValue(key, out var value) && !string.IsNullOrWhiteSpace(value.N)
            ? int.Parse(value.N, CultureInfo.InvariantCulture)
            : null;

    private static DateTimeOffset? GetDateTime(IReadOnlyDictionary<string, AttributeValue> item, string key) =>
        item.TryGetValue(key, out var value) && !string.IsNullOrWhiteSpace(value.S)
            ? ParseDateTime(value.S)
            : null;

    private static double ParseDouble(string value) =>
        double.Parse(value, CultureInfo.InvariantCulture);

    private static DateTimeOffset ParseDateTime(string value) =>
        DateTimeOffset.Parse(value, CultureInfo.InvariantCulture, DateTimeStyles.RoundtripKind);

    private static Visibility ParseVisibility(string value) =>
        value.ToLowerInvariant() switch
        {
            "shared" => Visibility.Shared,
            "global" => Visibility.Global,
            _ => Visibility.Private,
        };

    private static string ToWireValue(Visibility visibility) =>
        visibility switch
        {
            Visibility.Shared => "shared",
            Visibility.Global => "global",
            _ => "private",
        };
}
