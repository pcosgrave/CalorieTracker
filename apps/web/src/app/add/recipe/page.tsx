"use client";

import Link from "next/link";
import { FormEvent, useEffect, useMemo, useState } from "react";
import type { FoodProduct, Nutrients } from "@calorie-tracker/shared";
import {
  createId,
  ownerUserId,
  readFoodProducts,
  readRecipeProducts,
  recipeDraftStorageKey,
  round,
  scaleNutrients,
  toNumber,
  todayDateKey,
  writeRecipeProduct,
} from "../../lib/diary";
import styles from "../../page.module.css";

type RecipeOption = {
  id: string;
  kind: "ingredient" | "recipe";
  name: string;
  brand?: string;
  servingLabel: string;
  servingQuantity: number;
  servingUnit: string;
  nutrients: Nutrients;
  components?: RecipeComponent[];
};

type RecipeComponent = {
  item: RecipeOption;
  amount: string;
  unit: string;
};

type RecipeDraft = {
  name: string;
  brand: string;
  servingQuantity: string;
  servingUnit: string;
  components: RecipeComponent[];
};

type StoredRecipeProduct = FoodProduct & {
  recipeComponents?: RecipeComponent[];
};

const parentRecipeDraftStorageKey = "calorie-tracker:recipe-parent-draft:v1";

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
  "serving",
  "tbsp",
  "tsp",
];

function unitOptions(...units: string[]): string[] {
  return [...new Set([...units.filter((unit) => unit.trim().length > 0), ...measurementUnits])];
}

const conversionGroups: Record<string, Record<string, number>> = {
  volume: {
    tsp: 1,
    tbsp: 3,
    "fl oz": 6,
    cup: 48,
    pint: 96,
    quart: 192,
    ml: 0.202884,
    liter: 202.884,
  },
  weight: {
    milligram: 0.001,
    gram: 1,
    kg: 1000,
    oz: 28.3495,
    lb: 453.592,
  },
};

const seedOptions: RecipeOption[] = [
  {
    id: "ingredient-coffee",
    kind: "ingredient",
    name: "Coffee",
    servingLabel: "16 oz",
    servingQuantity: 16,
    servingUnit: "oz",
    nutrients: { calories: 9, proteinGrams: 0.5, carbohydrateGrams: 0, fatGrams: 0 },
  },
  {
    id: "ingredient-greek-yogurt",
    kind: "ingredient",
    name: "Greek yogurt",
    brand: "Plain",
    servingLabel: "1 cup",
    servingQuantity: 1,
    servingUnit: "cup",
    nutrients: { calories: 140, proteinGrams: 20, carbohydrateGrams: 8, fatGrams: 3 },
  },
  {
    id: "ingredient-banana",
    kind: "ingredient",
    name: "Banana",
    servingLabel: "1 medium",
    servingQuantity: 1,
    servingUnit: "banana",
    nutrients: { calories: 105, proteinGrams: 1.3, carbohydrateGrams: 27, fatGrams: 0.4 },
  },
  {
    id: "ingredient-oats",
    kind: "ingredient",
    name: "Rolled oats",
    servingLabel: "1/2 cup",
    servingQuantity: 0.5,
    servingUnit: "cup",
    nutrients: { calories: 150, proteinGrams: 5, carbohydrateGrams: 27, fatGrams: 3 },
  },
  {
    id: "recipe-yogurt-bowl",
    kind: "recipe",
    name: "Yogurt banana bowl",
    servingLabel: "1 bowl",
    servingQuantity: 1,
    servingUnit: "bowl",
    nutrients: { calories: 395, proteinGrams: 26, carbohydrateGrams: 62, fatGrams: 6.4 },
    components: [
      {
        item: {
          id: "ingredient-greek-yogurt",
          kind: "ingredient",
          name: "Greek yogurt",
          servingLabel: "1 cup",
          servingQuantity: 1,
          servingUnit: "cup",
          nutrients: { calories: 140, proteinGrams: 20, carbohydrateGrams: 8, fatGrams: 3 },
        },
        amount: "1",
        unit: "cup",
      },
      {
        item: {
          id: "ingredient-banana",
          kind: "ingredient",
          name: "Banana",
          servingLabel: "1 medium",
          servingQuantity: 1,
          servingUnit: "banana",
          nutrients: { calories: 105, proteinGrams: 1.3, carbohydrateGrams: 27, fatGrams: 0.4 },
        },
        amount: "1",
        unit: "banana",
      },
      {
        item: {
          id: "ingredient-oats",
          kind: "ingredient",
          name: "Rolled oats",
          servingLabel: "1/2 cup",
          servingQuantity: 0.5,
          servingUnit: "cup",
          nutrients: { calories: 150, proteinGrams: 5, carbohydrateGrams: 27, fatGrams: 3 },
        },
        amount: "0.5",
        unit: "cup",
      },
    ],
  },
  {
    id: "recipe-breakfast-wrap",
    kind: "recipe",
    name: "Breakfast egg wrap",
    servingLabel: "1 wrap",
    servingQuantity: 1,
    servingUnit: "wrap",
    nutrients: { calories: 430, proteinGrams: 25, carbohydrateGrams: 38, fatGrams: 19 },
    components: [
      {
        item: {
          id: "ingredient-eggs",
          kind: "ingredient",
          name: "Eggs",
          servingLabel: "2 eggs",
          servingQuantity: 2,
          servingUnit: "egg",
          nutrients: { calories: 140, proteinGrams: 12, carbohydrateGrams: 1, fatGrams: 10 },
        },
        amount: "2",
        unit: "egg",
      },
      {
        item: {
          id: "ingredient-tortilla",
          kind: "ingredient",
          name: "Tortilla",
          servingLabel: "1 wrap",
          servingQuantity: 1,
          servingUnit: "wrap",
          nutrients: { calories: 180, proteinGrams: 5, carbohydrateGrams: 30, fatGrams: 4 },
        },
        amount: "1",
        unit: "wrap",
      },
    ],
  },
];

