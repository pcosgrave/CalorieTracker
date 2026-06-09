import { z } from "zod";

export const nutrientsSchema = z.object({
  calories: z.number().nonnegative(),
  proteinGrams: z.number().nonnegative(),
  carbohydrateGrams: z.number().nonnegative(),
  fatGrams: z.number().nonnegative(),
  fiberGrams: z.number().nonnegative().optional(),
  sugarGrams: z.number().nonnegative().optional(),
  sodiumMilligrams: z.number().nonnegative().optional(),
});

export const servingSchema = z.object({
  label: z.string().min(1),
  quantity: z.number().positive(),
  unit: z.string().min(1),
  grams: z.number().positive().optional(),
});

export const createFoodProductSchema = z.object({
  barcode: z.string().min(4).max(32).optional(),
  name: z.string().min(1),
  brand: z.string().min(1).optional(),
  serving: servingSchema,
  nutrients: nutrientsSchema,
});

export const updateFoodProductSchema = createFoodProductSchema;

export const publishCommunityFoodSchema = z.object({
  productId: z.string().min(1).optional(),
  barcode: z.string().min(4).max(32).optional(),
  name: z.string().min(1),
  brand: z.string().min(1).optional(),
  serving: servingSchema,
  nutrients: nutrientsSchema,
});

export const createDiaryEntrySchema = z.object({
  productId: z.string().min(1),
  loggedAt: z.string().datetime(),
  meal: z.enum(["breakfast", "lunch", "dinner", "snack"]),
  servingMultiplier: z.number().positive(),
  loggedAmount: z.number().positive().optional(),
  loggedUnit: z.string().min(1).optional(),
});

export const updateDiaryEntrySchema = createDiaryEntrySchema;

export const createWeightEntrySchema = z.object({
  loggedAt: z.string().datetime(),
  weightKg: z.number().positive(),
  source: z.enum(["manual", "health_connect", "import"]).optional(),
});

export const updateWeightEntrySchema = createWeightEntrySchema;

export const emptySchema = z.object({});

export const aiFoodLogParseSchema = z.object({
  transcript: z.string().min(1),
  date: z.string().date(),
  fallbackMeal: z.enum(["Breakfast", "Lunch", "Dinner", "Snack"]),
});

const syncMetadataSchema = z.object({
  recordId: z.string().min(1),
  version: z.number().int().positive(),
  updatedAt: z.string().datetime(),
  deletedAt: z.string().datetime().optional(),
  originDeviceId: z.string().min(1),
  lastSyncedAt: z.string().datetime().optional(),
  syncStatus: z.enum(["local_only", "pending_push", "synced", "sync_error"]),
});

const foodProductRecordSchema = z.object({
  product: z.object({
    productId: z.string().min(1),
    ownerUserId: z.string().min(1),
    visibility: z.enum(["private", "shared", "global"]),
    barcode: z.string().optional(),
    name: z.string().min(1),
    brand: z.string().optional(),
    serving: servingSchema,
    nutrients: nutrientsSchema,
    frequency: z.number().int().nonnegative().optional(),
    breakfastFrequency: z.number().int().nonnegative().optional(),
    lunchFrequency: z.number().int().nonnegative().optional(),
    dinnerFrequency: z.number().int().nonnegative().optional(),
    snackFrequency: z.number().int().nonnegative().optional(),
    lastUsedAt: z.string().datetime().optional(),
    createdAt: z.string().datetime(),
    updatedAt: z.string().datetime(),
  }).passthrough(),
  sync: syncMetadataSchema,
});

const barcodeAliasRecordSchema = z.object({
  alias: z.object({
    barcode: z.string().min(1),
    ownerUserId: z.string().min(1),
    productId: z.string().min(1),
    visibility: z.enum(["private", "shared", "global"]),
    createdAt: z.string().datetime(),
  }),
  sync: syncMetadataSchema,
});

const diaryEntryRecordSchema = z.object({
  entry: z.object({
    entryId: z.string().min(1),
    ownerUserId: z.string().min(1),
    productId: z.string().min(1),
    loggedAt: z.string().datetime(),
    meal: z.enum(["breakfast", "lunch", "dinner", "snack"]),
    servingMultiplier: z.number().positive(),
    loggedAmount: z.number().positive().optional(),
    loggedUnit: z.string().min(1).optional(),
    productSnapshot: foodProductRecordSchema.shape.product,
    createdAt: z.string().datetime(),
    updatedAt: z.string().datetime(),
  }).passthrough(),
  sync: syncMetadataSchema,
});

const weightEntryRecordSchema = z.object({
  entry: z.object({
    entryId: z.string().min(1),
    ownerUserId: z.string().min(1),
    loggedAt: z.string().datetime(),
    weightKg: z.number().positive(),
    source: z.enum(["manual", "health_connect", "import"]),
    createdAt: z.string().datetime(),
    updatedAt: z.string().datetime(),
  }).passthrough(),
  sync: syncMetadataSchema,
});

const syncChangeSchemaBase = z.object({
  changeId: z.string().min(1),
  entityType: z.enum(["food_product", "barcode_alias", "diary_entry", "weight_entry"]),
  recordId: z.string().min(1),
  operation: z.enum(["upsert", "delete"]),
  changedAt: z.string().datetime(),
  deviceId: z.string().min(1),
  baseVersion: z.number().int().positive().optional(),
});

export const syncChangeSchema = z.discriminatedUnion("entityType", [
  syncChangeSchemaBase.extend({
    entityType: z.literal("food_product"),
    payload: foodProductRecordSchema.optional(),
  }),
  syncChangeSchemaBase.extend({
    entityType: z.literal("barcode_alias"),
    payload: barcodeAliasRecordSchema.optional(),
  }),
  syncChangeSchemaBase.extend({
    entityType: z.literal("diary_entry"),
    payload: diaryEntryRecordSchema.optional(),
  }),
  syncChangeSchemaBase.extend({
    entityType: z.literal("weight_entry"),
    payload: weightEntryRecordSchema.optional(),
  }),
]);

export const syncPushSchema = z.object({
  deviceId: z.string().min(1),
  cursor: z
    .object({
      deviceId: z.string().min(1),
      lastPulledAt: z.string().datetime().optional(),
      lastAcknowledgedChangeId: z.string().optional(),
    })
    .optional(),
  changes: z.array(syncChangeSchema),
});

export const syncPullSchema = z.object({
  deviceId: z.string().min(1),
  cursor: z
    .object({
      deviceId: z.string().min(1),
      lastPulledAt: z.string().datetime().optional(),
      lastAcknowledgedChangeId: z.string().optional(),
    })
    .optional(),
});
