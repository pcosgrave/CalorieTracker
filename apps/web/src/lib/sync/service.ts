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
import { isBarcodeAliasChange, isDiaryEntryChange, isFoodProductChange, isWeightEntryChange, markRecordForSync } from "@calorie-tracker/shared";
import { barcodeAliasStorageKey, createId, foodStorageKey, recipeStorageKey, storageKey } from "@/app/lib/diary";
import { weightStorageKey } from "@/app/lib/weight";
import { getCognitoConfig } from "@/lib/auth/config";
import {
  LocalStorageSyncOutboxRepository,
  LocalStorageSyncStateRepository,
  readBarcodeAliasRecordsSync,
  readDiaryEntryRecordsSync,
  readFoodProductRecordsSync,
  readPendingSyncChangesSync,
  webDeviceIdStorageKey,
  writeBarcodeAliasRecordsSync,
  writeDiaryEntryRecordsSync,
  writeFoodProductRecordsSync,
  readWeightEntryRecordsSync,
  writeWeightEntryRecordsSync,
} from "@/lib/repositories/local-storage";

type SyncResult = {
  pushed: number;
  pulled: number;
};

function deviceId(): string {
  const existing = window.localStorage.getItem(webDeviceIdStorageKey);
  if (existing) {
    return existing;
  }

  const next = createId("device");
  window.localStorage.setItem(webDeviceIdStorageKey, next);
  return next;
}

async function pushRequest(baseUrl: string, body: object): Promise<SyncPushResponse> {
  const response = await fetch("/api/sync/push", {
    method: "POST",
    headers: {
      "content-type": "application/json",
      "x-sync-api-base-url": baseUrl,
    },
    body: JSON.stringify(body),
  });

  if (!response.ok) {
    throw new Error(`Push failed with status ${response.status}`);
  }

  return (await response.json()) as SyncPushResponse;
}

async function pullRequest(baseUrl: string, body: object): Promise<SyncPullResponse> {
  const response = await fetch("/api/sync/pull", {
    method: "POST",
    headers: {
      "content-type": "application/json",
      "x-sync-api-base-url": baseUrl,
    },
    body: JSON.stringify(body),
  });

  if (!response.ok) {
    throw new Error(`Pull failed with status ${response.status}`);
  }

  return (await response.json()) as SyncPullResponse;
}

function mergeByRecordId<TRecord extends { sync: { recordId: string; version: number } }>(records: TRecord[], incoming: TRecord): TRecord[] {
  const existing = records.find((record) => record.sync.recordId === incoming.sync.recordId);
  if (!existing) {
    return [incoming, ...records];
  }

  if (incoming.sync.version < existing.sync.version) {
    return records;
  }

  return [incoming, ...records.filter((record) => record.sync.recordId !== incoming.sync.recordId)];
}

