import { DynamoDBClient } from "@aws-sdk/client-dynamodb";
import { DynamoDBDocumentClient, GetCommand, PutCommand, QueryCommand } from "@aws-sdk/lib-dynamodb";
import type {
  BarcodeLookupResponse,
  CreateDiaryEntryRequest,
  CreateFoodProductRequest,
  DiaryEntry,
  FoodProduct,
} from "@calorie-tracker/shared";
import { randomUUID } from "node:crypto";

const client = DynamoDBDocumentClient.from(new DynamoDBClient({}));

const productsTableName = requiredEnv("PRODUCTS_TABLE_NAME");
const barcodeAliasesTableName = requiredEnv("BARCODE_ALIASES_TABLE_NAME");
const diaryEntriesTableName = requiredEnv("DIARY_ENTRIES_TABLE_NAME");

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
