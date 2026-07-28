using Amazon.DynamoDBv2;
using Amazon.DynamoDBv2.Model;
using CalorieTracker.Api.Configuration;
using CalorieTracker.Api.Contracts;
using CalorieTracker.Api.Infrastructure;

namespace CalorieTracker.Api.Services.Sync;

public sealed class DynamoDbSyncChangeRepository(
    IAmazonDynamoDB dynamoDb,
    StorageOptions storageOptions) : ISyncChangeRepository
{
    public Task AppendChangeAsync(string ownerUserId, string changeKey, SyncChange change, CancellationToken cancellationToken)
    {
        var item = DynamoDbSerialization.ToAttributeMap(change);
        item["ownerUserId"] = new AttributeValue(ownerUserId);
        item["changeKey"] = new AttributeValue(changeKey);

        return dynamoDb.PutItemAsync(
            new PutItemRequest
            {
                TableName = storageOptions.SyncChangesTableName,
                Item = item,
            },
            cancellationToken);
    }

    public async Task<IReadOnlyList<SyncChange>> ListChangesAfterAsync(string ownerUserId, string changeKey, CancellationToken cancellationToken)
    {
        var response = await dynamoDb.QueryAsync(
            new QueryRequest
            {
                TableName = storageOptions.SyncChangesTableName,
                KeyConditionExpression = "ownerUserId = :ownerUserId AND changeKey > :changeKey",
                ExpressionAttributeValues = new Dictionary<string, AttributeValue>
                {
                    [":ownerUserId"] = new(ownerUserId),
                    [":changeKey"] = new(changeKey),
                },
                Limit = 200,
                ScanIndexForward = true,
            },
            cancellationToken);

        return response.Items.Select(DynamoDbSerialization.MapSyncChange).ToArray();
    }
}
