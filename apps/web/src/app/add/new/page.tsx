"use client";

import Link from "next/link";
import { FormEvent, useEffect, useState } from "react";
import type { FoodProduct } from "@calorie-tracker/shared";
import { createId, ownerUserId, readFoodProducts, toNumber, todayDateKey, writeFoodProduct } from "../../lib/diary";
import styles from "../../page.module.css";

type NewFoodForm = {
  name: string;
  brand: string;
  barcode: string;
  servingQuantity: string;
  servingUnit: string;
  calories: string;
  protein: string;
  carbs: string;
  fat: string;
};

const measurementUnits = [
  "bar",
  "bottle",
  "box",
  "can",
  "container",
  "cup",
  "fl oz",
  "gram",
  "jar",
  "kg",
  "lb",
  "liter",
  "milligram",
  "ml",
  "oz",
  "package",
  "pint",
  "quart",
  "service",
  "tbsp",
  "tsp",
];

const emptyForm: NewFoodForm = {
  name: "",
  brand: "",
  barcode: "",
  servingQuantity: "1",
  servingUnit: "serving",
  calories: "",
  protein: "",
  carbs: "",
  fat: "",
};

export default function NewFoodPage() {
  const [form, setForm] = useState<NewFoodForm>(emptyForm);
  const [date, setDate] = useState(todayDateKey());
  const [editId, setEditId] = useState("");
  const [returnTo, setReturnTo] = useState("");
  const [savedName, setSavedName] = useState("");

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const editProductId = params.get("edit") || "";
    const existingProduct = editProductId ? readFoodProducts().find((product) => product.productId === editProductId) : undefined;

    setDate(params.get("date") || todayDateKey());
    setEditId(editProductId);
    setReturnTo(params.get("returnTo") || "");
    setForm((current) => ({
      ...current,
      name: existingProduct?.name || params.get("name") || "",
      brand: existingProduct?.brand || params.get("brand") || "",
      barcode: existingProduct?.barcode || params.get("barcode") || "",
      servingQuantity: existingProduct ? String(existingProduct.serving.quantity) : params.get("servingQuantity") || current.servingQuantity,
      servingUnit: existingProduct?.serving.unit || params.get("servingUnit") || current.servingUnit,
      calories: existingProduct ? String(existingProduct.nutrients.calories) : params.get("calories") || current.calories,
      protein: existingProduct ? String(existingProduct.nutrients.proteinGrams) : params.get("protein") || current.protein,
      carbs: existingProduct ? String(existingProduct.nutrients.carbohydrateGrams) : params.get("carbs") || current.carbs,
      fat: existingProduct ? String(existingProduct.nutrients.fatGrams) : params.get("fat") || current.fat,
    }));
  }, []);

  function updateField(field: keyof NewFoodForm, value: string): void {
    setForm((current) => ({ ...current, [field]: value }));
  }

  function saveFood(event: FormEvent<HTMLFormElement>): void {
    event.preventDefault();

    const now = new Date().toISOString();
    const servingQuantity = Math.max(toNumber(form.servingQuantity), 0.1);
    const product: FoodProduct = {
      productId: editId || createId("custom"),
      ownerUserId,
      visibility: "private",
      barcode: form.barcode.trim() || undefined,
      name: form.name.trim(),
      brand: form.brand.trim(),
      serving: {
        label: `${servingQuantity} ${form.servingUnit}`,
        quantity: servingQuantity,
        unit: form.servingUnit,
      },
      nutrients: {
        calories: toNumber(form.calories),
        proteinGrams: toNumber(form.protein),
        carbohydrateGrams: toNumber(form.carbs),
        fatGrams: toNumber(form.fat),
      },
      createdAt: now,
      updatedAt: now,
    };

    writeFoodProduct(product);
    setSavedName(product.name);
    window.location.href = returnTo || `/add?date=${date}`;
  }

  const canSave =
    form.name.trim().length > 0 &&
    toNumber(form.servingQuantity) > 0 &&
    toNumber(form.calories) > 0;

  return (
    <main className={styles.page}>
      <div className={styles.shell}>
        <header className={styles.topbar}>
          <div className={styles.brand}>
            <h1>Add New Food</h1>
            <p>Save a reusable food item for search and barcode lookup.</p>
          </div>
          <Link className={styles.textButton} href={`/add?date=${date}`}>
            Back to search
          </Link>
        </header>

        {savedName ? <p className={styles.success}>Saved {savedName}.</p> : null}

        <section className={styles.panel}>
          <form className={styles.form} onSubmit={saveFood}>
            <div className={styles.twoColumn}>
              <label>
                Name
                <input required value={form.name} onChange={(event) => updateField("name", event.target.value)} />
              </label>
              <label>
                Brand
                <input value={form.brand} onChange={(event) => updateField("brand", event.target.value)} />
              </label>
            </div>

            <label>
              Barcode
              <input
                inputMode="numeric"
                placeholder="Scan or type barcode"
                value={form.barcode}
                onChange={(event) => updateField("barcode", event.target.value)}
              />
            </label>

            <div className={styles.twoColumn}>
              <label>
                Serving size
                <input
                  inputMode="decimal"
                  min="0.1"
                  required
                  step="0.1"
                  type="number"
                  value={form.servingQuantity}
                  onChange={(event) => updateField("servingQuantity", event.target.value)}
                />
              </label>
              <label>
                Unit
                <select value={form.servingUnit} onChange={(event) => updateField("servingUnit", event.target.value)}>
                  <option value="serving">serving</option>
                  {measurementUnits.map((unit) => (
                    <option key={unit} value={unit}>
                      {unit}
                    </option>
                  ))}
                </select>
              </label>
            </div>

            <div className={styles.fourColumn}>
              <label>
                Calories
                <input
                  inputMode="decimal"
                  min="0"
                  required
                  type="number"
                  value={form.calories}
                  onChange={(event) => updateField("calories", event.target.value)}
                />
              </label>
              <label>
                Protein
                <input min="0" type="number" value={form.protein} onChange={(event) => updateField("protein", event.target.value)} />
              </label>
              <label>
                Carbs
                <input min="0" type="number" value={form.carbs} onChange={(event) => updateField("carbs", event.target.value)} />
              </label>
              <label>
                Fat
                <input min="0" type="number" value={form.fat} onChange={(event) => updateField("fat", event.target.value)} />
              </label>
            </div>

            <div className={styles.actions}>
              <Link className={styles.textButton} href={`/add?date=${date}`}>
                Cancel
              </Link>
              <button disabled={!canSave} type="submit">
                Save Food
              </button>
            </div>
          </form>
        </section>
      </div>
    </main>
  );
}