function emptyDraft(): RecipeDraft {
  return {
    name: "",
    brand: "",
    servingQuantity: "1",
    servingUnit: "serving",
    components: [],
  };
}

function productToOption(product: FoodProduct, kind: "ingredient" | "recipe"): RecipeOption {
  const recipeProduct = product as StoredRecipeProduct;
  const option: RecipeOption = {
    id: product.productId,
    kind,
    name: product.name,
    servingLabel: product.serving.label,
    servingQuantity: product.serving.quantity,
    servingUnit: product.serving.unit,
    nutrients: product.nutrients,
  };

  if (recipeProduct.recipeComponents?.length) {
    option.components = recipeProduct.recipeComponents;
  }

  if (product.brand) {
    option.brand = product.brand;
  }

  return option;
}

function productToComponent(product: StoredRecipeProduct): RecipeComponent {
  return {
    item: productToOption(product, "recipe"),
    amount: String(product.serving.quantity),
    unit: product.serving.unit,
  };
}

function readDraft(): RecipeDraft {
  try {
    const stored = window.localStorage.getItem(recipeDraftStorageKey);
    return stored ? { ...emptyDraft(), ...JSON.parse(stored) } : emptyDraft();
  } catch {
    return emptyDraft();
  }
}

function convertAmount(amount: number, fromUnit: string, toUnit: string): number | null {
  if (fromUnit === toUnit) {
    return amount;
  }

  const group = Object.values(conversionGroups).find((units) => fromUnit in units && toUnit in units);
  if (!group) {
    return null;
  }

  const fromRatio = group[fromUnit];
  const toRatio = group[toUnit];
  if (fromRatio === undefined || toRatio === undefined) {
    return null;
  }

  return (amount * fromRatio) / toRatio;
}

function totalComponents(components: RecipeComponent[]): Nutrients {
  return components.reduce(
    (total, component) => {
      const amount = Math.max(toNumber(component.amount), 0);
      const convertedAmount = convertAmount(amount, component.unit, component.item.servingUnit) ?? amount;
      const multiplier = convertedAmount / component.item.servingQuantity;
      const nutrients = scaleNutrients(component.item.nutrients, multiplier);

      return {
        calories: round(total.calories + nutrients.calories),
        proteinGrams: round(total.proteinGrams + nutrients.proteinGrams),
        carbohydrateGrams: round(total.carbohydrateGrams + nutrients.carbohydrateGrams),
        fatGrams: round(total.fatGrams + nutrients.fatGrams),
      };
    },
    { calories: 0, proteinGrams: 0, carbohydrateGrams: 0, fatGrams: 0 },
  );
}

