using Amazon.CognitoIdentityProvider;
using Amazon.CognitoIdentityProvider.Model;
using Amazon.DynamoDBv2;
using Amazon.DynamoDBv2.Model;
using CalorieTracker.Api.Configuration;

namespace CalorieTracker.Api.Services.Account;

public sealed class AccountService(
    IAmazonDynamoDB dynamoDb,
    IAmazonCognitoIdentityProvider cognito,
    StorageOptions storageOptions,
    CognitoOptions cognitoOptions)
{
    public async Task DeleteAccountAsync(string userId, CancellationToken cancellationToken)
    {
        await DeleteOwnedItemsAsync(storageOptions.ProductsTableName!, userId, "productId", cancellationToken);
        await DeleteOwnedItemsAsync(storageOptions.BarcodeAliasesTableName!, userId, "barcode", cancellationToken);
        await DeleteOwnedItemsAsync(storageOptions.DiaryEntriesTableName!, userId, "entryId", cancellationToken);
        await DeleteOwnedItemsAsync(storageOptions.WeightEntriesTableName!, userId, "entryId", cancellationToken);
        await DeleteOwnedItemsAsync(storageOptions.SyncChangesTableName!, userId, "changeKey", cancellationToken);

        await cognito.AdminDeleteUserAsync(
            new AdminDeleteUserRequest
            {
                UserPoolId = cognitoOptions.UserPoolId,
                Username = userId,
            },
            cancellationToken);
    }

    private async Task DeleteOwnedItemsAsync(string tableName, string ownerUserId, string rangeKeyName, CancellationToken cancellationToken)
    {
        Dictionary<string, AttributeValue>? exclusiveStartKey = null;

        do
        {
            var response = await dynamoDb.QueryAsync(
                new QueryRequest
                {
                    TableName = tableName,
                    KeyConditionExpression = "ownerUserId = :ownerUserId",
                    ExpressionAttributeValues = new Dictionary<string, AttributeValue>
                    {
                        [":ownerUserId"] = new(ownerUserId),
                    },
                    ExclusiveStartKey = exclusiveStartKey,
                },
                cancellationToken);

            foreach (var item in response.Items)
            {
                if (!item.TryGetValue(rangeKeyName, out var rangeKeyValue) || string.IsNullOrWhiteSpace(rangeKeyValue.S))
                {
                    continue;
                }

                await dynamoDb.DeleteItemAsync(
                    new DeleteItemRequest
                    {
                        TableName = tableName,
                        Key = new Dictionary<string, AttributeValue>
                        {
                            ["ownerUserId"] = new(ownerUserId),
                            [rangeKeyName] = new(rangeKeyValue.S),
                        },
                    },
                    cancellationToken);
            }

            exclusiveStartKey = response.LastEvaluatedKey;
        } while (exclusiveStartKey is not null && exclusiveStartKey.Count > 0);
    }
}
