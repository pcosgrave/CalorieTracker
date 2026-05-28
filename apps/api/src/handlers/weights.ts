import { emptySchema, createWeightEntrySchema, updateWeightEntrySchema } from "../lib/schemas.js";
import { createWeightEntry, deleteWeightEntry, listWeightEntries, updateWeightEntry } from "../lib/store.js";
import { json, route } from "../lib/http.js";

export const create = route(createWeightEntrySchema, async ({ userId, body }) => {
  const entry = await createWeightEntry(userId, {
    loggedAt: body.loggedAt,
    weightKg: body.weightKg,
    ...(body.source ? { source: body.source } : {}),
  });
  return json(201, { entry });
});

export const list = route(emptySchema, async ({ userId }) => {
  const entries = await listWeightEntries(userId);
  return json(200, { entries });
});

export const update = route(updateWeightEntrySchema, async ({ event, userId, body }) => {
  const entryId = event.pathParameters?.entryId;
  if (!entryId) {
    return json(400, { message: "Entry ID is required" });
  }

  try {
    const entry = await updateWeightEntry(userId, entryId, {
      loggedAt: body.loggedAt,
      weightKg: body.weightKg,
      ...(body.source ? { source: body.source } : {}),
    });
    return json(200, { entry });
  } catch (error) {
    if (error instanceof Error && error.message === "Weight entry not found") {
      return json(404, { message: "Weight entry not found" });
    }
    throw error;
  }
});

export const remove = route(emptySchema, async ({ event, userId }) => {
  const entryId = event.pathParameters?.entryId;
  if (!entryId) {
    return json(400, { message: "Entry ID is required" });
  }

  await deleteWeightEntry(userId, entryId);
  return json(204, {});
});
