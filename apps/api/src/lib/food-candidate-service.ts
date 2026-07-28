import type { FoodProduct } from "@calorie-tracker/shared";
import { listFoodProducts } from "./store.js";

export interface ParsedFoodCandidate {
  foodName: string;
  brand?: string;
}

export interface FoodMatchCandidate {
  productId: string;
  name: string;
  brand?: string | undefined;
  servingQuantity: number;
  servingUnit: string;
}

export class FoodCandidateService {
  async loadUserFoods(userId: string): Promise<FoodProduct[]> {
    return listFoodProducts(userId);
  }

  matchParsedFood(
    parsed: ParsedFoodCandidate,
    foods: FoodProduct[],
  ): FoodMatchCandidate | null {
    const normalizedFoodName = normalizeText(parsed.foodName);
    const normalizedBrand = normalizeText(parsed.brand ?? "");

    const ranked = foods
      .map((food) => ({
        food,
        score: scoreFoodMatch(food, normalizedFoodName, normalizedBrand),
      }))
      .filter((candidate) => candidate.score >= 0)
      .sort((left, right) => right.score - left.score);

    const best = ranked[0];
    if (!best || best.score < 70) {
      return null;
    }

    return {
      productId: best.food.productId,
      name: best.food.name,
      ...(best.food.brand ? { brand: best.food.brand } : {}),
      servingQuantity: best.food.serving.quantity,
      servingUnit: best.food.serving.unit,
    };
  }
}

function scoreFoodMatch(
  food: FoodProduct,
  normalizedFoodName: string,
  normalizedBrand: string,
): number {
  const normalizedName = normalizeText(food.name);
  const candidateBrand = normalizeText(food.brand ?? "");

  if (!normalizedFoodName) {
    return -1;
  }

  let score = -1;

  if (normalizedName === normalizedFoodName) {
    score = 100;
  } else if (normalizedName.includes(normalizedFoodName) || normalizedFoodName.includes(normalizedName)) {
    score = 85;
  } else {
    const overlap = overlapScore(normalizedName, normalizedFoodName);
    if (overlap >= 0.75) {
      score = 70;
    } else if (overlap >= 0.5) {
      score = 55;
    }
  }

  if (score < 0) {
    return -1;
  }

  if (normalizedBrand) {
    if (candidateBrand === normalizedBrand) {
      score += 15;
    } else if (candidateBrand && (candidateBrand.includes(normalizedBrand) || normalizedBrand.includes(candidateBrand))) {
      score += 8;
    } else {
      score -= 10;
    }
  }

  return score;
}

function overlapScore(left: string, right: string): number {
  const leftTokens = tokenSet(left);
  const rightTokens = tokenSet(right);
  if (leftTokens.size === 0 || rightTokens.size === 0) {
    return 0;
  }

  let matches = 0;
  for (const token of leftTokens) {
    if (rightTokens.has(token)) {
      matches += 1;
    }
  }

  return matches / Math.max(leftTokens.size, rightTokens.size);
}

function tokenSet(value: string): Set<string> {
  return new Set(
    value
      .split(" ")
      .map((token) => token.trim())
      .filter((token) => token.length > 0),
  );
}

function normalizeText(value: string): string {
  return value
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, " ")
    .replace(/\s+/g, " ");
}
