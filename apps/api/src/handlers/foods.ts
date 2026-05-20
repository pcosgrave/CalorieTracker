import { z } from "zod";
import { json, route } from "../lib/http.js";
import { createFoodProductSchema, emptySchema } from "../lib/schemas.js";
import { createFoodProduct, listFoodProducts, lookupBarcode } from "../lib/store.js";

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