function applyIncomingChange(change: SyncChange): void {
  const currentDeviceId = deviceId();
  if (change.operation === "delete") {
    if (isFoodProductChange(change)) {
      const deletedAt = change.changedAt;
      const ingredientRecords = readFoodProductRecordsSync(foodStorageKey, currentDeviceId);
      const recipeRecords = readFoodProductRecordsSync(recipeStorageKey, currentDeviceId);
      const applyFoodDelete = (records: FoodProductRecord[]) =>
        records.map((record) =>
          record.sync.recordId === change.recordId
            ? markRecordForSync(record, { updatedAt: deletedAt, deletedAt, syncStatus: "synced" })
            : record,
        );
      writeFoodProductRecordsSync(foodStorageKey, applyFoodDelete(ingredientRecords));
      writeFoodProductRecordsSync(recipeStorageKey, applyFoodDelete(recipeRecords));
      return;
    }

    if (isDiaryEntryChange(change)) {
      const deletedAt = change.changedAt;
      const currentRecords = readDiaryEntryRecordsSync(storageKey, currentDeviceId);
      writeDiaryEntryRecordsSync(
        storageKey,
        currentRecords.map((record) =>
          record.sync.recordId === change.recordId
            ? markRecordForSync(record, { updatedAt: deletedAt, deletedAt, syncStatus: "synced" })
            : record,
        ),
      );
      return;
    }

    if (isBarcodeAliasChange(change)) {
      const deletedAt = change.changedAt;
      const currentRecords = readBarcodeAliasRecordsSync(barcodeAliasStorageKey);
      writeBarcodeAliasRecordsSync(
        barcodeAliasStorageKey,
        currentRecords.map((record) =>
          record.sync.recordId === change.recordId
            ? markRecordForSync(record, { updatedAt: deletedAt, deletedAt, syncStatus: "synced" })
            : record,
        ),
      );
      return;
    }
  }

  if (isFoodProductChange(change) && change.payload) {
    const record = change.payload as FoodProductRecord & { product: { recipeComponents?: unknown } };
    const targetKey = record.product.recipeComponents ? recipeStorageKey : foodStorageKey;
    const currentRecords = readFoodProductRecordsSync(targetKey, currentDeviceId);
    writeFoodProductRecordsSync(targetKey, mergeByRecordId(currentRecords, record));
    return;
  }

  if (isDiaryEntryChange(change) && change.payload) {
    const record = change.payload as DiaryEntryRecord;
    const currentRecords = readDiaryEntryRecordsSync(storageKey, currentDeviceId);
    writeDiaryEntryRecordsSync(storageKey, mergeByRecordId(currentRecords, record));
    return;
  }

  if (isBarcodeAliasChange(change) && change.payload) {
    const record = change.payload as BarcodeAliasRecord;
    const currentRecords = readBarcodeAliasRecordsSync(barcodeAliasStorageKey);
    writeBarcodeAliasRecordsSync(barcodeAliasStorageKey, mergeByRecordId(currentRecords, record));
    return;
  }

  if (change.operation === "delete" && isWeightEntryChange(change)) {
    const deletedAt = change.changedAt;
    const currentRecords = readWeightEntryRecordsSync(weightStorageKey, currentDeviceId);
    writeWeightEntryRecordsSync(
      weightStorageKey,
      currentRecords.map((record) =>
        record.sync.recordId === change.recordId
          ? markRecordForSync(record, { updatedAt: deletedAt, deletedAt, syncStatus: "synced" })
          : record,
      ),
    );
    return;
  }

  if (isWeightEntryChange(change) && change.payload) {
    const record = change.payload as WeightEntryRecord;
    const currentRecords = readWeightEntryRecordsSync(weightStorageKey, currentDeviceId);
    writeWeightEntryRecordsSync(weightStorageKey, mergeByRecordId(currentRecords, record));
  }
}

export async function getSyncSettings(): Promise<SyncSettings> {
  const settings = await new LocalStorageSyncStateRepository().getSettings();
  return {
    ...settings,
    apiBaseUrl: settings.apiBaseUrl || getCognitoConfig().apiBaseUrl,
  };
}

export async function saveSyncSettings(settings: SyncSettings): Promise<void> {
  await new LocalStorageSyncStateRepository().saveSettings(settings);
}

export function getPendingSyncCount(): number {
  return readPendingSyncChangesSync().length;
}

export async function syncNow(): Promise<SyncResult> {
  const stateRepository = new LocalStorageSyncStateRepository();
  const outboxRepository = new LocalStorageSyncOutboxRepository();
  const settings = await stateRepository.getSettings();

  if (!settings.syncEnabled || !settings.apiBaseUrl) {
    throw new Error("Sync is disabled or API base URL is missing");
  }

  const cursor = await stateRepository.getCursor();
  const pendingChanges = await outboxRepository.listPendingChanges();
  const pushResponse = await pushRequest(settings.apiBaseUrl, {
    deviceId: deviceId(),
    cursor,
    changes: pendingChanges,
  });

  await outboxRepository.acknowledge(pushResponse.acceptedChangeIds);
  for (const rejection of pushResponse.rejectedChanges) {
    await outboxRepository.markRejected(rejection.changeId);
  }

  const nextCursor: SyncCursor = pushResponse.cursor;
  const pullResponse = await pullRequest(settings.apiBaseUrl, {
    deviceId: deviceId(),
    cursor: nextCursor,
  });

  for (const change of pullResponse.changes) {
    applyIncomingChange(change);
  }

  await stateRepository.saveCursor(pullResponse.cursor);
  await stateRepository.saveSettings({
    ...settings,
    lastSuccessfulSyncAt: new Date().toISOString(),
  });

  return {
    pushed: pushResponse.acceptedChangeIds.length,
    pulled: pullResponse.changes.length,
  };
}

export async function maybeAutoSync(): Promise<SyncResult | null> {
  const settings = await getSyncSettings();
  if (!settings.syncEnabled || settings.backupMode !== "automatic_backup" || !settings.apiBaseUrl) {
    return null;
  }

  try {
    return await syncNow();
  } catch {
    return null;
  }
}

export function clearLocalAppData(): void {
  const keysToRemove: string[] = [];
  for (let index = 0; index < window.localStorage.length; index += 1) {
    const key = window.localStorage.key(index);
    if (key?.startsWith("calorie-tracker:")) {
      keysToRemove.push(key);
    }
  }

  for (const key of keysToRemove) {
    window.localStorage.removeItem(key);
  }
}
