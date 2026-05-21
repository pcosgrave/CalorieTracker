import type { DiaryEntry, MealType } from "@calorie-tracker/shared";
import { mealOrder } from "@/app/lib/diary";

export function groupEntriesByMeal(entries: DiaryEntry[]): Record<MealType, DiaryEntry[]> {
  return mealOrder.reduce(
    (groups, meal) => ({
      ...groups,
      [meal]: entries.filter((entry) => entry.meal === meal),
    }),
    {} as Record<MealType, DiaryEntry[]>,
  );
}
