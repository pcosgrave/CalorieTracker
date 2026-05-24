import { emptySchema, createWeightEntrySchema } from "../lib/schemas.js";
import { createWeightEntry, listWeightEntries } from "../lib/store.js";
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
