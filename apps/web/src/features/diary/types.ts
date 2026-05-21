import type { MealType } from "@calorie-tracker/shared";

export type EntryEditForm = {
  entryId: string;
  name: string;
  brand: string;
  calories: string;
  protein: string;
  carbs: string;
  fat: string;
  servingMultiplier: string;
  meal: MealType;
  date: string;
};
