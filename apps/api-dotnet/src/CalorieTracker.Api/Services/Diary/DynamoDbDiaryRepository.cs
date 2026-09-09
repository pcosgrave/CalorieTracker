using Amazon.DynamoDBv2;
using Amazon.DynamoDBv2.Model;
using CalorieTracker.Api.Configuration;
using CalorieTracker.Api.Contracts;
using CalorieTracker.Api.Infrastructure;

namespace CalorieTracker.Api.Services.Diary;

public sealed class DynamoDbDiaryRepository(
    IAmazonDynamoDB dynamoDb,
    StorageOptions storageOptions) : IDiaryRepository
{
    public async Task<DiaryEntry?> GetEntryAsync(string ownerUserId, string entryId, CancellationToken cancellationToken)
    {
        var response = await dynamoDb.GetItemAsync(
            new GetItemRequest
            {
                TableName = storageOptions.DiaryEntriesTableName,
                Key = new Dictionary<string, AttributeValue>
                {
                    ["ownerUserId"] = new(ownerUserId),
                    ["entryId"] = new(entryId),
                },
            },
            cancellationToken);

        return response.Item.Count == 0 ? null : DynamoDbSerialization.MapDiaryEntry(response.Item);
    }

    public Task SaveEntryAsync(DiaryEntry entry, CancellationToken cancellationToken) =>
        dynamoDb.PutItemAsync(
            new PutItemRequest
            {
                TableName = storageOptions.DiaryEntriesTableName,
                Item = DynamoDbSerialization.ToAttributeMap(entry),
            },
            cancellationToken);

    public Task DeleteEntryAsync(string ownerUserId, string entryId, CancellationToken cancellationToken) =>
        dynamoDb.DeleteItemAsync(
            new DeleteItemRequest
            {
                TableName = storageOptions.DiaryEntriesTableName,
                Key = new Dictionary<string, AttributeValue>
                {
                    ["ownerUserId"] = new(ownerUserId),
                    ["entryId"] = new(entryId),
                },
            },
            cancellationToken);
}
