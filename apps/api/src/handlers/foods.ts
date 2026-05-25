import { z } from "zod";
import { json, route } from "../lib/http.js";
import { createFoodProductSchema, emptySchema, publishCommunityFoodSchema } from "../lib/schemas.js";
import {
  createFoodProduct,
  listFoodProducts,
  lookupBarcode,
  lookupCommunityBarcode,
  publishCommunityFood,
  searchCommunityFoodProducts,
  searchFoodProducts,
} from "../lib/store.js";

export const create = route(createFoodProductSchema, async ({ userId, body }) => {
  const product = await createFoodProduct(userId, body);
  return json(201, { product });
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

export const publishCommunity = route(publishCommunityFoodSchema, async ({ userId, body }) => {
  const result = await publishCommunityFood(userId, body);
  return json(result.existed ? 200 : 201, result);
});
