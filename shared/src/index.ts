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

export interface FoodProduct {
  productId: string;
  ownerUserId: string;
  visibility: Visibility;
  barcode?: string | undefined;
  name: string;
  brand?: string | undefined;
  serving: Serving;
  nutrients: Nutrients;
  createdAt: string;
  updatedAt: string;
}

export interface BarcodeAlias {
  barcode: string;
  ownerUserId: string;
  productId: string;
  visibility: Visibility;
  createdAt: string;
}

export interface DiaryEntry {
  entryId: string;
  ownerUserId: string;
  productId: string;
  loggedAt: string;
  meal: MealType;
  servingMultiplier: number;
  productSnapshot: FoodProduct;
  createdAt: string;
  updatedAt: string;
}

export interface CreateFoodProductRequest {
  barcode?: string | undefined;
  name: string;
  brand?: string | undefined;
  serving: Serving;
  nutrients: Nutrients;
}

export interface CreateDiaryEntryRequest {
  productId: string;
  loggedAt: string;
  meal: MealType;
  servingMultiplier: number;
}

export interface BarcodeLookupResponse {
  found: boolean;
  product?: FoodProduct | undefined;
}
