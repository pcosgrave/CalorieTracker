import type { FoodProduct, Nutrients } from "@calorie-tracker/shared";
import { createId, ownerUserId, todayDateKey } from "@/app/lib/diary";
import type { CatalogItem, CatalogKind, LogState, StoredRecipeProduct } from "./types";

export function dateFromUrl(): string {
  const params = new URLSearchParams(window.location.search);
  return params.get("date") || todayDateKey();
}

export function defaultLogState(date: string): LogState {
  return {
    date,
    meal: "breakfast",
    servingAmount: "1",
    servingUnit: "serving",
  };
}

export function itemServingUnit(item: CatalogItem): string {
  return item.servingUnit || "serving";
}

export function makeProductFromItem(item: CatalogItem, now: string, nutrients: Nutrients): FoodProduct {
  return {
    productId: createId(item.kind),
    ownerUserId,
    visibility: "private",
    barcode: item.barcode,
    name: item.name,
    brand: item.brand,
    serving: {
      label: item.servingLabel,
      quantity: item.servingQuantity,
      unit: itemServingUnit(item),
      grams: item.servingGrams,
    },
    nutrients,
    createdAt: now,
    updatedAt: now,
  };
}

export function productToCatalogItem(product: FoodProduct, kind: CatalogKind): CatalogItem {
  const recipeProduct = product as StoredRecipeProduct;
  const item: CatalogItem = {
    id: product.productId,
    kind,
    name: product.name,
    servingLabel: product.serving.label,
    servingQuantity: product.serving.quantity,
    servingUnit: product.serving.unit,
    nutrients: product.nutrients,
    frequency: 0,
    lastUsedDaysAgo: 0,
  };

  if (product.brand) {
    item.brand = product.brand;
  }

  if (product.barcode) {
    item.barcode = product.barcode;
  }

  if (product.serving.grams) {
    item.servingGrams = product.serving.grams;
  }

  if (recipeProduct.recipeComponents?.length) {
    item.ingredients = recipeProduct.recipeComponents.map((component) => `${component.item.name} - ${component.amount} ${component.unit}`);
  }

  return item;
}

export function splitIngredientLabel(ingredient: string): { name: string; serving?: string } {
  const [name, ...servingParts] = ingredient.split(" - ");
  const serving = servingParts.join(" - ");

  return {
    name: name || ingredient,
    ...(serving ? { serving } : {}),
  };
}
