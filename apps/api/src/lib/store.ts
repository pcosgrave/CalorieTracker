import { DynamoDBClient } from "@aws-sdk/client-dynamodb";
import { DynamoDBDocumentClient, GetCommand, PutCommand, QueryCommand } from "@aws-sdk/lib-dynamodb";
import type {
  BarcodeLookupResponse,
  CreateDiaryEntryRequest,
  CreateFoodProductRequest,
  CreateWeightEntryRequest,
  DiaryEntry,
  FoodProduct,
  SyncChange,
  SyncCursor,
  SyncPullRequest,
  SyncPullResponse,
  SyncPushRequest,
  SyncPushResponse,
  WeightEntry,
} from "@calorie-tracker/shared";
import { randomUUID } from "node:crypto";

const client = DynamoDBDocumentClient.from(new DynamoDBClient({}));

const productsTableName = requiredEnv("PRODUCTS_TABLE_NAME");
const barcodeAliasesTableName = requiredEnv("BARCODE_ALIASES_TABLE_NAME");
const diaryEntriesTableName = requiredEnv("DIARY_ENTRIES_TABLE_NAME");
const weightEntriesTableName = requiredEnv("WEIGHT_ENTRIES_TABLE_NAME");
const syncChangesTableName = requiredEnv("SYNC_CHANGES_TABLE_NAME");

export async function lookupBarcode(userId: string, barcode: string): Promise<BarcodeLookupResponse> {
  const alias = await client.send(
    new GetCommand({
      TableName: barcodeAliasesTableName,
      Key: {
        ownerUserId: userId,
        barcode,
      },
    }),
  );

  if (!alias.Item) {
    return { found: false };
  }

  const product = await getProduct(userId, alias.Item.productId);
  return product ? { found: true, product } : { found: false };
}

export async function createFoodProduct(
  userId: string,
  request: CreateFoodProductRequest,
): Promise<FoodProduct> {
  const now = new Date().toISOString();
  const product: FoodProduct = {
    productId: randomUUID(),
    ownerUserId: userId,
    visibility: "private",
    ...request,
    createdAt: now,
    updatedAt: now,
  };

  await client.send(
    new PutCommand({
      TableName: productsTableName,
      Item: product,
      ConditionExpression: "attribute_not_exists(ownerUserId) AND attribute_not_exists(productId)",
    }),
  );

  if (request.barcode) {
    await client.send(
      new PutCommand({
        TableName: barcodeAliasesTableName,
        Item: {
          ownerUserId: userId,
          barcode: request.barcode,
          productId: product.productId,
          visibility: "private",
          createdAt: now,
        },
      }),
    );
  }

  return product;
}

export async function listFoodProducts(userId: string): Promise<FoodProduct[]> {
  const result = await client.send(
    new QueryCommand({
      TableName: productsTableName,
      KeyConditionExpression: "ownerUserId = :ownerUserId",
      ExpressionAttributeValues: {
        ":ownerUserId": userId,
      },
      Limit: 100,
      ScanIndexForward: false,
    }),
  );

  return (result.Items ?? []) as FoodProduct[];
}

export async function createDiaryEntry(
  userId: string,
  request: CreateDiaryEntryRequest,
): Promise<DiaryEntry> {
  const product = await getProduct(userId, request.productId);
  if (!product) {
    throw new Error("Product not found");
  }

  const now = new Date().toISOString();
  const entry: DiaryEntry = {
    entryId: randomUUID(),
    ownerUserId: userId,
    productId: request.productId,
    loggedAt: request.loggedAt,
    meal: request.meal,
    servingMultiplier: request.servingMultiplier,
    productSnapshot: product,
    createdAt: now,
    updatedAt: now,
  };

  await client.send(
    new PutCommand({
      TableName: diaryEntriesTableName,
      Item: entry,
      ConditionExpression: "attribute_not_exists(ownerUserId) AND attribute_not_exists(entryId)",
    }),
  );

  return entry;
}

