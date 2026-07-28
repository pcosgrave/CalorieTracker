import { json, route } from "../lib/http.js";
import { FoodCandidateService } from "../lib/food-candidate-service.js";
import { GeminiFoodLogService } from "../lib/gemini-food-log-service.js";
import { aiFoodLogParseSchema } from "../lib/schemas.js";

const foodCandidateService = new FoodCandidateService();
const geminiFoodLogService = new GeminiFoodLogService();

export const parseFoodLog = route(aiFoodLogParseSchema, async ({ userId, body }) => {
  const transcript = body.transcript.trim();
  const meal = body.fallbackMeal;
  const foods = await foodCandidateService.loadUserFoods(userId);
  const parsed = await geminiFoodLogService.parseFoodLog(transcript, body.date, meal);

  console.info("AI parse loaded user foods", {
    userId,
    foodCount: foods.length,
    parsedEntryCount: parsed.entries.length,
  });

  return json(200, {
    entries: parsed.entries.map((entry) => {
      const matchedFood = foodCandidateService.matchParsedFood(
        {
          foodName: entry.foodName,
          ...(entry.brand ? { brand: entry.brand } : {}),
        },
        foods,
      );

      return {
        foodName: entry.foodName,
        brand: entry.brand ?? "",
        meal: entry.meal ?? meal,
        quantity: entry.quantity,
        unit: entry.unit ?? "",
        matchStatus: matchedFood ? "Matched" : "Unresolved",
        matchedFoodId: matchedFood?.productId,
        notes: entry.notes ?? (matchedFood ? "Matched to an existing food" : "Needs review"),
      };
    }),
    createdFoods: [],
  });
});
