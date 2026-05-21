"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import type { DiaryEntry, MealType } from "@calorie-tracker/shared";
import {
  entryDateKey,
  entriesForDate,
  formatDateChip,
  formatDateHeading,
  mealLabels,
  mealOrder,
  readDiaryEntries,
  round,
  scaleNutrients,
  shiftDateKey,
  todayDateKey,
  totalsForEntries,
  toNumber,
  writeDiaryEntries,
} from "./lib/diary";
import styles from "./page.module.css";

type EntryEditForm = {
  entryId: string;
  name: string;
  brand: string;
  calories: string;
  protein: string;
  carbs: string;
  fat: string;
  servingMultiplier: string;
  meal: MealType;
  date: string;
};

function groupEntriesByMeal(entries: DiaryEntry[]): Record<MealType, DiaryEntry[]> {
  return mealOrder.reduce(
    (groups, meal) => ({
      ...groups,
      [meal]: entries.filter((entry) => entry.meal === meal),
    }),
    {} as Record<MealType, DiaryEntry[]>,
  );
}

export default function Home() {
  const [entries, setEntries] = useState<DiaryEntry[]>([]);
  const [loaded, setLoaded] = useState(false);
  const [selectedDate, setSelectedDate] = useState(todayDateKey);
  const [openMenuId, setOpenMenuId] = useState("");
  const [editForm, setEditForm] = useState<EntryEditForm | null>(null);

  useEffect(() => {
    setEntries(readDiaryEntries());
    setSelectedDate(new URLSearchParams(window.location.search).get("date") || todayDateKey());
    setLoaded(true);
  }, []);

  useEffect(() => {
    if (loaded) {
      writeDiaryEntries(entries);
    }
  }, [entries, loaded]);

  const selectedEntries = useMemo(() => entriesForDate(entries, selectedDate), [entries, selectedDate]);
  const totals = useMemo(() => totalsForEntries(selectedEntries), [selectedEntries]);
  const groupedEntries = useMemo(() => groupEntriesByMeal(selectedEntries), [selectedEntries]);
  const dateOptions = useMemo(
    () => Array.from({ length: 9 }, (_, index) => shiftDateKey(selectedDate, index - 4)),
    [selectedDate],
  );

  function removeEntry(entryId: string): void {
    setEntries((current) => current.filter((entry) => entry.entryId !== entryId));
    setOpenMenuId("");
    setEditForm((current) => (current?.entryId === entryId ? null : current));
  }

  function clearSelectedDay(): void {
    setEntries((current) => current.filter((entry) => entryDateKey(entry) !== selectedDate));
    setEditForm(null);
    setOpenMenuId("");
  }

  function startEditingEntry(entry: DiaryEntry): void {
    setEditForm({
      entryId: entry.entryId,
      name: entry.productSnapshot.name,
      brand: entry.productSnapshot.brand || "",
      calories: String(entry.productSnapshot.nutrients.calories),
      protein: String(entry.productSnapshot.nutrients.proteinGrams),
      carbs: String(entry.productSnapshot.nutrients.carbohydrateGrams),
      fat: String(entry.productSnapshot.nutrients.fatGrams),
      servingMultiplier: String(entry.servingMultiplier),
      meal: entry.meal,
      date: entryDateKey(entry),
    });
    setOpenMenuId("");
  }

  function updateEditForm(field: keyof EntryEditForm, value: string): void {
    setEditForm((current) => {
      if (!current) {
        return current;
      }

      return { ...current, [field]: field === "meal" ? (value as MealType) : value };
    });
  }

  function saveEditedEntry(): void {
    if (!editForm) {
      return;
    }

    const now = new Date().toISOString();
    setEntries((current) =>
      current.map((entry) => {
        if (entry.entryId !== editForm.entryId) {
          return entry;
        }

        return {
          ...entry,
          loggedAt: `${editForm.date}T12:00:00.000Z`,
          meal: editForm.meal,
          servingMultiplier: Math.max(toNumber(editForm.servingMultiplier), 0.1),
          updatedAt: now,
          productSnapshot: {
            ...entry.productSnapshot,
            name: editForm.name.trim() || entry.productSnapshot.name,
            brand: editForm.brand.trim(),
            nutrients: {
              calories: round(toNumber(editForm.calories)),
              proteinGrams: round(toNumber(editForm.protein)),
              carbohydrateGrams: round(toNumber(editForm.carbs)),
              fatGrams: round(toNumber(editForm.fat)),
            },
            updatedAt: now,
          },
        };
      }),
    );
    setSelectedDate(editForm.date);
    setEditForm(null);
  }

  return (
    <main className={styles.page}>
      <div className={styles.shell}>
        <header className={styles.topbar}>
          <div className={styles.brand}>
            <h1>CalorieTracker</h1>
            <p>Manual labels, private barcode shortcuts, cloud sync when signed in.</p>
          </div>
          <button type="button">Sign in with Google</button>
        </header>

        <section className={styles.panel}>
          <div className={styles.dayControls}>
            <button
              aria-label="Previous day"
              className="secondary"
              type="button"
              onClick={() => setSelectedDate((current) => shiftDateKey(current, -1))}
            >
              &lt;
            </button>
            <div className={styles.dateScroller} aria-label="Select day">
              {dateOptions.map((dateKey) => {
                const chip = formatDateChip(dateKey);
                const isSelected = dateKey === selectedDate;

                return (
                  <button
                    className={isSelected ? styles.dateChipSelected : styles.dateChip}
                    key={dateKey}
                    type="button"
                    onClick={() => setSelectedDate(dateKey)}
                  >
                    <span>{chip.day}</span>
                    <strong>{chip.label}</strong>
                  </button>
                );
              })}
            </div>
            <button
              aria-label="Next day"
              className="secondary"
              type="button"
              onClick={() => setSelectedDate((current) => shiftDateKey(current, 1))}
            >
              &gt;
            </button>
          </div>
        </section>

        <section className={styles.panel}>
          <div className={styles.panelHeader}>
            <div>
              <h2>{formatDateHeading(selectedDate)}</h2>
              <p className={styles.subtle}>{selectedDate}</p>
            </div>
            <button className="secondary" disabled={selectedEntries.length === 0} type="button" onClick={clearSelectedDay}>
              Clear day
            </button>
          </div>

          <div className={styles.daySummary}>
            <div className={styles.metric}>
              <strong>{totals.calories}</strong>
              <span>Calories</span>
            </div>
            <div className={styles.metric}>
              <strong>{totals.protein}g</strong>
              <span>Protein</span>
            </div>
            <div className={styles.metric}>
              <strong>{totals.carbs}g</strong>
              <span>Carbs</span>
            </div>
            <div className={styles.metric}>
              <strong>{totals.fat}g</strong>
              <span>Fat</span>
            </div>
          </div>

          <div className={styles.addRow}>
            <Link aria-label={`Add food for ${selectedDate}`} className={styles.addButton} href={`/add?date=${selectedDate}`}>
              +
            </Link>
          </div>
        </section>

        {editForm ? (
          <section className={styles.panel}>
            <div className={styles.panelHeader}>
              <h2>Edit logged food</h2>
              <button className="secondary" type="button" onClick={() => setEditForm(null)}>
                Cancel
              </button>
            </div>
            <div className={styles.form}>
              <div className={styles.twoColumn}>
                <label>
                  Name
                  <input value={editForm.name} onChange={(event) => updateEditForm("name", event.target.value)} />
                </label>
                <label>
                  Brand
                  <input value={editForm.brand} onChange={(event) => updateEditForm("brand", event.target.value)} />
                </label>
              </div>
              <div className={styles.fourColumn}>
                <label>
                  Calories
                  <input min="0" type="number" value={editForm.calories} onChange={(event) => updateEditForm("calories", event.target.value)} />
                </label>
                <label>
                  Protein
                  <input min="0" type="number" value={editForm.protein} onChange={(event) => updateEditForm("protein", event.target.value)} />
                </label>
                <label>
                  Carbs
                  <input min="0" type="number" value={editForm.carbs} onChange={(event) => updateEditForm("carbs", event.target.value)} />
                </label>
                <label>
                  Fat
                  <input min="0" type="number" value={editForm.fat} onChange={(event) => updateEditForm("fat", event.target.value)} />
                </label>
              </div>
              <div className={styles.twoColumn}>
                <label>
                  Servings
                  <input
                    min="0.1"
                    step="0.1"
                    type="number"
                    value={editForm.servingMultiplier}
                    onChange={(event) => updateEditForm("servingMultiplier", event.target.value)}
                  />
                </label>
                <label>
                  Meal
                  <select value={editForm.meal} onChange={(event) => updateEditForm("meal", event.target.value)}>
                    {mealOrder.map((meal) => (
                      <option key={meal} value={meal}>
                        {mealLabels[meal]}
                      </option>
                    ))}
                  </select>
                </label>
              </div>
              <label>
                Day
                <input type="date" value={editForm.date} onChange={(event) => updateEditForm("date", event.target.value)} />
              </label>
              <div className={styles.actions}>
                <button type="button" onClick={saveEditedEntry}>
                  Save changes
                </button>
              </div>
            </div>
          </section>
        ) : null}

        <section className={styles.mealList}>
          {mealOrder.map((meal) => {
            const mealEntries = groupedEntries[meal];
            const mealTotals = totalsForEntries(mealEntries);

            return (
              <section className={styles.panel} key={meal}>
                <div className={styles.mealHeader}>
                  <div>
                    <h2>{mealLabels[meal]}</h2>
                    <p className={styles.subtle}>{mealEntries.length} foods</p>
                  </div>
                  <strong>{mealTotals.calories} cal</strong>
                </div>

                <div className={styles.list}>
                  {mealEntries.length === 0 ? (
                    <p className={styles.empty}>No food logged.</p>
                  ) : (
                    mealEntries.map((entry) => {
                      const food = entry.productSnapshot;
                      const nutrients = scaleNutrients(food.nutrients, entry.servingMultiplier);

                      return (
                        <article className={styles.item} key={entry.entryId}>
                          <button className={styles.itemMain} type="button" onClick={() => startEditingEntry(entry)}>
                            <strong>{food.name}</strong>
                            <span>
                              {food.brand || "No brand"} - {food.serving.label}
                              {entry.servingMultiplier !== 1 ? ` x ${entry.servingMultiplier}` : ""}
                              {food.barcode ? ` - ${food.barcode}` : ""}
                            </span>
                          </button>
                          <div className={styles.itemActions}>
                            <strong>{nutrients.calories} cal</strong>
                            <button
                              aria-label={`Options for ${food.name}`}
                              className={styles.menuButton}
                              type="button"
                              onClick={() => setOpenMenuId((current) => (current === entry.entryId ? "" : entry.entryId))}
                            >
                              ⋮
                            </button>
                            {openMenuId === entry.entryId ? (
                              <div className={styles.menuPanel}>
                                <button type="button" onClick={() => startEditingEntry(entry)}>
                                  Edit
                                </button>
                                <button type="button" onClick={() => removeEntry(entry.entryId)}>
                                  Delete
                                </button>
                              </div>
                            ) : null}
                          </div>
                        </article>
                      );
                    })
                  )}
                </div>
              </section>
            );
          })}
        </section>
      </div>
    </main>
  );
}
