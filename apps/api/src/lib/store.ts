import { CognitoIdentityProviderClient, AdminDeleteUserCommand } from "@aws-sdk/client-cognito-identity-provider";
import { DynamoDBClient } from "@aws-sdk/client-dynamodb";
import { DeleteCommand, DynamoDBDocumentClient, GetCommand, PutCommand, QueryCommand } from "@aws-sdk/lib-dynamodb";
import type {
  BarcodeAlias,
  BarcodeAliasRecord,
  BarcodeLookupResponse,
  CreateDiaryEntryRequest,
  CreateFoodProductRequest,
  CreateWeightEntryRequest,
  DiaryEntry,
  DiaryEntryRecord,
  FoodProduct,
  FoodProductRecord,
  SyncChange,
  SyncCursor,
  SyncPullRequest,
  SyncPullResponse,
  SyncPushRequest,
  SyncPushResponse,
  SyncChangeRejection,
  SyncMetadata,
  WeightEntry,
  WeightEntryRecord,
} from "@calorie-tracker/shared";
import { randomUUID } from "node:crypto";

const client = DynamoDBDocumentClient.from(new DynamoDBClient({}));

const productsTableName = requiredEnv("PRODUCTS_TABLE_NAME");
const barcodeAliasesTableName = requiredEnv("BARCODE_ALIASES_TABLE_NAME");
const diaryEntriesTableName = requiredEnv("DIARY_ENTRIES_TABLE_NAME");
const weightEntriesTableName = requiredEnv("WEIGHT_ENTRIES_TABLE_NAME");
const syncChangesTableName = requiredEnv("SYNC_CHANGES_TABLE_NAME");
const userPoolId = requiredEnv("USER_POOL_ID");
const communityOwnerUserId = "__community__";
const cognito = new CognitoIdentityProviderClient({});

interface FoodSearchResponse {
  products: FoodProduct[];
}

interface PublishCommunityFoodRequest extends CreateFoodProductRequest {
  productId?: string | undefined;
}

interface PublishCommunityFoodResponse {
  product: FoodProduct;
  existed: boolean;
}

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

