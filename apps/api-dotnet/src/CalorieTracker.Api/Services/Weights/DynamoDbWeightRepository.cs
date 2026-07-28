using Amazon.DynamoDBv2;
using Amazon.DynamoDBv2.Model;
using CalorieTracker.Api.Configuration;
using CalorieTracker.Api.Contracts;
using CalorieTracker.Api.Infrastructure;

namespace CalorieTracker.Api.Services.Weights;

public sealed class DynamoDbWeightRepository(
    IAmazonDynamoDB dynamoDb,
    StorageOptions storageOptions) : IWeightRepository
{
    public async Task<WeightEntry?> GetEntryAsync(string ownerUserId, string entryId, CancellationToken cancellationToken)
    {
        var response = await dynamoDb.GetItemAsync(
            new GetItemRequest
            {
                TableName = storageOptions.WeightEntriesTableName,
                Key = new Dictionary<string, AttributeValue>
                {
                    ["ownerUserId"] = new(ownerUserId),
                    ["entryId"] = new(entryId),
                },
            },
            cancellationToken);

        return response.Item.Count == 0 ? null : DynamoDbSerialization.MapWeightEntry(response.Item);
    }

    public async Task<IReadOnlyList<WeightEntry>> ListEntriesAsync(string ownerUserId, CancellationToken cancellationToken)
    {
        var response = await dynamoDb.QueryAsync(
            new QueryRequest
            {
                TableName = storageOptions.WeightEntriesTableName,
                KeyConditionExpression = "ownerUserId = :ownerUserId",
                ExpressionAttributeValues = new Dictionary<string, AttributeValue>
                {
                    [":ownerUserId"] = new(ownerUserId),
                },
                Limit = 365,
                ScanIndexForward = false,
            },
            cancellationToken);

        return response.Items.Select(DynamoDbSerialization.MapWeightEntry).ToArray();
    }

    public Task SaveEntryAsync(WeightEntry entry, CancellationToken cancellationToken) =>
        dynamoDb.PutItemAsync(
            new PutItemRequest
            {
                TableName = storageOptions.WeightEntriesTableName,
                Item = DynamoDbSerialization.ToAttributeMap(entry),
            },
            cancellationToken);

    public Task DeleteEntryAsync(string ownerUserId, string entryId, CancellationToken cancellationToken) =>
        dynamoDb.DeleteItemAsync(
            new DeleteItemRequest
            {
                TableName = storageOptions.WeightEntriesTableName,
                Key = new Dictionary<string, AttributeValue>
                {
                    ["ownerUserId"] = new(ownerUserId),
                    ["entryId"] = new(entryId),
                },
            },
            cancellationToken);
}
