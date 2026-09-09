using System.Text.Json;

namespace CalorieTracker.Api.Contracts;

public sealed record SyncMetadata(
    string RecordId,
    int Version,
    DateTimeOffset UpdatedAt,
    DateTimeOffset? DeletedAt,
    string OriginDeviceId,
    DateTimeOffset? LastSyncedAt,
    string SyncStatus);

public sealed record FoodProductRecord(
    FoodProduct Product,
    SyncMetadata Sync);

public sealed record BarcodeAliasRecord(
    BarcodeAlias Alias,
    SyncMetadata Sync);

public sealed record DiaryEntryRecord(
    DiaryEntry Entry,
    SyncMetadata Sync);

public sealed record WeightEntryRecord(
    WeightEntry Entry,
    SyncMetadata Sync);

public sealed record SyncCursor(
    string DeviceId,
    DateTimeOffset? LastPulledAt,
    string? LastAcknowledgedChangeId);

public sealed record SyncChange(
    string ChangeId,
    string EntityType,
    string RecordId,
    string Operation,
    DateTimeOffset ChangedAt,
    string DeviceId,
    int? BaseVersion,
    JsonElement? Payload);

public sealed record SyncPushRequest(
    string DeviceId,
    SyncCursor? Cursor,
    IReadOnlyList<SyncChange> Changes);

public sealed record SyncPullRequest(
    string DeviceId,
    SyncCursor? Cursor);

public sealed record SyncChangeRejection(
    string ChangeId,
    string Code,
    string Message);

public sealed record SyncPushResponse(
    SyncCursor Cursor,
    IReadOnlyList<string> AcceptedChangeIds,
    IReadOnlyList<SyncChangeRejection> RejectedChanges);

public sealed record SyncPullResponse(
    SyncCursor Cursor,
    IReadOnlyList<SyncChange> Changes);
