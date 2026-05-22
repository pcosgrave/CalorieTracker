import type {
  BarcodeAliasRecord,
  DiaryEntry,
  DiaryEntryRecord,
  FoodProduct,
  FoodProductRecord,
  SyncChange,
  SyncCursor,
  SyncSettings,
} from "@calorie-tracker/shared";
import {
  createSyncChange,
  createSyncMetadata,
  isDeletedRecord,
  markRecordForSync,
} from "@calorie-tracker/shared";
import { currentUserScope } from "@/lib/auth/client";
import type {
  BarcodeAliasRepository,
  DiaryRepository,
  FoodRepository,
  SyncOutboxRepository,
  SyncStateRepository,
} from "./contracts";

export const webDeviceIdStorageKey = "calorie-tracker:sync-device-id:v1";
export const syncOutboxStorageKey = "calorie-tracker:sync-outbox:v1";
export const syncCursorStorageKey = "calorie-tracker:sync-cursor:v1";
export const syncSettingsStorageKey = "calorie-tracker:sync-settings:v1";

function scopeKey(baseKey: string): string {
  return `${baseKey}:${currentUserScope()}`;
}

type RecordId = string;

type RecordStorage<TRecord> = {
  key: string;
  getRecordId: (record: TRecord) => RecordId;
  fromLegacyItem?: (item: unknown) => TRecord;
};

function safeParse<T>(value: string | null): T | null {
  if (!value) {
    return null;
  }

  try {
    return JSON.parse(value) as T;
  } catch {
    return null;
  }
}

function readArray<T>(key: string): T[] {
  const parsed = safeParse<unknown[]>(window.localStorage.getItem(scopeKey(key)));
  return Array.isArray(parsed) ? (parsed as T[]) : [];
}

function writeArray<T>(key: string, items: T[]): void {
  window.localStorage.setItem(scopeKey(key), JSON.stringify(items));
}

function isSyncRecord(value: unknown): value is { sync: { recordId: string } } {
  return Boolean(
    value &&
      typeof value === "object" &&
      "sync" in value &&
      typeof (value as { sync?: { recordId?: string } }).sync?.recordId === "string",
  );
}

function nowIso(): string {
  return new Date().toISOString();
}

export function getOrCreateDeviceId(createId: (prefix: string) => string): string {
  const existing = window.localStorage.getItem(webDeviceIdStorageKey);
  if (existing) {
    return existing;
  }

  const next = createId("device");
  window.localStorage.setItem(webDeviceIdStorageKey, next);
  return next;
}

function createMigratedFoodRecord(product: FoodProduct, deviceId: string): FoodProductRecord {
  return {
    product,
    sync: createSyncMetadata({
      recordId: product.productId,
      deviceId,
      updatedAt: product.updatedAt || product.createdAt || nowIso(),
    }),
  };
}

function createMigratedDiaryRecord(entry: DiaryEntry, deviceId: string): DiaryEntryRecord {
  return {
    entry,
    sync: createSyncMetadata({
      recordId: entry.entryId,
      deviceId,
      updatedAt: entry.updatedAt || entry.createdAt || nowIso(),
    }),
  };
}

function createMigratedBarcodeAliasRecord(
  params: {
    barcode: string;
    productId: string;
    ownerUserId: string;
    visibility: "private" | "shared" | "global";
    createdAt: string;
  },
  deviceId: string,
): BarcodeAliasRecord {
  return {
    alias: {
      barcode: params.barcode,
      productId: params.productId,
      ownerUserId: params.ownerUserId,
      visibility: params.visibility,
      createdAt: params.createdAt,
    },
    sync: createSyncMetadata({
      recordId: `${params.ownerUserId}:${params.barcode}`,
      deviceId,
      updatedAt: params.createdAt,
    }),
  };
}

function loadRecords<TRecord>(config: RecordStorage<TRecord>): TRecord[] {
  const rawItems = readArray<unknown>(config.key);
  return rawItems.flatMap((item) => {
    if (isSyncRecord(item)) {
      return [item as TRecord];
    }

    if (config.fromLegacyItem) {
      const migrated = config.fromLegacyItem(item);
      return migrated ? [migrated] : [];
    }

    return [];
  });
}

function saveRecords<TRecord>(config: RecordStorage<TRecord>, records: TRecord[]): void {
  writeArray(config.key, records);
}

class LocalStorageFoodRepository implements FoodRepository {
  constructor(private readonly config: RecordStorage<FoodProductRecord>) {}

  async list(): Promise<FoodProductRecord[]> {
    return loadRecords(this.config).filter((record) => !isDeletedRecord(record));
  }

  async getById(recordId: string): Promise<FoodProductRecord | null> {
    return loadRecords(this.config).find((record) => record.sync.recordId === recordId && !isDeletedRecord(record)) ?? null;
  }

  async getByBarcode(barcode: string): Promise<FoodProductRecord | null> {
    return loadRecords(this.config).find((record) => record.product.barcode === barcode && !isDeletedRecord(record)) ?? null;
  }

