import type {
  BarcodeAliasRecord,
  DiaryEntryRecord,
  FoodProductRecord,
  SyncChange,
  SyncCursor,
  SyncPullResponse,
  SyncPushResponse,
  SyncSettings,
  WeightEntryRecord,
} from "@calorie-tracker/shared";

export interface FoodRepository {
  list(): Promise<FoodProductRecord[]>;
  getById(recordId: string): Promise<FoodProductRecord | null>;
  getByBarcode(barcode: string): Promise<FoodProductRecord | null>;
  save(record: FoodProductRecord): Promise<void>;
  softDelete(recordId: string, deletedAt: string): Promise<void>;
}

export interface BarcodeAliasRepository {
  list(): Promise<BarcodeAliasRecord[]>;
  getByBarcode(barcode: string): Promise<BarcodeAliasRecord | null>;
  save(record: BarcodeAliasRecord): Promise<void>;
  softDelete(recordId: string, deletedAt: string): Promise<void>;
}

export interface DiaryRepository {
  list(): Promise<DiaryEntryRecord[]>;
  listByDateRange(startDate: string, endDate: string): Promise<DiaryEntryRecord[]>;
  getById(recordId: string): Promise<DiaryEntryRecord | null>;
  save(record: DiaryEntryRecord): Promise<void>;
  softDelete(recordId: string, deletedAt: string): Promise<void>;
}

export interface WeightRepository {
  list(): Promise<WeightEntryRecord[]>;
  getById(recordId: string): Promise<WeightEntryRecord | null>;
  save(record: WeightEntryRecord): Promise<void>;
  softDelete(recordId: string, deletedAt: string): Promise<void>;
}

export interface SyncOutboxRepository {
  listPendingChanges(): Promise<SyncChange[]>;
  enqueue(change: SyncChange): Promise<void>;
  acknowledge(changeIds: string[], syncedAt: string): Promise<void>;
  markRejected(changeId: string): Promise<void>;
  clear(): Promise<void>;
}

export interface SyncStateRepository {
  getCursor(): Promise<SyncCursor | null>;
  saveCursor(cursor: SyncCursor): Promise<void>;
  getSettings(): Promise<SyncSettings>;
  saveSettings(settings: SyncSettings): Promise<void>;
}

export interface SyncTransport {
  push(changes: SyncChange[], cursor: SyncCursor | null): Promise<SyncPushResponse>;
  pull(cursor: SyncCursor | null): Promise<SyncPullResponse>;
}
