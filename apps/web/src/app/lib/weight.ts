import type { WeightEntry, WeightEntryRecord } from "@calorie-tracker/shared";
import { createSyncMetadata, isDeletedRecord, markRecordForSync } from "@calorie-tracker/shared";
import { currentUserScope } from "@/lib/auth/client";
import {
  createPendingDeleteChange,
  createPendingUpsertChange,
  getOrCreateDeviceId,
  readPendingSyncChangesSync,
  readWeightEntryRecordsSync,
  weightStorageKey,
  writePendingSyncChangesSync,
  writeWeightEntryRecordsSync,
} from "@/lib/repositories/local-storage";

export { weightStorageKey } from "@/lib/repositories/local-storage";

export function createId(prefix: string): string {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
    return `${prefix}-${crypto.randomUUID()}`;
  }

  return `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2)}`;
}

function deviceId(): string {
  return getOrCreateDeviceId(createId);
}

function writeOutboxChange(change: ReturnType<typeof createPendingUpsertChange>): void {
  const changes = readPendingSyncChangesSync();
  writePendingSyncChangesSync([change, ...changes.filter((existing) => existing.changeId !== change.changeId)]);
}

function toWeightRecord(entry: WeightEntry, existing?: WeightEntryRecord): WeightEntryRecord {
  const updatedAt = entry.updatedAt || entry.createdAt || new Date().toISOString();
  return existing
    ? markRecordForSync(
        {
          ...existing,
          entry,
        },
        {
          updatedAt,
          nextVersion: existing.sync.version + 1,
        },
      )
    : {
        entry,
        sync: createSyncMetadata({
          recordId: entry.entryId,
          deviceId: deviceId(),
          updatedAt,
          syncStatus: "pending_push",
        }),
      };
}

export function readWeightEntryRecords(): WeightEntryRecord[] {
  return readWeightEntryRecordsSync(weightStorageKey, deviceId())
    .filter((record) => !isDeletedRecord(record))
    .sort((left, right) => right.entry.loggedAt.localeCompare(left.entry.loggedAt));
}

export function readWeightEntries(): WeightEntry[] {
  return readWeightEntryRecords().map((record) => record.entry);
}

export function writeWeightEntry(entry: WeightEntry): void {
  const existingRecords = readWeightEntryRecordsSync(weightStorageKey, deviceId());
  const existing = existingRecords.find((record) => record.sync.recordId === entry.entryId);
  const nextRecord = toWeightRecord(entry, existing);
  writeWeightEntryRecordsSync(
    weightStorageKey,
    [nextRecord, ...existingRecords.filter((record) => record.sync.recordId !== entry.entryId)],
  );
  writeOutboxChange(
    createPendingUpsertChange({
      entityType: "weight_entry",
      changeId: createId("change"),
      deviceId: deviceId(),
      changedAt: nextRecord.sync.updatedAt,
      record: nextRecord,
      ...(existing ? { baseVersion: existing.sync.version } : {}),
    }),
  );
}

export function deleteWeightEntry(entryId: string): void {
  const existingRecords = readWeightEntryRecordsSync(weightStorageKey, deviceId());
  const existing = existingRecords.find((record) => record.sync.recordId === entryId);
  if (!existing) {
    return;
  }

  const deletedAt = new Date().toISOString();
  const nextRecord = markRecordForSync(existing, { updatedAt: deletedAt, deletedAt });
  writeWeightEntryRecordsSync(
    weightStorageKey,
    [nextRecord, ...existingRecords.filter((record) => record.sync.recordId !== entryId)],
  );
  writeOutboxChange(
    createPendingDeleteChange({
      entityType: "weight_entry",
      changeId: createId("change"),
      deviceId: deviceId(),
      changedAt: deletedAt,
      record: nextRecord,
      baseVersion: existing.sync.version,
    }),
  );
}

export function createWeightEntry(loggedAt: string, weightKg: number, entryId?: string): WeightEntry {
  const now = new Date().toISOString();
  return {
    entryId: entryId ?? createId("weight"),
    ownerUserId: currentUserScope(),
    loggedAt,
    weightKg,
    source: "manual",
    createdAt: now,
    updatedAt: now,
  };
}

export function convertWeightFromKg(weightKg: number, unit: "kilograms" | "pounds"): number {
  return unit === "pounds" ? weightKg * 2.2046226218 : weightKg;
}

export function convertWeightToKg(value: number, unit: "kilograms" | "pounds"): number {
  return unit === "pounds" ? value / 2.2046226218 : value;
}
