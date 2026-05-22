"use client";

import Link from "next/link";
import { FormEvent, useEffect, useState } from "react";
import type { FoodProduct, MealType } from "@calorie-tracker/shared";
import {
  createId,
  mealLabels,
  mealOrder,
  currentOwnerUserId,
  shiftDateKey,
  toNumber,
  todayDateKey,
  writeDiaryEntry,
} from "../../lib/diary";
import styles from "../../page.module.css";

export default function QuickCaloriesPage() {
  const [date, setDate] = useState(todayDateKey());
  const [calories, setCalories] = useState("");
  const [meal, setMeal] = useState<MealType>("snack");

  useEffect(() => {
    setDate(new URLSearchParams(window.location.search).get("date") || todayDateKey());
  }, []);

  function logQuickCalories(event: FormEvent<HTMLFormElement>): void {
    event.preventDefault();

    const calorieTotal = toNumber(calories);
    if (calorieTotal <= 0) {
      return;
    }

    const now = new Date().toISOString();
    const product: FoodProduct = {
      productId: createId("quick"),
      ownerUserId: currentOwnerUserId(),
      visibility: "private",
      name: "Quick calories",
      serving: {
        label: "1 entry",
        quantity: 1,
        unit: "entry",
      },
      nutrients: {
        calories: calorieTotal,
        proteinGrams: 0,
        carbohydrateGrams: 0,
        fatGrams: 0,
      },
      createdAt: now,
      updatedAt: now,
    };

    writeDiaryEntry(product, date, meal, 1);
    window.location.href = `/?date=${date}`;
  }

  return (
    <main className={styles.page}>
      <div className={styles.shell}>
        <header className={styles.topbar}>
          <div className={styles.brand}>
            <h1>Quick Calories</h1>
            <p>Log a calorie total without creating a reusable food.</p>
          </div>
          <Link className={styles.textButton} href={`/add?date=${date}`}>
            Back to add food
          </Link>
        </header>

        <section className={styles.panel}>
          <form className={styles.form} onSubmit={logQuickCalories}>
            <div className={styles.dayPickRow}>
              <strong>Day</strong>
              <div className={styles.dayStepper}>
                <button className="secondary" type="button" onClick={() => setDate((current) => shiftDateKey(current, -1))}>
                  &lt;
                </button>
                <input aria-label="Quick calorie date" type="date" value={date} onChange={(event) => setDate(event.target.value)} />
                <button className="secondary" type="button" onClick={() => setDate((current) => shiftDateKey(current, 1))}>
                  &gt;
                </button>
              </div>
            </div>

            <div className={styles.twoColumn}>
              <label>
                Calories
                <input
                  inputMode="decimal"
                  min="1"
                  placeholder="250"
                  required
                  type="number"
                  value={calories}
                  onChange={(event) => setCalories(event.target.value)}
                />
              </label>
              <label>
                Meal
                <select value={meal} onChange={(event) => setMeal(event.target.value as MealType)}>
                  {mealOrder.map((mealOption) => (
                    <option key={mealOption} value={mealOption}>
                      {mealLabels[mealOption]}
                    </option>
                  ))}
                </select>
              </label>
            </div>

            <div className={styles.actions}>
              <Link className={styles.textButton} href={`/add?date=${date}`}>
                Cancel
              </Link>
              <button disabled={toNumber(calories) <= 0} type="submit">
                Log calories
              </button>
            </div>
          </form>
        </section>
      </div>
    </main>
  );
}
