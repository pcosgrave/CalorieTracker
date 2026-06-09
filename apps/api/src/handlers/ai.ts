import { json, route } from "../lib/http.js";
import { FoodCandidateService } from "../lib/food-candidate-service.js";
import { aiFoodLogParseSchema } from "../lib/schemas.js";

const foodCandidateService = new FoodCandidateService();

export const parseFoodLog = route(aiFoodLogParseSchema, async ({ userId, body }) => {
  const transcript = body.transcript.trim();
  const meal = body.fallbackMeal;
  const foods = await foodCandidateService.loadUserFoods(userId);

  console.info("AI parse loaded user foods", {
    userId,
    foodCount: foods.length,
  });

  return json(200, {
    entries: [
      {
        foodName: transcript.length > 0 ? "Mocked oatmeal bowl" : "Mocked breakfast bowl",
        calories: 340,
        meal,
        servingQuantity: 1,
        servingUnit: "bowl",
      },
      {
        foodName: "Mocked grilled chicken salad",
        calories: 420,
        meal: "Lunch",
        servingQuantity: 1,
        servingUnit: "plate",
      },
      {
        foodName: "Mocked yogurt parfait",
        calories: 190,
        meal: "Snack",
        servingQuantity: 1,
        servingUnit: "cup",
      },
    ],
    createdFoods: [],
  });
});
