using Amazon.DynamoDBv2;
using Amazon.DynamoDBv2.Model;
using CalorieTracker.Api.Configuration;
using CalorieTracker.Api.Contracts;

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
                ["memberUserId"] = new() { S = userId },
                ["name"] = new() { S = summary.Name },
                ["role"] = new() { S = summary.Role },
                ["members"] = new() { L = [new AttributeValue { M = new Dictionary<string, AttributeValue> { ["memberId"] = new() { S = userId }, ["name"] = new() { S = userId }, ["role"] = new() { S = "admin" } } }] },
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

    public async Task<HouseholdSummary?> JoinAsync(string userId, Guid householdId, string? name, CancellationToken cancellationToken)
    {
        var existing = await dynamoDb.QueryAsync(new QueryRequest { TableName = storage.HouseholdsTableName, IndexName = "householdId-index", KeyConditionExpression = "householdId = :householdId", ExpressionAttributeValues = new() { [":householdId"] = new() { S = householdId.ToString() } } }, cancellationToken);
        var household = existing.Items.FirstOrDefault(x => x.TryGetValue("role", out var role) && role.S == "owner");
        if (household is null) return null;
        await dynamoDb.PutItemAsync(new PutItemRequest { TableName = storage.HouseholdsTableName, Item = new Dictionary<string, AttributeValue> { ["ownerUserId"] = new() { S = userId }, ["householdId"] = new() { S = householdId.ToString() }, ["memberUserId"] = new() { S = userId }, ["name"] = new() { S = household["name"].S }, ["role"] = new() { S = "member" }, ["displayName"] = new() { S = string.IsNullOrWhiteSpace(name) ? userId : name.Trim() } } }, cancellationToken);
        return new HouseholdSummary(householdId, household["name"].S, "member");
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

    public async Task<IReadOnlyList<HouseholdMemberResponse>?> MembersAsync(string userId, Guid householdId, CancellationToken cancellationToken)
    {
        var items = await dynamoDb.QueryAsync(new QueryRequest { TableName = storage.HouseholdsTableName, IndexName = "householdId-index", KeyConditionExpression = "householdId = :householdId", ExpressionAttributeValues = new() { [":householdId"] = new() { S = householdId.ToString() } } }, cancellationToken);
        if (!items.Items.Any(x => x.TryGetValue("memberUserId", out var member) && member.S == userId)) return null;
        return items.Items.Select(x => new HouseholdMemberResponse(x["memberUserId"].S, x.TryGetValue("displayName", out var display) ? display.S : x["memberUserId"].S, x["role"].S)).ToArray();
    }

    public async Task<HouseholdMemberResponse?> AddMemberAsync(string userId, Guid householdId, string email, string? name, CancellationToken cancellationToken)
    {
        var item = await GetItem(userId, householdId, cancellationToken);
        if (item is null) return null;
        var member = new HouseholdMemberResponse(email.Trim().ToLowerInvariant(), string.IsNullOrWhiteSpace(name) ? email.Trim() : name.Trim(), "member");
        var members = MembersFrom(item).Where(x => !string.Equals(x.MemberId, member.MemberId, StringComparison.OrdinalIgnoreCase)).Append(member).ToArray();
        await SaveMembers(userId, householdId, members, cancellationToken);
        return member;
    }

    public async Task<bool> RemoveMemberAsync(string userId, Guid householdId, string memberId, CancellationToken cancellationToken)
    {
        var item = await GetItem(userId, householdId, cancellationToken);
        if (item is null || !item.TryGetValue("role", out var role) || role.S != "owner") return false;
        var target = await GetItem(memberId, householdId, cancellationToken);
        if (target is null || target.TryGetValue("role", out var targetRole) && targetRole.S == "owner") return false;
        await dynamoDb.DeleteItemAsync(new DeleteItemRequest { TableName = storage.HouseholdsTableName, Key = Key(memberId, householdId) }, cancellationToken);
        return true;
    }

    public async Task<bool> LeaveAsync(string userId, Guid householdId, CancellationToken cancellationToken)
    {
        var item = await GetItem(userId, householdId, cancellationToken);
        if (item is null) return false;
        if (item.TryGetValue("role", out var role) && role.S == "owner") return await DeleteAsync(userId, householdId, cancellationToken);
        await dynamoDb.DeleteItemAsync(new DeleteItemRequest { TableName = storage.HouseholdsTableName, Key = Key(userId, householdId) }, cancellationToken);
        return true;
    }

    public async Task<bool> DeleteAsync(string userId, Guid householdId, CancellationToken cancellationToken)
    {
        var owner = await GetItem(userId, householdId, cancellationToken);
        if (owner is null || !owner.TryGetValue("role", out var role) || role.S != "owner") return false;
        var response = await dynamoDb.DeleteItemAsync(new DeleteItemRequest { TableName = storage.HouseholdsTableName, Key = Key(userId, householdId), ReturnValues = ReturnValue.ALL_OLD }, cancellationToken);
        return response.Attributes.Count > 0;
    }

    private async Task<Dictionary<string, AttributeValue>?> GetItem(string userId, Guid householdId, CancellationToken cancellationToken) { var response = await dynamoDb.GetItemAsync(new GetItemRequest { TableName = storage.HouseholdsTableName, Key = Key(userId, householdId) }, cancellationToken); return response.Item.Count == 0 ? null : response.Item; }
    private async Task SaveMembers(string userId, Guid householdId, IReadOnlyList<HouseholdMemberResponse> members, CancellationToken cancellationToken) => await dynamoDb.UpdateItemAsync(new UpdateItemRequest { TableName = storage.HouseholdsTableName, Key = Key(userId, householdId), UpdateExpression = "SET members = :members", ExpressionAttributeValues = new Dictionary<string, AttributeValue> { [":members"] = new() { L = members.Select(x => new AttributeValue { M = new Dictionary<string, AttributeValue> { ["memberId"] = new() { S = x.MemberId }, ["name"] = new() { S = x.Name }, ["role"] = new() { S = x.Role } } }).ToList() } } }, cancellationToken);
    private static Dictionary<string, AttributeValue> Key(string userId, Guid householdId) => new() { ["ownerUserId"] = new() { S = userId }, ["householdId"] = new() { S = householdId.ToString() } };
    private static IReadOnlyList<HouseholdMemberResponse> MembersFrom(Dictionary<string, AttributeValue> item) => item.TryGetValue("members", out var value) ? value.L.Select(x => new HouseholdMemberResponse(x.M["memberId"].S, x.M["name"].S, x.M["role"].S)).ToArray() : Array.Empty<HouseholdMemberResponse>();

    private static HouseholdSummary ToSummary(Dictionary<string, AttributeValue> item) =>
        new(Guid.Parse(item["householdId"].S), item["name"].S, item["role"].S);
}

public sealed record HouseholdSummary(Guid HouseholdId, string Name, string Role);
