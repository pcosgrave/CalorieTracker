using Amazon.DynamoDBv2;
using Amazon.DynamoDBv2.Model;
using CalorieTracker.Api.Configuration;
using CalorieTracker.Api.Contracts;
using CalorieTracker.Api.Infrastructure;

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
                Item = DynamoDbSerialization.ToAttributeMap(product),
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
                Item = DynamoDbSerialization.ToAttributeMap(alias),
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

    private static FoodProduct MapFoodProduct(IReadOnlyDictionary<string, AttributeValue> item)
        => DynamoDbSerialization.MapFoodProduct(item);

    private static BarcodeAlias MapBarcodeAlias(IReadOnlyDictionary<string, AttributeValue> item) =>
        DynamoDbSerialization.MapBarcodeAlias(item);
}
