import type { FoodProduct, MealType, Nutrients } from "@calorie-tracker/shared";

export type CatalogKind = "ingredient" | "recipe";
export type SortMode = "alphabetical" | "frequency" | "recent";

export type CatalogItem = {
  id: string;
  kind: CatalogKind;
  name: string;
  brand?: string;
  barcode?: string;
  servingLabel: string;
  servingUnit: string;
  servingQuantity: number;
  servingGrams?: number;
  nutrients: Nutrients;
  ingredients?: string[];
  frequency: number;
  lastUsedDaysAgo: number;
};

export type RecipeComponent = {
  item: {
    name: string;
    servingLabel: string;
  };
  amount: string;
  unit: string;
};

export type StoredRecipeProduct = FoodProduct & {
  recipeComponents?: RecipeComponent[];
};

export type LogState = {
  date: string;
  meal: MealType;
  servingAmount: string;
  servingUnit: string;
};
