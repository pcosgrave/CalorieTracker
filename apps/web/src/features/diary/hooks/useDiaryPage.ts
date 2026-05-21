import { useEffect, useMemo, useState } from "react";
import type { DiaryEntry, MealType } from "@calorie-tracker/shared";
import {
  entryDateKey,
  entriesForDate,
  readDiaryEntries,
  round,
  shiftDateKey,
  todayDateKey,
  toNumber,
  totalsForEntries,
  writeDiaryEntries,
} from "@/app/lib/diary";
import type { EntryEditForm } from "../types";
import { groupEntriesByMeal } from "../utils";

export function useDiaryPage() {
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

  return {
    dateOptions,
    editForm,
    groupedEntries,
    openMenuId,
    selectedDate,
    selectedEntries,
    totals,
    clearSelectedDay,
    removeEntry,
    saveEditedEntry,
    setEditForm,
    setOpenMenuId,
    setSelectedDate,
    startEditingEntry,
    updateEditForm,
  };
}
