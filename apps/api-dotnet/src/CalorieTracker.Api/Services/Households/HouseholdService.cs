using Amazon.DynamoDBv2;
using Amazon.DynamoDBv2.Model;
using CalorieTracker.Api.Configuration;

namespace CalorieTracker.Api.Services.Households;

public sealed class HouseholdService(IAmazonDynamoDB dynamoDb, StorageOptions storage)
{
    public async Task<HouseholdSummary> CreateAsync(string userId, string name, CancellationToken cancellationToken)
    {
        var householdId = Guid.NewGuid();
        var summary = new HouseholdSummary(householdId, name.Trim(), "owner");
        await dynamoDb.PutItemAsync(new PutItemRequest
        {
            TableName = storage.HouseholdsTableName,
            Item = new Dictionary<string, AttributeValue>
            {
                ["ownerUserId"] = new() { S = userId },
                ["householdId"] = new() { S = householdId.ToString() },
                ["name"] = new() { S = summary.Name },
                ["role"] = new() { S = summary.Role },
            },
            ConditionExpression = "attribute_not_exists(householdId)",
        }, cancellationToken);
        return summary;
    }

    public async Task<IReadOnlyList<HouseholdSummary>> ListAsync(string userId, CancellationToken cancellationToken)
    {
        var response = await dynamoDb.QueryAsync(new QueryRequest
        {
            TableName = storage.HouseholdsTableName,
            KeyConditionExpression = "ownerUserId = :ownerUserId",
            ExpressionAttributeValues = new Dictionary<string, AttributeValue>
            {
                [":ownerUserId"] = new() { S = userId },
            },
        }, cancellationToken);
        return response.Items.Select(ToSummary).ToArray();
    }

    public async Task<HouseholdSummary?> GetAsync(string userId, Guid householdId, CancellationToken cancellationToken)
    {
        var response = await dynamoDb.GetItemAsync(new GetItemRequest
        {
            TableName = storage.HouseholdsTableName,
            Key = new Dictionary<string, AttributeValue>
            {
                ["ownerUserId"] = new() { S = userId },
                ["householdId"] = new() { S = householdId.ToString() },
            },
        }, cancellationToken);
        return response.Item.Count == 0 ? null : ToSummary(response.Item);
    }

    private static HouseholdSummary ToSummary(Dictionary<string, AttributeValue> item) =>
        new(Guid.Parse(item["householdId"].S), item["name"].S, item["role"].S);
}

public sealed record HouseholdSummary(Guid HouseholdId, string Name, string Role);