export async function createWeightEntry(
  userId: string,
  request: CreateWeightEntryRequest,
): Promise<WeightEntry> {
  const now = new Date().toISOString();
  const entry: WeightEntry = {
    entryId: randomUUID(),
    ownerUserId: userId,
    loggedAt: request.loggedAt,
    weightKg: request.weightKg,
    source: request.source ?? "manual",
    createdAt: now,
    updatedAt: now,
  };

  await client.send(
    new PutCommand({
      TableName: weightEntriesTableName,
      Item: entry,
      ConditionExpression: "attribute_not_exists(ownerUserId) AND attribute_not_exists(entryId)",
    }),
  );

  return entry;
}

export async function listWeightEntries(userId: string): Promise<WeightEntry[]> {
  const result = await client.send(
    new QueryCommand({
      TableName: weightEntriesTableName,
      KeyConditionExpression: "ownerUserId = :ownerUserId",
      ExpressionAttributeValues: {
        ":ownerUserId": userId,
      },
      Limit: 365,
      ScanIndexForward: false,
    }),
  );

  return (result.Items ?? []) as WeightEntry[];
}

export async function pushSyncChanges(userId: string, request: SyncPushRequest): Promise<SyncPushResponse> {
  const acceptedChangeIds: string[] = [];

  for (const change of request.changes) {
    await client.send(
      new PutCommand({
        TableName: syncChangesTableName,
        Item: {
          ownerUserId: userId,
          changeKey: toSyncSortKey(change.changedAt, change.changeId),
          changeId: change.changeId,
          entityType: change.entityType,
          recordId: change.recordId,
          operation: change.operation,
          changedAt: change.changedAt,
          deviceId: change.deviceId,
          baseVersion: change.baseVersion,
          payload: change.payload,
        },
      }),
    );
    acceptedChangeIds.push(change.changeId);
  }

  return {
    cursor: {
      deviceId: request.deviceId,
      lastPulledAt: request.changes.at(-1)?.changedAt ?? request.cursor?.lastPulledAt,
      lastAcknowledgedChangeId: acceptedChangeIds.at(-1) ?? request.cursor?.lastAcknowledgedChangeId,
    },
    acceptedChangeIds,
    rejectedChanges: [],
  };
}

export async function pullSyncChanges(userId: string, request: SyncPullRequest): Promise<SyncPullResponse> {
  const changedAfter = request.cursor?.lastPulledAt ?? "";
  const result = await client.send(
    new QueryCommand({
      TableName: syncChangesTableName,
      KeyConditionExpression: "ownerUserId = :ownerUserId AND changeKey > :changeKey",
      ExpressionAttributeValues: {
        ":ownerUserId": userId,
        ":changeKey": toSyncSortKey(changedAfter, request.cursor?.lastAcknowledgedChangeId ?? ""),
      },
      Limit: 200,
      ScanIndexForward: true,
    }),
  );

  const changes = (result.Items ?? []).map((item) => ({
    changeId: item.changeId,
    entityType: item.entityType,
    recordId: item.recordId,
    operation: item.operation,
    changedAt: item.changedAt,
    deviceId: item.deviceId,
    baseVersion: item.baseVersion,
    payload: item.payload,
  })) as SyncChange[];

  const lastChange = changes.at(-1);
  return {
    cursor: {
      deviceId: request.deviceId,
      lastPulledAt: lastChange?.changedAt ?? request.cursor?.lastPulledAt,
      lastAcknowledgedChangeId: lastChange?.changeId ?? request.cursor?.lastAcknowledgedChangeId,
    },
    changes,
  };
}

async function getProduct(userId: string, productId: string): Promise<FoodProduct | undefined> {
  const result = await client.send(
    new GetCommand({
      TableName: productsTableName,
      Key: {
        ownerUserId: userId,
        productId,
      },
    }),
  );

  return result.Item as FoodProduct | undefined;
}

function requiredEnv(name: string): string {
  const value = process.env[name];
  if (!value) {
    throw new Error(`Missing environment variable ${name}`);
  }
  return value;
}

function toSyncSortKey(changedAt: string, changeId: string): string {
  return `${changedAt}#${changeId}`;
}
