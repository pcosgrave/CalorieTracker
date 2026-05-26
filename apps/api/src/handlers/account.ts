import { deleteAccount } from "../lib/store.js";
import { json, route } from "../lib/http.js";
import { emptySchema } from "../lib/schemas.js";

export const remove = route(emptySchema, async ({ userId }) => {
  await deleteAccount(userId);
  return json(200, { deleted: true });
});
