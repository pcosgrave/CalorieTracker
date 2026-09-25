using CalorieTracker.Api.Contracts;
using CalorieTracker.Api.Infrastructure;
using CalorieTracker.Api.Services.Diary;
using CalorieTracker.Api.Services.Foods;
using CalorieTracker.Api.Services.Weights;

namespace CalorieTracker.Api.Services.Sync;

public sealed class SyncService(
    IFoodRepository foodRepository,
    IDiaryRepository diaryRepository,
    IWeightRepository weightRepository,
    ISyncChangeRepository syncRepository)
{
    public async Task<SyncPushResponse> PushChangesAsync(string userId, SyncPushRequest request, CancellationToken cancellationToken)
    {
        var acceptedChangeIds = new List<string>();
        var rejectedChanges = new List<SyncChangeRejection>();

        foreach (var change in request.Changes)
        {
            var rejection = await RejectIfStaleAsync(userId, change, cancellationToken);
            if (rejection is not null)
            {
                rejectedChanges.Add(new SyncChangeRejection(change.ChangeId, "conflict", rejection));
                continue;
            }

            await MaterializeSyncChangeAsync(userId, change, cancellationToken);
            await syncRepository.AppendChangeAsync(userId, ToSyncSortKey(change.ChangedAt, change.ChangeId), change, cancellationToken);
            acceptedChangeIds.Add(change.ChangeId);
        }

        return new SyncPushResponse(
            Cursor: new SyncCursor(
                DeviceId: request.DeviceId,
                LastPulledAt: request.Changes.LastOrDefault()?.ChangedAt ?? request.Cursor?.LastPulledAt,
                LastAcknowledgedChangeId: acceptedChangeIds.LastOrDefault() ?? request.Cursor?.LastAcknowledgedChangeId),
            AcceptedChangeIds: acceptedChangeIds,
            RejectedChanges: rejectedChanges);
    }

    public async Task<SyncPullResponse> PullChangesAsync(string userId, SyncPullRequest request, CancellationToken cancellationToken)
    {
        var changes = await syncRepository.ListChangesAfterAsync(
            userId,
            ToSyncSortKey(request.Cursor?.LastPulledAt, request.Cursor?.LastAcknowledgedChangeId),
            cancellationToken);

        var lastChange = changes.LastOrDefault();
        return new SyncPullResponse(
            Cursor: new SyncCursor(
                DeviceId: request.DeviceId,
                LastPulledAt: lastChange?.ChangedAt ?? request.Cursor?.LastPulledAt,
                LastAcknowledgedChangeId: lastChange?.ChangeId ?? request.Cursor?.LastAcknowledgedChangeId),
            Changes: changes);
    }

    private async Task<string?> RejectIfStaleAsync(string userId, SyncChange change, CancellationToken cancellationToken)
    {
        var incomingUpdatedAt = GetIncomingUpdatedAt(change);
        if (incomingUpdatedAt is null)
        {
            return null;
        }

        var currentUpdatedAt = await GetCurrentUpdatedAtAsync(userId, change, cancellationToken);
        if (currentUpdatedAt is null)
        {
            return null;
        }

        if (incomingUpdatedAt < currentUpdatedAt)
        {
            return $"Incoming {change.EntityType} change is older than current server state";
        }

        return null;
    }

    private async Task MaterializeSyncChangeAsync(string userId, SyncChange change, CancellationToken cancellationToken)
    {
        if (string.Equals(change.Operation, "delete", StringComparison.OrdinalIgnoreCase))
        {
            await DeleteMaterializedRecordAsync(userId, change, cancellationToken);
            return;
        }

        await UpsertMaterializedRecordAsync(userId, change, cancellationToken);
    }

    private async Task UpsertMaterializedRecordAsync(string userId, SyncChange change, CancellationToken cancellationToken)
    {
        switch (change.EntityType)
        {
            case "food_product":
            {
                var payload = AppJson.Deserialize<FoodProductRecord>(change.Payload);
                if (payload is null) return;
                var product = payload.Product with { OwnerUserId = userId };
                await foodRepository.SaveProductAsync(product, cancellationToken);
                if (!string.IsNullOrWhiteSpace(product.Barcode))
                {
                    await foodRepository.SaveBarcodeAliasAsync(
                        new BarcodeAlias(product.Barcode!, userId, product.ProductId, Visibility.Private, product.CreatedAt),
                        cancellationToken);
                }
                return;
            }
            case "barcode_alias":
            {
                var payload = AppJson.Deserialize<BarcodeAliasRecord>(change.Payload);
                if (payload is null) return;
                await foodRepository.SaveBarcodeAliasAsync(payload.Alias with { OwnerUserId = userId }, cancellationToken);
                return;
            }
            case "diary_entry":
            {
                var payload = AppJson.Deserialize<DiaryEntryRecord>(change.Payload);
                if (payload is null) return;
                await diaryRepository.SaveEntryAsync(
                    payload.Entry with
                    {
                        OwnerUserId = userId,
                        ProductSnapshot = payload.Entry.ProductSnapshot with { OwnerUserId = userId },
                    },
                    cancellationToken);
                return;
            }
            case "weight_entry":
            {
                var payload = AppJson.Deserialize<WeightEntryRecord>(change.Payload);
                if (payload is null) return;
                await weightRepository.SaveEntryAsync(payload.Entry with { OwnerUserId = userId }, cancellationToken);
                return;
            }
        }
    }

    private async Task DeleteMaterializedRecordAsync(string userId, SyncChange change, CancellationToken cancellationToken)
    {
        switch (change.EntityType)
        {
            case "food_product":
            {
                var payload = AppJson.Deserialize<FoodProductRecord>(change.Payload);
                await foodRepository.DeleteProductAsync(userId, change.RecordId, cancellationToken);
                if (!string.IsNullOrWhiteSpace(payload?.Product.Barcode))
                {
                    await foodRepository.DeleteBarcodeAliasAsync(userId, payload.Product.Barcode!, cancellationToken);
                }
                return;
            }
            case "barcode_alias":
            {
                var payload = AppJson.Deserialize<BarcodeAliasRecord>(change.Payload);
                var barcode = payload?.Alias.Barcode ?? ParseBarcodeRecordId(change.RecordId);
                if (!string.IsNullOrWhiteSpace(barcode))
                {
                    await foodRepository.DeleteBarcodeAliasAsync(userId, barcode!, cancellationToken);
                }
                return;
            }
            case "diary_entry":
                await diaryRepository.DeleteEntryAsync(userId, change.RecordId, cancellationToken);
                return;
            case "weight_entry":
                await weightRepository.DeleteEntryAsync(userId, change.RecordId, cancellationToken);
                return;
        }
    }

    private async Task<DateTimeOffset?> GetCurrentUpdatedAtAsync(string userId, SyncChange change, CancellationToken cancellationToken)
    {
        switch (change.EntityType)
        {
            case "food_product":
                return (await foodRepository.GetProductAsync(userId, change.RecordId, cancellationToken))?.UpdatedAt;
            case "barcode_alias":
            {
                var barcode = GetBarcodeForChange(change);
                if (barcode is null) return null;
                return (await foodRepository.GetBarcodeAliasAsync(userId, barcode, cancellationToken))?.CreatedAt;
            }
            case "diary_entry":
                return (await diaryRepository.GetEntryAsync(userId, change.RecordId, cancellationToken))?.UpdatedAt;
            case "weight_entry":
                return (await weightRepository.GetEntryAsync(userId, change.RecordId, cancellationToken))?.UpdatedAt;
            default:
                return null;
        }
    }

    private static DateTimeOffset? GetIncomingUpdatedAt(SyncChange change)
    {
        return change.EntityType switch
        {
            "food_product" => AppJson.Deserialize<FoodProductRecord>(change.Payload)?.Product.UpdatedAt
                ?? AppJson.Deserialize<SyncMetadataEnvelope>(change.Payload)?.Sync?.UpdatedAt
                ?? change.ChangedAt,
            "barcode_alias" => AppJson.Deserialize<SyncMetadataEnvelope>(change.Payload)?.Sync?.UpdatedAt
                ?? change.ChangedAt,
            "diary_entry" => AppJson.Deserialize<DiaryEntryRecord>(change.Payload)?.Entry.UpdatedAt
                ?? AppJson.Deserialize<SyncMetadataEnvelope>(change.Payload)?.Sync?.UpdatedAt
                ?? change.ChangedAt,
            "weight_entry" => AppJson.Deserialize<WeightEntryRecord>(change.Payload)?.Entry.UpdatedAt
                ?? AppJson.Deserialize<SyncMetadataEnvelope>(change.Payload)?.Sync?.UpdatedAt
                ?? change.ChangedAt,
            _ => null,
        };
    }

    private static string ToSyncSortKey(DateTimeOffset? changedAt, string? changeId) =>
        $"{changedAt?.ToString("O") ?? string.Empty}#{changeId ?? string.Empty}";

    private static string? GetBarcodeForChange(SyncChange change)
    {
        if (change.EntityType == "barcode_alias")
        {
            return AppJson.Deserialize<BarcodeAliasRecord>(change.Payload)?.Alias.Barcode ?? ParseBarcodeRecordId(change.RecordId);
        }

        if (change.EntityType == "food_product")
        {
            return AppJson.Deserialize<FoodProductRecord>(change.Payload)?.Product.Barcode;
        }

        return null;
    }

    private static string? ParseBarcodeRecordId(string recordId)
    {
        var separatorIndex = recordId.IndexOf(':');
        if (separatorIndex < 0 || separatorIndex == recordId.Length - 1)
        {
            return null;
        }

        return recordId[(separatorIndex + 1)..];
    }

    private sealed record SyncMetadataEnvelope(SyncMetadata? Sync);
}