export async function lookupCommunityBarcode(barcode: string): Promise<BarcodeLookupResponse> {
  const alias = await client.send(
    new GetCommand({
      TableName: barcodeAliasesTableName,
      Key: {
        ownerUserId: communityOwnerUserId,
        barcode,
      },
    }),
  );

  if (!alias.Item) {
    return { found: false };
  }

  const product = await getProduct(communityOwnerUserId, alias.Item.productId);
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

export async function updateFoodProduct(
  userId: string,
  productId: string,
  request: CreateFoodProductRequest,
): Promise<FoodProduct> {
  const existing = await getProduct(userId, productId);
  if (!existing) {
    throw new Error("Product not found");
  }

  const updatedAt = new Date().toISOString();
  const product: FoodProduct = {
    ...existing,
    ...request,
    productId,
    ownerUserId: userId,
    visibility: existing.visibility,
    createdAt: existing.createdAt,
    updatedAt,
  };

  await client.send(
    new PutCommand({
      TableName: productsTableName,
      Item: product,
    }),
  );

  if (existing.barcode && existing.barcode !== request.barcode) {
    await deleteBarcodeAlias(userId, existing.barcode);
  }
  if (request.barcode) {
    await upsertBarcodeAlias({
      barcode: request.barcode,
      ownerUserId: userId,
      productId,
      visibility: existing.visibility,
      createdAt: existing.createdAt,
    });
  }

  return product;
}

export async function deleteFoodProduct(userId: string, productId: string): Promise<void> {
  const existing = await getProduct(userId, productId);
  if (!existing) {
    return;
  }

  await client.send(
    new DeleteCommand({
      TableName: productsTableName,
      Key: {
        ownerUserId: userId,
        productId,
      },
    }),
  );

  if (existing.barcode) {
    await deleteBarcodeAlias(userId, existing.barcode);
  }
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

export async function searchFoodProducts(userId: string, query: string): Promise<FoodSearchResponse> {
  const normalized = query.trim().toLowerCase();
  if (!normalized) {
    return { products: [] };
  }

  const products = await listFoodProducts(userId);
  return {
    products: products.filter((product) => matchesProductQuery(product, normalized)).slice(0, 25),
  };
}

export async function searchCommunityFoodProducts(query: string): Promise<FoodSearchResponse> {
  const normalized = query.trim().toLowerCase();
  if (!normalized) {
    return { products: [] };
  }

  const products = await listFoodProducts(communityOwnerUserId);
  return {
    products: products.filter((product) => matchesProductQuery(product, normalized)).slice(0, 25),
  };
}

export async function publishCommunityFood(
  userId: string | undefined,
  request: PublishCommunityFoodRequest,
): Promise<PublishCommunityFoodResponse> {
  const now = new Date().toISOString();
  const normalizedName = normalizeCommunityKeyPart(request.name);
  const normalizedBrand = normalizeCommunityKeyPart(request.brand ?? "");
  const productId = `community:${normalizedName}:${normalizedBrand || "unbranded"}`;
  const existing = await getProduct(communityOwnerUserId, productId);

  if (existing) {
    return { product: existing, existed: true };
  }

  const product: FoodProduct = {
    productId,
    ownerUserId: communityOwnerUserId,
    visibility: "shared",
    barcode: request.barcode,
    name: request.name,
    brand: request.brand,
    serving: request.serving,
    nutrients: request.nutrients,
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
    try {
      await client.send(
        new PutCommand({
          TableName: barcodeAliasesTableName,
          Item: {
            ownerUserId: communityOwnerUserId,
            barcode: request.barcode,
            productId,
            visibility: "shared",
            createdAt: now,
            uploadedByUserId: userId,
          },
          ConditionExpression: "attribute_not_exists(ownerUserId) AND attribute_not_exists(barcode)",
        }),
      );
    } catch {
      // First upload wins for the barcode alias as well.
    }
  }

  return { product, existed: false };
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
    loggedAmount: request.loggedAmount,
    loggedUnit: request.loggedUnit,
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

export async function updateDiaryEntry(
  userId: string,
  entryId: string,
  request: CreateDiaryEntryRequest,
): Promise<DiaryEntry> {
  const existing = await getDiaryEntry(userId, entryId);
  if (!existing) {
    throw new Error("Diary entry not found");
  }

  const product = await getProduct(userId, request.productId);
  if (!product) {
    throw new Error("Product not found");
  }

  const updatedAt = new Date().toISOString();
  const entry: DiaryEntry = {
    ...existing,
    entryId,
    ownerUserId: userId,
    productId: request.productId,
    loggedAt: request.loggedAt,
    meal: request.meal,
    servingMultiplier: request.servingMultiplier,
    loggedAmount: request.loggedAmount,
    loggedUnit: request.loggedUnit,
    productSnapshot: product,
    createdAt: existing.createdAt,
    updatedAt,
  };

  await client.send(
    new PutCommand({
      TableName: diaryEntriesTableName,
      Item: entry,
    }),
  );

  return entry;
}

export async function deleteDiaryEntry(userId: string, entryId: string): Promise<void> {
  await client.send(
    new DeleteCommand({
      TableName: diaryEntriesTableName,
      Key: {
        ownerUserId: userId,
        entryId,
      },
    }),
  );
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

export async function updateWeightEntry(
  userId: string,
  entryId: string,
  request: CreateWeightEntryRequest,
): Promise<WeightEntry> {
  const existing = await getWeightEntry(userId, entryId);
  if (!existing) {
    throw new Error("Weight entry not found");
  }

  const updatedAt = new Date().toISOString();
  const entry: WeightEntry = {
    ...existing,
    entryId,
    ownerUserId: userId,
    loggedAt: request.loggedAt,
    weightKg: request.weightKg,
    source: request.source ?? existing.source,
    createdAt: existing.createdAt,
    updatedAt,
  };

  await client.send(
    new PutCommand({
      TableName: weightEntriesTableName,
      Item: entry,
    }),
  );

  return entry;
}

export async function deleteWeightEntry(userId: string, entryId: string): Promise<void> {
  await client.send(
    new DeleteCommand({
      TableName: weightEntriesTableName,
      Key: {
        ownerUserId: userId,
        entryId,
      },
    }),
  );
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
  const rejectedChanges: SyncChangeRejection[] = [];

  for (const change of request.changes) {
    const rejection = await rejectIfStale(userId, change);
    if (rejection) {
      rejectedChanges.push({
        changeId: change.changeId,
        code: "conflict",
        message: rejection,
      });
      continue;
    }

    await materializeSyncChange(userId, change);
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
    rejectedChanges,
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

export async function deleteAccount(userId: string): Promise<void> {
  await deleteOwnedItems(productsTableName, userId, "productId");
  await deleteOwnedItems(barcodeAliasesTableName, userId, "barcode");
  await deleteOwnedItems(diaryEntriesTableName, userId, "entryId");
  await deleteOwnedItems(weightEntriesTableName, userId, "entryId");
  await deleteOwnedItems(syncChangesTableName, userId, "changeKey");

  await cognito.send(
    new AdminDeleteUserCommand({
      UserPoolId: userPoolId,
      Username: userId,
    }),
  );
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

async function getDiaryEntry(userId: string, entryId: string): Promise<DiaryEntry | undefined> {
  const result = await client.send(
    new GetCommand({
      TableName: diaryEntriesTableName,
      Key: {
        ownerUserId: userId,
        entryId,
      },
    }),
  );

  return result.Item as DiaryEntry | undefined;
}

async function getWeightEntry(userId: string, entryId: string): Promise<WeightEntry | undefined> {
  const result = await client.send(
    new GetCommand({
      TableName: weightEntriesTableName,
      Key: {
        ownerUserId: userId,
        entryId,
      },
    }),
  );

  return result.Item as WeightEntry | undefined;
}

async function upsertBarcodeAlias(alias: BarcodeAlias): Promise<void> {
  await client.send(
    new PutCommand({
      TableName: barcodeAliasesTableName,
      Item: alias,
    }),
  );
}

async function deleteBarcodeAlias(ownerUserId: string, barcode: string): Promise<void> {
  await client.send(
    new DeleteCommand({
      TableName: barcodeAliasesTableName,
      Key: {
        ownerUserId,
        barcode,
      },
    }),
  );
}

async function deleteOwnedItems(
  tableName: string,
  ownerUserId: string,
  rangeKeyName: string,
): Promise<void> {
  let exclusiveStartKey: Record<string, unknown> | undefined;

  do {
    const result = await client.send(
      new QueryCommand({
        TableName: tableName,
        KeyConditionExpression: "ownerUserId = :ownerUserId",
        ExpressionAttributeValues: {
          ":ownerUserId": ownerUserId,
        },
        ExclusiveStartKey: exclusiveStartKey,
      }),
    );

    for (const item of result.Items ?? []) {
      const rangeKeyValue = item[rangeKeyName];
      if (typeof rangeKeyValue !== "string" || rangeKeyValue.length == 0) {
        continue;
      }

      await client.send(
        new DeleteCommand({
          TableName: tableName,
          Key: {
            ownerUserId,
            [rangeKeyName]: rangeKeyValue,
          },
        }),
      );
    }

    exclusiveStartKey = result.LastEvaluatedKey;
  } while (exclusiveStartKey);
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

function matchesProductQuery(product: FoodProduct, normalizedQuery: string): boolean {
  const name = product.name.toLowerCase();
  const brand = (product.brand ?? "").toLowerCase();
  return (
    name.includes(normalizedQuery) ||
    brand.includes(normalizedQuery) ||
    `${name} ${brand}`.includes(normalizedQuery)
  );
}

async function materializeSyncChange(userId: string, change: SyncChange): Promise<void> {
  if (change.operation === "delete") {
    await deleteMaterializedRecord(userId, change);
    return;
  }

  await upsertMaterializedRecord(userId, change);
}

async function rejectIfStale(userId: string, change: SyncChange): Promise<string | null> {
  const incomingUpdatedAt = getIncomingUpdatedAt(change);
  if (!incomingUpdatedAt) {
    return null;
  }

  const currentUpdatedAt = await getCurrentUpdatedAt(userId, change);
  if (!currentUpdatedAt) {
    return null;
  }

  if (Date.parse(incomingUpdatedAt) < Date.parse(currentUpdatedAt)) {
    return `Incoming ${change.entityType} change is older than current server state`;
  }

  return null;
}

async function upsertMaterializedRecord(userId: string, change: SyncChange): Promise<void> {
  switch (change.entityType) {
    case "food_product": {
      const payload = change.payload as FoodProductRecord | undefined;
      if (!payload) return;
      const product = {
        ...payload.product,
        ownerUserId: userId,
      };
      await client.send(
        new PutCommand({
          TableName: productsTableName,
          Item: product,
        }),
      );

      if (product.barcode) {
        const alias: BarcodeAlias = {
          barcode: product.barcode,
          ownerUserId: userId,
          productId: product.productId,
          visibility: "private",
          createdAt: product.createdAt,
        };
        await client.send(
          new PutCommand({
            TableName: barcodeAliasesTableName,
            Item: alias,
          }),
        );
      }
      return;
    }
    case "barcode_alias": {
      const payload = change.payload as BarcodeAliasRecord | undefined;
      if (!payload) return;
      await client.send(
        new PutCommand({
          TableName: barcodeAliasesTableName,
          Item: {
            ...payload.alias,
            ownerUserId: userId,
          },
        }),
      );
      return;
    }
    case "diary_entry": {
      const payload = change.payload as DiaryEntryRecord | undefined;
      if (!payload) return;
      await client.send(
        new PutCommand({
          TableName: diaryEntriesTableName,
          Item: {
            ...payload.entry,
            ownerUserId: userId,
            productSnapshot: {
              ...payload.entry.productSnapshot,
              ownerUserId: userId,
            },
          },
        }),
      );
      return;
    }
    case "weight_entry": {
      const payload = change.payload as WeightEntryRecord | undefined;
      if (!payload) return;
      await client.send(
        new PutCommand({
          TableName: weightEntriesTableName,
          Item: {
            ...payload.entry,
            ownerUserId: userId,
          },
        }),
      );
      return;
    }
  }
}

async function deleteMaterializedRecord(userId: string, change: SyncChange): Promise<void> {
  switch (change.entityType) {
    case "food_product": {
      const payload = change.payload as FoodProductRecord | undefined;
      await client.send(
        new DeleteCommand({
          TableName: productsTableName,
          Key: {
            ownerUserId: userId,
            productId: change.recordId,
          },
        }),
      );
      if (payload?.product.barcode) {
        await client.send(
          new DeleteCommand({
            TableName: barcodeAliasesTableName,
            Key: {
              ownerUserId: userId,
              barcode: payload.product.barcode,
            },
          }),
        );
      }
      return;
    }
    case "barcode_alias": {
      const payload = change.payload as BarcodeAliasRecord | undefined;
      const barcode = payload?.alias.barcode ?? parseBarcodeRecordId(change.recordId);
      if (!barcode) return;
      await client.send(
        new DeleteCommand({
          TableName: barcodeAliasesTableName,
          Key: {
            ownerUserId: userId,
            barcode,
          },
        }),
      );
      return;
    }
    case "diary_entry": {
      await client.send(
        new DeleteCommand({
          TableName: diaryEntriesTableName,
          Key: {
            ownerUserId: userId,
            entryId: change.recordId,
          },
        }),
      );
      return;
    }
    case "weight_entry": {
      await client.send(
        new DeleteCommand({
          TableName: weightEntriesTableName,
          Key: {
            ownerUserId: userId,
            entryId: change.recordId,
          },
        }),
      );
      return;
    }
  }
}

async function getCurrentUpdatedAt(userId: string, change: SyncChange): Promise<string | undefined> {
  switch (change.entityType) {
    case "food_product":
      return (await getProduct(userId, change.recordId))?.updatedAt;
    case "barcode_alias": {
      const barcode = getBarcodeForChange(change);
      if (!barcode) return undefined;
      return (await getBarcodeAlias(userId, barcode))?.createdAt;
    }
    case "diary_entry":
      return (await getDiaryEntry(userId, change.recordId))?.updatedAt;
    case "weight_entry":
      return (await getWeightEntry(userId, change.recordId))?.updatedAt;
  }
}

function getIncomingUpdatedAt(change: SyncChange): string | undefined {
  switch (change.entityType) {
    case "food_product":
      return (change.payload as FoodProductRecord | undefined)?.product.updatedAt
        ?? getSyncUpdatedAt(change.payload as { sync?: SyncMetadata } | undefined)
        ?? change.changedAt;
    case "barcode_alias":
      return getSyncUpdatedAt(change.payload as { sync?: SyncMetadata } | undefined) ?? change.changedAt;
    case "diary_entry":
      return (change.payload as DiaryEntryRecord | undefined)?.entry.updatedAt
        ?? getSyncUpdatedAt(change.payload as { sync?: SyncMetadata } | undefined)
        ?? change.changedAt;
    case "weight_entry":
      return (change.payload as WeightEntryRecord | undefined)?.entry.updatedAt
        ?? getSyncUpdatedAt(change.payload as { sync?: SyncMetadata } | undefined)
        ?? change.changedAt;
  }
}

function getSyncUpdatedAt(payload: { sync?: SyncMetadata } | undefined): string | undefined {
  return payload?.sync?.updatedAt;
}

function getBarcodeForChange(change: SyncChange): string | undefined {
  const payload = change.payload as BarcodeAliasRecord | FoodProductRecord | undefined;
  if (change.entityType === "barcode_alias") {
    return (payload as BarcodeAliasRecord | undefined)?.alias.barcode ?? parseBarcodeRecordId(change.recordId);
  }
  if (change.entityType === "food_product") {
    return (payload as FoodProductRecord | undefined)?.product.barcode;
  }
  return undefined;
}

function parseBarcodeRecordId(recordId: string): string | undefined {
  const separatorIndex = recordId.indexOf(":");
  if (separatorIndex < 0 || separatorIndex === recordId.length - 1) {
    return undefined;
  }
  return recordId.slice(separatorIndex + 1);
}

async function getBarcodeAlias(userId: string, barcode: string): Promise<BarcodeAlias | undefined> {
  const result = await client.send(
    new GetCommand({
      TableName: barcodeAliasesTableName,
      Key: {
        ownerUserId: userId,
        barcode,
      },
    }),
  );

  return result.Item as BarcodeAlias | undefined;
}

function normalizeCommunityKeyPart(value: string): string {
  return value
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "")
    .slice(0, 80);
}
