import type { DiaryEntry, FoodProduct, MealType, Nutrients } from "@calorie-tracker/shared";

export const storageKey = "calorie-tracker:diary:v1";
export const foodStorageKey = "calorie-tracker:foods:v1";
export const recipeStorageKey = "calorie-tracker:recipes:v1";
export const recipeDraftStorageKey = "calorie-tracker:recipe-draft:v1";
export const ownerUserId = "local";

export const mealOrder: MealType[] = ["breakfast", "lunch", "dinner", "snack"];

export const mealLabels: Record<MealType, string> = {
  breakfast: "Breakfast",
  lunch: "Lunch",
  dinner: "Dinner",
  snack: "Snack",
};

export type NutritionTotals = {
  calories: number;
  protein: number;
  carbs: number;
  fat: number;
};

export const emptyTotals: NutritionTotals = {
  calories: 0,
  protein: 0,
  carbs: 0,
  fat: 0,
};

export function toNumber(value: string): number {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : 0;
}

export function round(value: number): number {
  return Math.round(value * 10) / 10;
}

export function scaleNutrients(nutrients: Nutrients, multiplier: number): Nutrients {
  return {
    calories: round(nutrients.calories * multiplier),
    proteinGrams: round(nutrients.proteinGrams * multiplier),
    carbohydrateGrams: round(nutrients.carbohydrateGrams * multiplier),
    fatGrams: round(nutrients.fatGrams * multiplier),
  };
}

export function totalsForEntries(entries: DiaryEntry[]): NutritionTotals {
  return entries.reduce((acc, entry) => {
    const nutrients = scaleNutrients(entry.productSnapshot.nutrients, entry.servingMultiplier);

    return {
      calories: round(acc.calories + nutrients.calories),
      protein: round(acc.protein + nutrients.proteinGrams),
      carbs: round(acc.carbs + nutrients.carbohydrateGrams),
      fat: round(acc.fat + nutrients.fatGrams),
    };
  }, emptyTotals);
}

export function createId(prefix: string): string {
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) {
    return `${prefix}-${crypto.randomUUID()}`;
  }

  return `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2)}`;
}

export function todayDateKey(): string {
  const now = new Date();
  return toDateKey(now);
}

export function toDateKey(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");

  return `${year}-${month}-${day}`;
}

export function entryDateKey(entry: DiaryEntry): string {
  return entry.loggedAt.slice(0, 10);
}

export function shiftDateKey(dateKey: string, offsetDays: number): string {
  const date = new Date(`${dateKey}T12:00:00`);
  date.setDate(date.getDate() + offsetDays);
  return toDateKey(date);
}

export function formatDateHeading(dateKey: string): string {
  const today = todayDateKey();
  if (dateKey === today) {
    return "Today";
  }

  if (dateKey === shiftDateKey(today, -1)) {
    return "Yesterday";
  }

  if (dateKey === shiftDateKey(today, 1)) {
    return "Tomorrow";
  }

  return new Intl.DateTimeFormat("en", {
    weekday: "long",
    month: "short",
    day: "numeric",
  }).format(new Date(`${dateKey}T12:00:00`));
}

export function formatDateChip(dateKey: string): { day: string; label: string } {
  const date = new Date(`${dateKey}T12:00:00`);

  return {
    day: new Intl.DateTimeFormat("en", { weekday: "short" }).format(date),
    label: new Intl.DateTimeFormat("en", { month: "short", day: "numeric" }).format(date),
  };
}

export function readDiaryEntries(): DiaryEntry[] {
  try {
    const stored = window.localStorage.getItem(storageKey);
    if (!stored) {
      return [];
    }

    const parsed = JSON.parse(stored);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

export function writeDiaryEntries(entries: DiaryEntry[]): void {
  window.localStorage.setItem(storageKey, JSON.stringify(entries));
}

export function writeDiaryEntry(product: FoodProduct, date: string, meal: MealType, servingMultiplier: number): void {
  const now = new Date().toISOString();
  const entry: DiaryEntry = {
    entryId: createId("entry"),
    ownerUserId,
    productId: product.productId,
    loggedAt: `${date}T12:00:00.000Z`,
    meal,
    servingMultiplier,
    productSnapshot: product,
    createdAt: now,
    updatedAt: now,
  };

  writeDiaryEntries([entry, ...readDiaryEntries()]);
}

export function readFoodProducts(): FoodProduct[] {
  try {
    const stored = window.localStorage.getItem(foodStorageKey);
    if (!stored) {
      return [];
    }

    const parsed = JSON.parse(stored);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

export function writeFoodProduct(product: FoodProduct): void {
  const products = readFoodProducts();
  const withoutExisting = products.filter((existing) => existing.productId !== product.productId);
  window.localStorage.setItem(foodStorageKey, JSON.stringify([product, ...withoutExisting]));
}

export function deleteFoodProduct(productId: string): void {
  window.localStorage.setItem(foodStorageKey, JSON.stringify(readFoodProducts().filter((product) => product.productId !== productId)));
}

export function readRecipeProducts(): FoodProduct[] {
  try {
    const stored = window.localStorage.getItem(recipeStorageKey);
    if (!stored) {
      return [];
    }

    const parsed = JSON.parse(stored);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

export function writeRecipeProduct(product: FoodProduct): void {
  const recipes = readRecipeProducts();
  const withoutExisting = recipes.filter((existing) => existing.productId !== product.productId);
  window.localStorage.setItem(recipeStorageKey, JSON.stringify([product, ...withoutExisting]));
}

export function deleteRecipeProduct(productId: string): void {
  window.localStorage.setItem(recipeStorageKey, JSON.stringify(readRecipeProducts().filter((product) => product.productId !== productId)));
}

export function entriesForDate(entries: DiaryEntry[], dateKey: string): DiaryEntry[] {
  return entries.filter((entry) => entryDateKey(entry) === dateKey);
}
