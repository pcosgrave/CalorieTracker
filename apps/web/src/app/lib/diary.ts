import type {
  BarcodeAliasRecord,
  DiaryEntry,
  DiaryEntryRecord,
  FoodProduct,
  FoodProductRecord,
  MealType,
  Nutrients,
} from "@calorie-tracker/shared";
import { createSyncMetadata, isDeletedRecord, markRecordForSync } from "@calorie-tracker/shared";
import {
  createBarcodeAliasFromProduct,
  createPendingDeleteChange,
  createPendingUpsertChange,
  getOrCreateDeviceId,
  readBarcodeAliasRecordsSync,
  readDiaryEntryRecordsSync,
  readFoodProductRecordsSync,
  readPendingSyncChangesSync,
  syncOutboxStorageKey,
  writeBarcodeAliasRecordsSync,
  writeDiaryEntryRecordsSync,
  writeFoodProductRecordsSync,
  writePendingSyncChangesSync,
} from "@/lib/repositories/local-storage";

export const storageKey = "calorie-tracker:diary:v1";
export const foodStorageKey = "calorie-tracker:foods:v1";
export const recipeStorageKey = "calorie-tracker:recipes:v1";
export const barcodeAliasStorageKey = "calorie-tracker:barcode-aliases:v1";
export const recipeDraftStorageKey = "calorie-tracker:recipe-draft:v1";
export const ownerUserId = "local";

export const mealOrder: MealType[] = ["breakfast", "lunch", "dinner", "snack"];

export const mealLabels: Record<MealType, string> = {
  breakfast: "Breakfast",
  lunch: "Lunch",
  dinner: "Dinner",
  snack: "Snack",
};

export type NutritionTotals = {
  calories: number;
  protein: number;
  carbs: number;
  fat: number;
};

export const emptyTotals: NutritionTotals = {
  calories: 0,
  protein: 0,
  carbs: 0,
  fat: 0,
};

export function toNumber(value: string): number {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : 0;
}

export function round(value: number): number {
  return Math.round(value * 10) / 10;
}

export function scaleNutrients(nutrients: Nutrients, multiplier: number): Nutrients {
  return {
    calories: round(nutrients.calories * multiplier),
    proteinGrams: round(nutrients.proteinGrams * multiplier),
    carbohydrateGrams: round(nutrients.carbohydrateGrams * multiplier),
    fatGrams: round(nutrients.fatGrams * multiplier),
  };
}

export function totalsForEntries(entries: DiaryEntry[]): NutritionTotals {
  return entries.reduce((acc, entry) => {
    const nutrients = scaleNutrients(entry.productSnapshot.nutrients, entry.servingMultiplier);

    return {
      calories: round(acc.calories + nutrients.calories),
      protein: round(acc.protein + nutrients.proteinGrams),
      carbs: round(acc.carbs + nutrients.carbohydrateGrams),
      fat: round(acc.fat + nutrients.fatGrams),
    };
  }, emptyTotals);
}

export function createId(prefix: string): string {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
    return `${prefix}-${crypto.randomUUID()}`;
  }

  return `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2)}`;
}

function deviceId(): string {
  return getOrCreateDeviceId(createId);
}

export function todayDateKey(): string {
  const now = new Date();
  return toDateKey(now);
}

export function toDateKey(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");

  return `${year}-${month}-${day}`;
}

export function entryDateKey(entry: DiaryEntry): string {
  return entry.loggedAt.slice(0, 10);
}

export function shiftDateKey(dateKey: string, offsetDays: number): string {
  const date = new Date(`${dateKey}T12:00:00`);
  date.setDate(date.getDate() + offsetDays);
  return toDateKey(date);
}

export function formatDateHeading(dateKey: string): string {
  const today = todayDateKey();
  if (dateKey === today) {
    return "Today";
  }

  if (dateKey === shiftDateKey(today, -1)) {
    return "Yesterday";
  }

  if (dateKey === shiftDateKey(today, 1)) {
    return "Tomorrow";
  }

  return new Intl.DateTimeFormat("en", {
    weekday: "long",
    month: "short",
    day: "numeric",
  }).format(new Date(`${dateKey}T12:00:00`));
}

export function formatDateChip(dateKey: string): { day: string; label: string } {
  const date = new Date(`${dateKey}T12:00:00`);

  return {
    day: new Intl.DateTimeFormat("en", { weekday: "short" }).format(date),
    label: new Intl.DateTimeFormat("en", { month: "short", day: "numeric" }).format(date),
  };
}

