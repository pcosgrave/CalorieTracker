import type { FoodProduct } from "@calorie-tracker/shared";
import { listFoodProducts } from "./store.js";

export class FoodCandidateService {
  async loadUserFoods(userId: string): Promise<FoodProduct[]> {
    return listFoodProducts(userId);
  }
}
