import { json, route } from "../lib/http.js";
import { createDiaryEntrySchema, emptySchema, updateDiaryEntrySchema } from "../lib/schemas.js";
import { createDiaryEntry, deleteDiaryEntry, updateDiaryEntry } from "../lib/store.js";

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

export const update = route(updateDiaryEntrySchema, async ({ event, userId, body }) => {
  const entryId = event.pathParameters?.entryId;
  if (!entryId) {
    return json(400, { message: "Entry ID is required" });
  }

  try {
    const entry = await updateDiaryEntry(userId, entryId, body);
    return json(200, { entry });
  } catch (error) {
    if (error instanceof Error && error.message === "Diary entry not found") {
      return json(404, { message: "Diary entry not found" });
    }
    if (error instanceof Error && error.message === "Product not found") {
      return json(404, { message: "Product not found" });
    }
    throw error;
  }
});

export const remove = route(emptySchema, async ({ event, userId }) => {
  const entryId = event.pathParameters?.entryId;
  if (!entryId) {
    return json(400, { message: "Entry ID is required" });
  }

  await deleteDiaryEntry(userId, entryId);
  return json(204, {});
});
