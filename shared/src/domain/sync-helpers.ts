import type {
  BarcodeAliasChange,
  BarcodeAliasRecord,
  DiaryEntryChange,
  DiaryEntryRecord,
  FoodProductChange,
  FoodProductRecord,
  SyncChange,
  SyncEntityType,
  SyncMetadata,
  SyncOperation,
  SyncStatus,
  WeightEntryChange,
  WeightEntryRecord,
} from "../contracts/sync.js";

type SyncRecordMap = {
  barcode_alias: BarcodeAliasRecord;
  diary_entry: DiaryEntryRecord;
  food_product: FoodProductRecord;
  weight_entry: WeightEntryRecord;
};

export function createSyncMetadata(params: {
  recordId: string;
  deviceId: string;
  updatedAt: string;
  version?: number;
  deletedAt?: string;
  lastSyncedAt?: string;
  syncStatus?: SyncStatus;
}): SyncMetadata {
  return {
    recordId: params.recordId,
    version: params.version ?? 1,
    updatedAt: params.updatedAt,
    deletedAt: params.deletedAt,
    originDeviceId: params.deviceId,
    lastSyncedAt: params.lastSyncedAt,
    syncStatus: params.syncStatus ?? "local_only",
  };
}

export function markRecordForSync<TRecord extends { sync: SyncMetadata }>(
  record: TRecord,
  params: {
    updatedAt: string;
    deletedAt?: string;
    nextVersion?: number;
    syncStatus?: SyncStatus;
  },
): TRecord {
  return {
    ...record,
    sync: {
      ...record.sync,
      updatedAt: params.updatedAt,
      deletedAt: params.deletedAt,
      version: params.nextVersion ?? record.sync.version + 1,
      syncStatus: params.syncStatus ?? "pending_push",
    },
  };
}

export function createSyncChange<TEntityType extends SyncEntityType>(
  entityType: TEntityType,
  params: {
    changeId: string;
    deviceId: string;
    changedAt: string;
    operation: SyncOperation;
    record: SyncRecordMap[TEntityType];
    baseVersion?: number;
  },
): Extract<SyncChange, { entityType: TEntityType }> {
  return {
    changeId: params.changeId,
    entityType,
    recordId: params.record.sync.recordId,
    operation: params.operation,
    changedAt: params.changedAt,
    deviceId: params.deviceId,
    baseVersion: params.baseVersion,
    payload: params.operation === "delete" ? undefined : params.record,
  } as Extract<SyncChange, { entityType: TEntityType }>;
}

export function isDeletedRecord(record: { sync: SyncMetadata }): boolean {
  return Boolean(record.sync.deletedAt);
}

export function isPendingSync(record: { sync: SyncMetadata }): boolean {
  return record.sync.syncStatus === "pending_push" || record.sync.syncStatus === "sync_error";
}

export function isFoodProductChange(change: SyncChange): change is FoodProductChange {
  return change.entityType === "food_product";
}

export function isBarcodeAliasChange(change: SyncChange): change is BarcodeAliasChange {
  return change.entityType === "barcode_alias";
}

export function isDiaryEntryChange(change: SyncChange): change is DiaryEntryChange {
  return change.entityType === "diary_entry";
}

export function isWeightEntryChange(change: SyncChange): change is WeightEntryChange {
  return change.entityType === "weight_entry";
}