  async save(record: FoodProductRecord): Promise<void> {
    const records = loadRecords(this.config);
    const next = [record, ...records.filter((existing) => existing.sync.recordId !== record.sync.recordId)];
    saveRecords(this.config, next);
  }

  async softDelete(recordId: string, deletedAt: string): Promise<void> {
    const records = loadRecords(this.config);
    const next = records.map((record) =>
      record.sync.recordId === recordId
        ? markRecordForSync(record, {
            deletedAt,
            updatedAt: deletedAt,
          })
        : record,
    );
    saveRecords(this.config, next);
  }
}

class LocalStorageDiaryRepository implements DiaryRepository {
  constructor(private readonly config: RecordStorage<DiaryEntryRecord>) {}

  async list(): Promise<DiaryEntryRecord[]> {
    return loadRecords(this.config).filter((record) => !isDeletedRecord(record));
  }

  async listByDateRange(startDate: string, endDate: string): Promise<DiaryEntryRecord[]> {
    return (await this.list()).filter((record) => {
      const dateKey = record.entry.loggedAt.slice(0, 10);
      return dateKey >= startDate && dateKey <= endDate;
    });
  }

  async getById(recordId: string): Promise<DiaryEntryRecord | null> {
    return loadRecords(this.config).find((record) => record.sync.recordId === recordId && !isDeletedRecord(record)) ?? null;
  }

  async save(record: DiaryEntryRecord): Promise<void> {
    const records = loadRecords(this.config);
    const next = [record, ...records.filter((existing) => existing.sync.recordId !== record.sync.recordId)];
    saveRecords(this.config, next);
  }

  async softDelete(recordId: string, deletedAt: string): Promise<void> {
    const records = loadRecords(this.config);
    const next = records.map((record) =>
      record.sync.recordId === recordId
        ? markRecordForSync(record, {
            deletedAt,
            updatedAt: deletedAt,
          })
        : record,
    );
    saveRecords(this.config, next);
  }
}

class LocalStorageBarcodeAliasRepository implements BarcodeAliasRepository {
  constructor(private readonly config: RecordStorage<BarcodeAliasRecord>) {}

  async list(): Promise<BarcodeAliasRecord[]> {
    return loadRecords(this.config).filter((record) => !isDeletedRecord(record));
  }

  async getByBarcode(barcode: string): Promise<BarcodeAliasRecord | null> {
    return loadRecords(this.config).find((record) => record.alias.barcode === barcode && !isDeletedRecord(record)) ?? null;
  }

  async save(record: BarcodeAliasRecord): Promise<void> {
    const records = loadRecords(this.config);
    const next = [record, ...records.filter((existing) => existing.sync.recordId !== record.sync.recordId)];
    saveRecords(this.config, next);
  }

  async softDelete(recordId: string, deletedAt: string): Promise<void> {
    const records = loadRecords(this.config);
    const next = records.map((record) =>
      record.sync.recordId === recordId
        ? markRecordForSync(record, {
            deletedAt,
            updatedAt: deletedAt,
          })
        : record,
    );
    saveRecords(this.config, next);
  }
}

export class LocalStorageSyncOutboxRepository implements SyncOutboxRepository {
  async listPendingChanges(): Promise<SyncChange[]> {
    return readArray<SyncChange>(syncOutboxStorageKey);
  }

  async enqueue(change: SyncChange): Promise<void> {
    const changes = await this.listPendingChanges();
    writeArray(syncOutboxStorageKey, [change, ...changes.filter((existing) => existing.changeId !== change.changeId)]);
  }

  async acknowledge(changeIds: string[]): Promise<void> {
    const changeIdSet = new Set(changeIds);
    const changes = await this.listPendingChanges();
    writeArray(
      syncOutboxStorageKey,
      changes.filter((change) => !changeIdSet.has(change.changeId)),
    );
  }

  async markRejected(changeId: string): Promise<void> {
    const changes = await this.listPendingChanges();
    writeArray(
      syncOutboxStorageKey,
      changes.filter((change) => change.changeId !== changeId),
    );
  }

  async clear(): Promise<void> {
    writeArray(syncOutboxStorageKey, []);
  }
}

export class LocalStorageSyncStateRepository implements SyncStateRepository {
  async getCursor(): Promise<SyncCursor | null> {
    return safeParse<SyncCursor>(window.localStorage.getItem(scopeKey(syncCursorStorageKey)));
  }

  async saveCursor(cursor: SyncCursor): Promise<void> {
    window.localStorage.setItem(scopeKey(syncCursorStorageKey), JSON.stringify(cursor));
  }

  async getSettings(): Promise<SyncSettings> {
    return (
      safeParse<SyncSettings>(window.localStorage.getItem(scopeKey(syncSettingsStorageKey))) ?? {
        syncEnabled: false,
        backupMode: "disabled",
        apiBaseUrl: "",
      }
    );
  }