export default function AddRecipePage() {
  const [draft, setDraft] = useState<RecipeDraft>(emptyDraft);
  const [search, setSearch] = useState("");
  const [options, setOptions] = useState<RecipeOption[]>(seedOptions);
  const [date, setDate] = useState(todayDateKey());
  const [editId, setEditId] = useState("");
  const [returnToParentDraft, setReturnToParentDraft] = useState(false);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const shouldReturnToParentDraft = params.get("parentDraft") === "1";
    const recipeId = params.get("edit") || "";
    const existingRecipe = recipeId
      ? (readRecipeProducts().find((product) => product.productId === recipeId) as StoredRecipeProduct | undefined)
      : undefined;

    setDate(params.get("date") || todayDateKey());
    setEditId(recipeId);
    setReturnToParentDraft(shouldReturnToParentDraft);
    setDraft(() => {
      const storedDraft = readDraft();
      const hasExistingRecipe = existingRecipe !== undefined && existingRecipe.productId === recipeId;
      return {
        ...storedDraft,
        name: hasExistingRecipe ? existingRecipe.name : (params.get("name") || storedDraft.name),
        brand: hasExistingRecipe ? existingRecipe.brand || "" : (params.get("brand") || storedDraft.brand),
        servingQuantity: existingRecipe ? String(existingRecipe.serving.quantity) : (params.get("servingQuantity") || storedDraft.servingQuantity),
        servingUnit: existingRecipe?.serving.unit || params.get("servingUnit") || storedDraft.servingUnit,
        components: existingRecipe?.recipeComponents || storedDraft.components,
      };
    });

    // Get all recipe products except the one being edited
    const editableRecipeProducts = recipeId
      ? readRecipeProducts().filter((product) => product.productId !== recipeId)
      : readRecipeProducts();

    const recipeOptions = editableRecipeProducts.map((product) => {
      // For the recipe being edited, use the full product data with components
      if (recipeId && product.productId === recipeId && existingRecipe) {
        return productToOption(existingRecipe, "recipe");
      }
      return productToOption(product, "recipe");
    });

    setOptions([
      ...readFoodProducts().map((product) => productToOption(product, "ingredient")),
      ...recipeOptions,
      ...seedOptions,
    ]);
  }, []);

  useEffect(() => {
    window.localStorage.setItem(recipeDraftStorageKey, JSON.stringify(draft));
  }, [draft]);

  const results = useMemo(() => {
    const normalized = search.trim().toLowerCase();
    if (!normalized) {
      return options;
    }

    return options.filter(
      (option) =>
        option.name.toLowerCase().includes(normalized) ||
        option.brand?.toLowerCase().includes(normalized) ||
        option.kind.toLowerCase().includes(normalized) ||
        option.components?.some((component) => component.item.name.toLowerCase().includes(normalized)),
    );
  }, [options, search]);

  const totals = totalComponents(draft.components);
  const canSave = draft.name.trim().length > 0 && draft.components.length > 0;

  function updateDraft(field: keyof Omit<RecipeDraft, "components">, value: string): void {
    setDraft((current) => ({ ...current, [field]: value }));
  }

  function addComponent(item: RecipeOption): void {
    // When adding a recipe that's already in the draft being edited, add it as a component
    // This allows using an existing recipe within a parent recipe
    setDraft((current) => ({
      ...current,
      components: [...current.components, { item, amount: String(item.servingQuantity), unit: item.servingUnit }],
    }));
    setSearch("");
  }

  function updateComponent(index: number, updates: Partial<Pick<RecipeComponent, "amount" | "unit">>): void {
    setDraft((current) => ({
      ...current,
      components: current.components.map((component, componentIndex) => (componentIndex === index ? { ...component, ...updates } : component)),
    }));
  }

  function removeComponent(index: number): void {
    setDraft((current) => ({
      ...current,
      components: current.components.filter((_, componentIndex) => componentIndex !== index),
    }));
  }

  function saveRecipe(event: FormEvent<HTMLFormElement>): void {
    event.preventDefault();

    const now = new Date().toISOString();
    const servingQuantity = Math.max(toNumber(draft.servingQuantity), 0.1);
    const product: StoredRecipeProduct = {
      productId: editId || createId("recipe"),
      ownerUserId,
      visibility: "private",
      name: draft.name.trim(),
      brand: draft.brand.trim(),
      serving: {
        label: `${servingQuantity} ${draft.servingUnit}`,
        quantity: servingQuantity,
        unit: draft.servingUnit,
      },
      nutrients: totals,
      recipeComponents: draft.components,
      createdAt: now,
      updatedAt: now,
    };

    writeRecipeProduct(product);
    window.localStorage.removeItem(recipeDraftStorageKey);

    if (returnToParentDraft) {
      const parentDraft = window.localStorage.getItem(parentRecipeDraftStorageKey);
      if (parentDraft) {
        const restoredDraft = {
          ...emptyDraft(),
          ...JSON.parse(parentDraft),
        } as RecipeDraft;
        const nextDraft: RecipeDraft = {
          ...restoredDraft,
          components: [...restoredDraft.components, productToComponent(product)],
        };

        window.localStorage.setItem(recipeDraftStorageKey, JSON.stringify(nextDraft));
        window.localStorage.removeItem(parentRecipeDraftStorageKey);
        window.location.href = `/add/recipe?date=${date}`;
        return;
      }
    }

    window.location.href = `/add?date=${date}`;
  }

  function preserveDraft(): void {
    window.localStorage.setItem(recipeDraftStorageKey, JSON.stringify(draft));
  }

  function preserveParentDraft(): void {
    window.localStorage.setItem(parentRecipeDraftStorageKey, JSON.stringify(draft));
    window.localStorage.removeItem(recipeDraftStorageKey);
  }

  function startNestedRecipe(): void {
    preserveParentDraft();
    window.location.href = `/add/recipe?date=${date}&name=${encodeURIComponent(search.trim())}&parentDraft=1`;
  }

  function componentSummary(component: RecipeComponent): string {
    return `${component.item.name} - ${component.amount} ${component.unit}`;
  }

  function FoodTypeIcon({ kind }: { kind: RecipeOption["kind"] }) {
    if (kind === "recipe") {
      return (
        <svg aria-hidden="true" viewBox="0 0 24 24">
          <path d="M5 12h12a5 5 0 0 1-5 5h-2a5 5 0 0 1-5-5Z" />
          <path d="M4 12h14" />
          <path d="M18 5v12" />
          <path d="M16 5h4" />
        </svg>
      );
    }

    return (
      <svg aria-hidden="true" viewBox="0 0 24 24">
        <path d="M6 20 18 8" />
        <path d="m14 4 6 6" />
        <path d="M4 18 6 20 3 21Z" />
      </svg>
    );
  }

  return (
    <main className={styles.page}>
      <div className={styles.shell}>
        <header className={styles.topbar}>
          <div className={styles.brand}>
            <h1>Add Recipe</h1>
            <p>Build a reusable recipe from ingredients or existing recipes.</p>
          </div>
          <Link className={styles.textButton} href={`/add?date=${date}`}>
            Back to search
          </Link>
        </header>

        <section className={styles.addGrid}>
          <form className={styles.panel} onSubmit={saveRecipe}>
            <div className={styles.twoColumn}>
              <label>
                Recipe name
                <input required value={draft.name} onChange={(event) => updateDraft("name", event.target.value)} />
              </label>
              <label>
                Brand
                <input value={draft.brand} onChange={(event) => updateDraft("brand", event.target.value)} />
              </label>
            </div>
            <div className={styles.twoColumn}>
              <label>
                Serving size
                <input
                  min="0.1"
                  required
                  step="0.1"
                  type="number"
                  value={draft.servingQuantity}
                  onChange={(event) => updateDraft("servingQuantity", event.target.value)}
                />
              </label>
              <label>
                Unit
                <select value={draft.servingUnit} onChange={(event) => updateDraft("servingUnit", event.target.value)}>
                  {unitOptions(draft.servingUnit).map((unit) => (
                    <option key={unit} value={unit}>
                      {unit}
                    </option>
                  ))}
                </select>
              </label>
            </div>

            <div className={styles.mealHeader}>
              <div>
                <h2>Recipe items</h2>
                <p className={styles.subtle}>
                  {totals.calories} cal - {totals.proteinGrams}g protein - {totals.carbohydrateGrams}g carbs - {totals.fatGrams}g fat
                </p>
              </div>
            </div>

            <div className={styles.resultList}>
              {draft.components.length === 0 ? (
                <p className={styles.empty}>No items added yet.</p>
              ) : (
                draft.components.map((component, index) => (
                  <article className={styles.foodResult} key={`${component.item.id}-${index}`}>
                    <span className={styles.foodIdentity}>
                      <span className={styles.foodTypeIcon} aria-hidden="true">
                        <FoodTypeIcon kind={component.item.kind} />
                      </span>
                      <span className={styles.foodText}>
                        <strong>{component.item.name}</strong>
                        <small>{component.item.servingLabel}</small>
                        {component.item.components?.length ? (
                          <small className={styles.componentList}>{component.item.components.map(componentSummary).join(", ")}</small>
                        ) : null}
                      </span>
                    </span>
                    <div className={styles.optionActions}>
                      <input
                        aria-label={`Serving amount for ${component.item.name}`}
                        min="0.1"
                        step="0.1"
                        type="number"
                        value={component.amount}
                        onChange={(event) => updateComponent(index, { amount: event.target.value })}
                      />
                      <select
                        aria-label={`Serving unit for ${component.item.name}`}
                        value={component.unit}
                        onChange={(event) => updateComponent(index, { unit: event.target.value })}
                      >
                        {unitOptions(component.unit, component.item.servingUnit).map((unit) => (
                          <option key={unit} value={unit}>
                            {unit}
                          </option>
                        ))}
                      </select>
                      <button className="secondary" type="button" onClick={() => removeComponent(index)}>
                        Remove
                      </button>
                    </div>
                  </article>
                ))
              )}
            </div>

            <div className={styles.actions}>
              <button disabled={!canSave} type="submit">
                Save recipe
              </button>
            </div>
          </form>

          <section className={styles.panel}>
            <div className={styles.panelHeader}>
              <h2>Search foods</h2>
              <div className={styles.headerActions}>
                <Link
                  className={styles.textButton}
                  href={`/add/new?name=${encodeURIComponent(search.trim())}&returnTo=${encodeURIComponent("/add/recipe")}`}
                  onClick={preserveDraft}
                >
                  Add ingredient
                </Link>
                <button className={styles.textButton} type="button" onClick={startNestedRecipe}>
                  Add recipe
                </button>
              </div>
            </div>
            <div className={styles.searchWrap}>
              <input value={search} placeholder="Search ingredients or recipes" onChange={(event) => setSearch(event.target.value)} />
              {search ? (
                <button className={styles.clearSearch} type="button" onClick={() => setSearch("")}>
                  x
                </button>
              ) : null}
            </div>
            <div className={styles.resultList}>
              {results.length === 0 ? (
                <div className={styles.emptyAction}>
                  <p>No matching foods.</p>
                  <div className={styles.emptyActionButtons}>
                    <Link
                      className={styles.textButton}
                      href={`/add/new?name=${encodeURIComponent(search.trim())}&returnTo=${encodeURIComponent("/add/recipe")}`}
                      onClick={preserveDraft}
                    >
                      Add ingredient
                    </Link>
                    <button className={styles.textButton} type="button" onClick={startNestedRecipe}>
                      Add recipe
                    </button>
                  </div>
                </div>
              ) : (
                results.map((item) => (
                  <button className={styles.foodResult} key={item.id} type="button" onClick={() => addComponent(item)}>
                    <span className={styles.foodIdentity}>
                      <span className={styles.foodTypeIcon} aria-hidden="true">
                        <FoodTypeIcon kind={item.kind} />
                      </span>
                      <span className={styles.foodText}>
                        <strong>{item.name}</strong>
                        <small>
                          {item.brand ? `${item.brand} - ` : ""}
                          {item.servingQuantity} {item.servingUnit}
                        </small>
                        {item.components?.length ? (
                          <small className={styles.componentList}>{item.components.map(componentSummary).join(", ")}</small>
                        ) : null}
                      </span>
                    </span>
                    <small className={styles.servingBadge}>{item.servingLabel}</small>
                  </button>
                ))
              )}
            </div>
          </section>
        </section>
      </div>
    </main>
  );
}
