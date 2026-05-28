import { z } from "zod";
import { getUserId, json, publicRoute, route } from "../lib/http.js";
import { createFoodProductSchema, emptySchema, publishCommunityFoodSchema, updateFoodProductSchema } from "../lib/schemas.js";
import {
  createFoodProduct,
  deleteFoodProduct,
  listFoodProducts,
  lookupBarcode,
  lookupCommunityBarcode,
  publishCommunityFood,
  searchCommunityFoodProducts,
  searchFoodProducts,
  updateFoodProduct,
} from "../lib/store.js";

export const create = route(createFoodProductSchema, async ({ userId, body }) => {
  const product = await createFoodProduct(userId, body);
  return json(201, { product });
});

export const update = route(updateFoodProductSchema, async ({ event, userId, body }) => {
  const productId = event.pathParameters?.productId;
  if (!productId) {
    return json(400, { message: "Product ID is required" });
  }

  try {
    const product = await updateFoodProduct(userId, productId, body);
    return json(200, { product });
  } catch (error) {
    if (error instanceof Error && error.message === "Product not found") {
      return json(404, { message: "Product not found" });
    }
    throw error;
  }
});

export const remove = route(z.object({}), async ({ event, userId }) => {
  const productId = event.pathParameters?.productId;
  if (!productId) {
    return json(400, { message: "Product ID is required" });
  }

  await deleteFoodProduct(userId, productId);
  return json(204, {});
});

export const list = route(emptySchema, async ({ userId }) => {
  const products = await listFoodProducts(userId);
  return json(200, { products });
});

export const lookup = route(z.object({}), async ({ event, userId }) => {
  const barcode = event.pathParameters?.barcode;
  if (!barcode) {
    return json(400, { message: "Barcode is required" });
  }

  const result = await lookupBarcode(userId, barcode);
  return json(200, result);
});

export const search = route(emptySchema, async ({ event, userId }) => {
  const query = event.queryStringParameters?.query ?? "";
  const result = await searchFoodProducts(userId, query);
  return json(200, result);
});

export const lookupCommunity = route(z.object({}), async ({ event }) => {
  const barcode = event.pathParameters?.barcode;
  if (!barcode) {
    return json(400, { message: "Barcode is required" });
  }

  const result = await lookupCommunityBarcode(barcode);
  return json(200, result);
});

export const searchCommunity = route(emptySchema, async ({ event }) => {
  const query = event.queryStringParameters?.query ?? "";
  const result = await searchCommunityFoodProducts(query);
  return json(200, result);
});

export const publishCommunity = publicRoute(publishCommunityFoodSchema, async ({ event, body }) => {
  const userId = (() => {
    try {
      return getUserId(event);
    } catch {
      return undefined;
    }
  })();
  const result = await publishCommunityFood(userId, body);
  return json(result.existed ? 200 : 201, result);
});