  async saveSettings(settings: SyncSettings): Promise<void> {
    window.localStorage.setItem(scopeKey(syncSettingsStorageKey), JSON.stringify(settings));
  }
}

export function createFoodRepository(params: {
  storageKey: string;
  deviceId: string;
}): FoodRepository {
  return new LocalStorageFoodRepository({
    key: params.storageKey,
    getRecordId: (record) => record.sync.recordId,
    fromLegacyItem: (item) => createMigratedFoodRecord(item as FoodProduct, params.deviceId),
  });
}

export function createDiaryRepository(params: {
  storageKey: string;
  deviceId: string;
}): DiaryRepository {
  return new LocalStorageDiaryRepository({
    key: params.storageKey,
    getRecordId: (record) => record.sync.recordId,
    fromLegacyItem: (item) => createMigratedDiaryRecord(item as DiaryEntry, params.deviceId),
  });
}

export function createBarcodeAliasRepository(params: {
  storageKey: string;
  deviceId: string;
}): BarcodeAliasRepository {
  return new LocalStorageBarcodeAliasRepository({
    key: params.storageKey,
    getRecordId: (record) => record.sync.recordId,
    fromLegacyItem: (item) => item as BarcodeAliasRecord,
  });
}

export function createBarcodeAliasFromProduct(product: FoodProduct, deviceId: string): BarcodeAliasRecord | null {
  if (!product.barcode) {
    return null;
  }

  return createMigratedBarcodeAliasRecord(
    {
      barcode: product.barcode,
      productId: product.productId,
      ownerUserId: product.ownerUserId,
      visibility: product.visibility,
      createdAt: product.updatedAt || product.createdAt || nowIso(),
    },
    deviceId,
  );
}

export function createPendingUpsertChange(params: {
  entityType: "food_product" | "barcode_alias" | "diary_entry";
  changeId: string;
  deviceId: string;
  changedAt: string;
  record: FoodProductRecord | BarcodeAliasRecord | DiaryEntryRecord;
  baseVersion?: number;
}): SyncChange {
  return createSyncChange(params.entityType, {
    changeId: params.changeId,
    deviceId: params.deviceId,
    changedAt: params.changedAt,
    operation: "upsert",
    record: params.record as never,
    ...(params.baseVersion === undefined ? {} : { baseVersion: params.baseVersion }),
  });
}

export function createPendingDeleteChange(params: {
  entityType: "food_product" | "barcode_alias" | "diary_entry";
  changeId: string;
  deviceId: string;
  changedAt: string;
  record: FoodProductRecord | BarcodeAliasRecord | DiaryEntryRecord;
  baseVersion?: number;
}): SyncChange {
  return createSyncChange(params.entityType, {
    changeId: params.changeId,
    deviceId: params.deviceId,
    changedAt: params.changedAt,
    operation: "delete",
    record: params.record as never,
    ...(params.baseVersion === undefined ? {} : { baseVersion: params.baseVersion }),
  });
}

export function readFoodProductRecordsSync(storageKey: string, deviceId: string): FoodProductRecord[] {
  return loadRecords({
    key: storageKey,
    getRecordId: (record: FoodProductRecord) => record.sync.recordId,
    fromLegacyItem: (item) => createMigratedFoodRecord(item as FoodProduct, deviceId),
  });
}

export function writeFoodProductRecordsSync(storageKey: string, records: FoodProductRecord[]): void {
  saveRecords(
    {
      key: storageKey,
      getRecordId: (record: FoodProductRecord) => record.sync.recordId,
    },
    records,
  );
}

export function readDiaryEntryRecordsSync(storageKey: string, deviceId: string): DiaryEntryRecord[] {
  return loadRecords({
    key: storageKey,
    getRecordId: (record: DiaryEntryRecord) => record.sync.recordId,
    fromLegacyItem: (item) => createMigratedDiaryRecord(item as DiaryEntry, deviceId),
  });
}

export function writeDiaryEntryRecordsSync(storageKey: string, records: DiaryEntryRecord[]): void {
  saveRecords(
    {
      key: storageKey,
      getRecordId: (record: DiaryEntryRecord) => record.sync.recordId,
    },
    records,
  );
}

export function readBarcodeAliasRecordsSync(storageKey: string): BarcodeAliasRecord[] {
  return loadRecords({
    key: storageKey,
    getRecordId: (record: BarcodeAliasRecord) => record.sync.recordId,
    fromLegacyItem: (item) => item as BarcodeAliasRecord,
  });
}

export function writeBarcodeAliasRecordsSync(storageKey: string, records: BarcodeAliasRecord[]): void {
  saveRecords(
    {
      key: storageKey,
      getRecordId: (record: BarcodeAliasRecord) => record.sync.recordId,
    },
    records,
  );
}

export function readPendingSyncChangesSync(): SyncChange[] {
  return readArray<SyncChange>(syncOutboxStorageKey);
}

export function writePendingSyncChangesSync(changes: SyncChange[]): void {
  writeArray(syncOutboxStorageKey, changes);
}
