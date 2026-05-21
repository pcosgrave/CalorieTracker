export type Visibility = "private" | "shared" | "global";

export type MealType = "breakfast" | "lunch" | "dinner" | "snack";

export interface Nutrients {
  calories: number;
  proteinGrams: number;
  carbohydrateGrams: number;
  fatGrams: number;
  fiberGrams?: number | undefined;
  sugarGrams?: number | undefined;
  sodiumMilligrams?: number | undefined;
}

export interface Serving {
  label: string;
  quantity: number;
  unit: string;
  grams?: number | undefined;
}