function readDiaryEntryRecords(): DiaryEntryRecord[] {
  return readDiaryEntryRecordsSync(storageKey, deviceId());
}

function readFoodProductRecords(storage: string): FoodProductRecord[] {
  return readFoodProductRecordsSync(storage, deviceId());
}

function readBarcodeAliasRecords(): BarcodeAliasRecord[] {
  return readBarcodeAliasRecordsSync(barcodeAliasStorageKey);
}

function writeOutboxChange(change: ReturnType<typeof createPendingUpsertChange>): void {
  const changes = readPendingSyncChangesSync();
  writePendingSyncChangesSync([change, ...changes.filter((existing) => existing.changeId !== change.changeId)]);
}

function persistProductRecords(storage: string, records: FoodProductRecord[]): void {
  writeFoodProductRecordsSync(storage, records);
}

function persistDiaryRecords(records: DiaryEntryRecord[]): void {
  writeDiaryEntryRecordsSync(storageKey, records);
}

function persistBarcodeAliasRecords(records: BarcodeAliasRecord[]): void {
  writeBarcodeAliasRecordsSync(barcodeAliasStorageKey, records);
}

function toFoodRecord(product: FoodProduct, existing?: FoodProductRecord): FoodProductRecord {
  const updatedAt = product.updatedAt || new Date().toISOString();
  return existing
    ? markRecordForSync(
        {
          ...existing,
          product,
        },
        {
          updatedAt,
          nextVersion: existing.sync.version + 1,
        },
      )
    : {
        product,
        sync: createSyncMetadata({
          recordId: product.productId,
          deviceId: deviceId(),
          updatedAt,
          syncStatus: "pending_push",
        }),
      };
}

