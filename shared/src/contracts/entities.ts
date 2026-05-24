import type { MealType, Nutrients, Serving, Visibility } from "./core.js";

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

export interface WeightEntry {
  entryId: string;
  ownerUserId: string;
  loggedAt: string;
  weightKg: number;
  source: "manual" | "health_connect" | "import";
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

export interface CreateWeightEntryRequest {
  loggedAt: string;
  weightKg: number;
  source?: "manual" | "health_connect" | "import";
}

export interface BarcodeLookupResponse {
  found: boolean;
  product?: FoodProduct | undefined;
}
