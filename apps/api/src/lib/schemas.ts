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

export const createDiaryEntrySchema = z.object({
  productId: z.string().min(1),
  loggedAt: z.string().datetime(),
  meal: z.enum(["breakfast", "lunch", "dinner", "snack"]),
  servingMultiplier: z.number().positive(),
});

export const emptySchema = z.object({});