function toDiaryRecord(entry: DiaryEntry, existing?: DiaryEntryRecord): DiaryEntryRecord {
  const updatedAt = entry.updatedAt || new Date().toISOString();
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

function syncBarcodeAliasForProduct(product: FoodProduct): void {
  if (!product.barcode) {
    return;
  }

  const aliasRecord = createBarcodeAliasFromProduct(product, deviceId());
  if (!aliasRecord) {
    return;
  }

  const records = readBarcodeAliasRecords();
  const existing = records.find((record) => record.sync.recordId === aliasRecord.sync.recordId);
  const nextRecord = existing
    ? markRecordForSync(
        {
          ...existing,
          alias: aliasRecord.alias,
        },
        {
          updatedAt: product.updatedAt,
          nextVersion: existing.sync.version + 1,
        },
      )
    : aliasRecord;

  persistBarcodeAliasRecords([nextRecord, ...records.filter((record) => record.sync.recordId !== nextRecord.sync.recordId)]);
  writeOutboxChange(
    createPendingUpsertChange({
      entityType: "barcode_alias",
      changeId: createId("change"),
      deviceId: deviceId(),
      changedAt: nextRecord.sync.updatedAt,
      record: nextRecord,
      ...(existing ? { baseVersion: existing.sync.version } : {}),
    }),
  );
}

export function readDiaryEntries(): DiaryEntry[] {
  return readDiaryEntryRecords()
    .filter((record) => !isDeletedRecord(record))
    .map((record) => record.entry);
}

export function writeDiaryEntries(entries: DiaryEntry[]): void {
  const existingRecords = readDiaryEntryRecords();
  const existingById = new Map(existingRecords.map((record) => [record.sync.recordId, record]));
  const nextIds = new Set(entries.map((entry) => entry.entryId));
  const updatedRecords = entries.map((entry) => toDiaryRecord(entry, existingById.get(entry.entryId)));

  const deletedRecords = existingRecords
    .filter((record) => !nextIds.has(record.sync.recordId) && !isDeletedRecord(record))
    .map((record) =>
      markRecordForSync(record, {
        updatedAt: new Date().toISOString(),
        deletedAt: new Date().toISOString(),
      }),
    );

  persistDiaryRecords([...updatedRecords, ...deletedRecords]);

  for (const record of updatedRecords) {
    writeOutboxChange(
      createPendingUpsertChange({
        entityType: "diary_entry",
        changeId: createId("change"),
        deviceId: deviceId(),
        changedAt: record.sync.updatedAt,
        record,
        ...(existingById.get(record.sync.recordId) ? { baseVersion: existingById.get(record.sync.recordId)!.sync.version } : {}),
      }),
    );
  }

  for (const record of deletedRecords) {
    writeOutboxChange(
      createPendingDeleteChange({
        entityType: "diary_entry",
        changeId: createId("change"),
        deviceId: deviceId(),
        changedAt: record.sync.updatedAt,
        record,
        ...(existingById.get(record.sync.recordId) ? { baseVersion: existingById.get(record.sync.recordId)!.sync.version } : {}),
      }),
    );
  }
}

export function writeDiaryEntry(product: FoodProduct, date: string, meal: MealType, servingMultiplier: number): void {
  const now = new Date().toISOString();
  const entry: DiaryEntry = {
    entryId: createId("entry"),
    ownerUserId,
    productId: product.productId,
    loggedAt: `${date}T12:00:00.000Z`,
    meal,
    servingMultiplier,
    productSnapshot: product,
    createdAt: now,
    updatedAt: now,
  };

  writeDiaryEntries([entry, ...readDiaryEntries()]);
}

export function readFoodProducts(): FoodProduct[] {
  return readFoodProductRecords(foodStorageKey)
    .filter((record) => !isDeletedRecord(record))
    .map((record) => record.product);
}

export function writeFoodProduct(product: FoodProduct): void {
  const existingRecords = readFoodProductRecords(foodStorageKey);
  const existing = existingRecords.find((record) => record.sync.recordId === product.productId);
  const nextRecord = toFoodRecord(product, existing);
  persistProductRecords(foodStorageKey, [nextRecord, ...existingRecords.filter((record) => record.sync.recordId !== product.productId)]);
  writeOutboxChange(
    createPendingUpsertChange({
      entityType: "food_product",
      changeId: createId("change"),
      deviceId: deviceId(),
      changedAt: nextRecord.sync.updatedAt,
      record: nextRecord,
      ...(existing ? { baseVersion: existing.sync.version } : {}),
    }),
  );
  syncBarcodeAliasForProduct(product);
}

export function deleteFoodProduct(productId: string): void {
  const existingRecords = readFoodProductRecords(foodStorageKey);
  const existing = existingRecords.find((record) => record.sync.recordId === productId);
  if (!existing) {
    return;
  }

  const deletedAt = new Date().toISOString();
  const nextRecord = markRecordForSync(existing, { updatedAt: deletedAt, deletedAt });
  persistProductRecords(foodStorageKey, [nextRecord, ...existingRecords.filter((record) => record.sync.recordId !== productId)]);
  writeOutboxChange(
    createPendingDeleteChange({
      entityType: "food_product",
      changeId: createId("change"),
      deviceId: deviceId(),
      changedAt: deletedAt,
      record: nextRecord,
      baseVersion: existing.sync.version,
    }),
  );
}

export function readRecipeProducts(): FoodProduct[] {
  return readFoodProductRecords(recipeStorageKey)
    .filter((record) => !isDeletedRecord(record))
    .map((record) => record.product);
}

export function writeRecipeProduct(product: FoodProduct): void {
  const existingRecords = readFoodProductRecords(recipeStorageKey);
  const existing = existingRecords.find((record) => record.sync.recordId === product.productId);
  const nextRecord = toFoodRecord(product, existing);
  persistProductRecords(recipeStorageKey, [nextRecord, ...existingRecords.filter((record) => record.sync.recordId !== product.productId)]);
  writeOutboxChange(
    createPendingUpsertChange({
      entityType: "food_product",
      changeId: createId("change"),
      deviceId: deviceId(),
      changedAt: nextRecord.sync.updatedAt,
      record: nextRecord,
      ...(existing ? { baseVersion: existing.sync.version } : {}),
    }),
  );
}

export function deleteRecipeProduct(productId: string): void {
  const existingRecords = readFoodProductRecords(recipeStorageKey);
  const existing = existingRecords.find((record) => record.sync.recordId === productId);
  if (!existing) {
    return;
  }

  const deletedAt = new Date().toISOString();
  const nextRecord = markRecordForSync(existing, { updatedAt: deletedAt, deletedAt });
  persistProductRecords(recipeStorageKey, [nextRecord, ...existingRecords.filter((record) => record.sync.recordId !== productId)]);
  writeOutboxChange(
    createPendingDeleteChange({
      entityType: "food_product",
      changeId: createId("change"),
      deviceId: deviceId(),
      changedAt: deletedAt,
      record: nextRecord,
      baseVersion: existing.sync.version,
    }),
  );
}

export function readPendingSyncChanges() {
  return readPendingSyncChangesSync();
}

export function clearPendingSyncChanges(): void {
  writePendingSyncChangesSync([]);
}

export function entriesForDate(entries: DiaryEntry[], dateKey: string): DiaryEntry[] {
  return entries.filter((entry) => entryDateKey(entry) === dateKey);
}
