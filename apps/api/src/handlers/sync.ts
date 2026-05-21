import { json, route } from "../lib/http.js";
import { syncPullSchema, syncPushSchema } from "../lib/schemas.js";
import { pullSyncChanges, pushSyncChanges } from "../lib/store.js";

export const push = route(syncPushSchema, async ({ userId, body }) => {
  const response = await pushSyncChanges(userId, body);
  return json(200, response);
});

export const pull = route(syncPullSchema, async ({ userId, body }) => {
  const response = await pullSyncChanges(userId, body);
  return json(200, response);
});
