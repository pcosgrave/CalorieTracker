import type { BarcodeAlias, DiaryEntry, FoodProduct } from "./entities.js";

export type SyncStatus = "local_only" | "pending_push" | "synced" | "sync_error";

export type SyncEntityType = "food_product" | "barcode_alias" | "diary_entry";

export type SyncOperation = "upsert" | "delete";

export interface SyncMetadata {
  recordId: string;
  version: number;
  updatedAt: string;
  deletedAt?: string | undefined;
  originDeviceId: string;
  lastSyncedAt?: string | undefined;
  syncStatus: SyncStatus;
}

export interface FoodProductRecord {
  product: FoodProduct;
  sync: SyncMetadata;
}

export interface BarcodeAliasRecord {
  alias: BarcodeAlias;
  sync: SyncMetadata;
}

export interface DiaryEntryRecord {
  entry: DiaryEntry;
  sync: SyncMetadata;
}

export interface SyncCursor {
  deviceId: string;
  lastPulledAt?: string | undefined;
  lastAcknowledgedChangeId?: string | undefined;
}

export interface SyncSettings {
  syncEnabled: boolean;
  backupMode: "disabled" | "manual_backup" | "automatic_backup";
  apiBaseUrl?: string | undefined;
  lastSuccessfulSyncAt?: string | undefined;
}

export interface SyncChangeEnvelope<TPayload> {
  changeId: string;
  entityType: SyncEntityType;
  recordId: string;
  operation: SyncOperation;
  changedAt: string;
  deviceId: string;
  baseVersion?: number | undefined;
  payload?: TPayload | undefined;
}

export type FoodProductChange = SyncChangeEnvelope<FoodProductRecord>;
export type BarcodeAliasChange = SyncChangeEnvelope<BarcodeAliasRecord>;
export type DiaryEntryChange = SyncChangeEnvelope<DiaryEntryRecord>;

export type SyncChange = FoodProductChange | BarcodeAliasChange | DiaryEntryChange;

export interface SyncChangeRejection {
  changeId: string;
  code: "conflict" | "validation_error" | "not_found" | "unknown";
  message: string;
}

export interface SyncPushRequest {
  userId: string;
  deviceId: string;
  cursor?: SyncCursor | undefined;
  changes: SyncChange[];
}

export interface SyncPushResponse {
  cursor: SyncCursor;
  acceptedChangeIds: string[];
  rejectedChanges: SyncChangeRejection[];
}

export interface SyncPullRequest {
  userId: string;
  deviceId: string;
  cursor?: SyncCursor | undefined;
}

export interface SyncPullResponse {
  cursor: SyncCursor;
  changes: SyncChange[];
}
