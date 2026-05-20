import { json, route } from "../lib/http.js";
import { createDiaryEntrySchema } from "../lib/schemas.js";
import { createDiaryEntry } from "../lib/store.js";

export const create = route(createDiaryEntrySchema, async ({ userId, body }) => {
  try {
    const entry = await createDiaryEntry(userId, body);
    return json(201, { entry });
  } catch (error) {
    if (error instanceof Error && error.message === "Product not found") {
      return json(404, { message: "Product not found" });
    }
    throw error;
  }
});
