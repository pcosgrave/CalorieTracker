"use client";

import Link from "next/link";
import { useCallback, useEffect, useMemo, useState } from "react";
import type { FoodProduct, MealType, Nutrients } from "@calorie-tracker/shared";
import {
  createId,
  deleteFoodProduct,
  deleteRecipeProduct,
  formatDateHeading,
  mealLabels,
  mealOrder,
  ownerUserId,
  readFoodProducts,
  readRecipeProducts,
  round,
  scaleNutrients,
  shiftDateKey,
  todayDateKey,
  toNumber,
  writeDiaryEntry,
} from "../lib/diary";
import styles from "../page.module.css";

type CatalogKind = "ingredient" | "recipe";
type SortMode = "alphabetical" | "frequency" | "recent";

type CatalogItem = {
  id: string;
  kind: CatalogKind;
  name: string;
  brand?: string;
  barcode?: string;
  servingLabel: string;
  servingUnit: string;
  servingQuantity: number;
  servingGrams?: number;
  nutrients: Nutrients;
  ingredients?: string[];
  frequency: number;
  lastUsedDaysAgo: number;
};

type RecipeComponent = {
  item: {
    name: string;
    servingLabel: string;
  };
  amount: string;
  unit: string;
};

type StoredRecipeProduct = FoodProduct & {
  recipeComponents?: RecipeComponent[];
};

type LogState = {
  date: string;
  meal: MealType;
  servingAmount: string;
  servingUnit: string;
};

const catalogItems: CatalogItem[] = [
  {
    id: "ingredient-coffee",
    kind: "ingredient",
    name: "Coffee",
    servingLabel: "16 oz",
    servingQuantity: 16,
    servingUnit: "oz",
    nutrients: { calories: 9, proteinGrams: 0.5, carbohydrateGrams: 0, fatGrams: 0 },
    frequency: 18,
    lastUsedDaysAgo: 0,
  },
  {
    id: "ingredient-greek-yogurt",
    kind: "ingredient",
    name: "Greek yogurt",
    brand: "Plain",
    barcode: "012345678905",
    servingLabel: "1 cup",
    servingQuantity: 1,
    servingUnit: "cup",
    servingGrams: 227,
    nutrients: { calories: 140, proteinGrams: 20, carbohydrateGrams: 8, fatGrams: 3 },
    frequency: 14,
    lastUsedDaysAgo: 1,
  },
  {
    id: "ingredient-banana",
    kind: "ingredient",
    name: "Banana",
    servingLabel: "1 medium",
    servingQuantity: 1,
    servingUnit: "banana",
    servingGrams: 118,
    nutrients: { calories: 105, proteinGrams: 1.3, carbohydrateGrams: 27, fatGrams: 0.4 },
    frequency: 10,
    lastUsedDaysAgo: 2,
  },
  {
    id: "ingredient-oats",
    kind: "ingredient",
    name: "Rolled oats",
    servingLabel: "1/2 cup",
    servingQuantity: 0.5,
    servingUnit: "cup",
    servingGrams: 40,
    nutrients: { calories: 150, proteinGrams: 5, carbohydrateGrams: 27, fatGrams: 3 },
    frequency: 7,
    lastUsedDaysAgo: 4,
  },
  {
    id: "recipe-yogurt-bowl",
    kind: "recipe",
    name: "Yogurt banana bowl",
    servingLabel: "1 bowl",
    servingQuantity: 1,
    servingUnit: "bowl",
    nutrients: { calories: 395, proteinGrams: 26, carbohydrateGrams: 62, fatGrams: 6.4 },
    ingredients: ["Greek yogurt - 1 cup", "Banana - 1 medium", "Rolled oats - 1/2 cup"],
    frequency: 8,
    lastUsedDaysAgo: 1,
  },
  {
    id: "recipe-breakfast-wrap",
    kind: "recipe",
    name: "Breakfast egg wrap",
    servingLabel: "1 wrap",
    servingQuantity: 1,
    servingUnit: "wrap",
    nutrients: { calories: 430, proteinGrams: 25, carbohydrateGrams: 38, fatGrams: 19 },
    ingredients: ["Eggs - 2 eggs", "Tortilla - 1 wrap", "Cheddar - 1 oz", "Spinach - 1 cup"],
    frequency: 5,
    lastUsedDaysAgo: 6,
  },
];

const hiddenSeedStorageKey = "calorie-tracker:hidden-seed-options:v1";

function dateFromUrl(): string {
  const params = new URLSearchParams(window.location.search);
  return params.get("date") || todayDateKey();
}

function defaultLogState(date: string): LogState {
  return {
    date,
    meal: "breakfast",
    servingAmount: "1",
    servingUnit: "serving",
  };
}

function itemServingUnit(item: CatalogItem): string {
  return item.servingUnit || "serving";
}

function makeProductFromItem(item: CatalogItem, now: string, nutrients: Nutrients): FoodProduct {
  return {
    productId: createId(item.kind),
    ownerUserId,
    visibility: "private",
    barcode: item.barcode,
    name: item.name,
    brand: item.brand,
    serving: {
      label: item.servingLabel,
      quantity: item.servingQuantity,
      unit: itemServingUnit(item),
      grams: item.servingGrams,
    },
    nutrients,
    createdAt: now,
    updatedAt: now,
  };
}

function productToCatalogItem(product: FoodProduct, kind: CatalogKind): CatalogItem {
  const recipeProduct = product as StoredRecipeProduct;
  const item: CatalogItem = {
    id: product.productId,
    kind,
    name: product.name,
    servingLabel: product.serving.label,
    servingQuantity: product.serving.quantity,
    servingUnit: product.serving.unit,
    nutrients: product.nutrients,
    frequency: 0,
    lastUsedDaysAgo: 0,
  };

  if (product.brand) {
    item.brand = product.brand;
  }

  if (product.barcode) {
    item.barcode = product.barcode;
  }

  if (product.serving.grams) {
    item.servingGrams = product.serving.grams;
  }

  if (recipeProduct.recipeComponents?.length) {
    item.ingredients = recipeProduct.recipeComponents.map((component) => `${component.item.name} - ${component.amount} ${component.unit}`);
  }

  return item;
}

function splitIngredientLabel(ingredient: string): { name: string; serving?: string } {
  const [name, ...servingParts] = ingredient.split(" - ");
  const serving = servingParts.join(" - ");

  return {
    name: name || ingredient,
    ...(serving ? { serving } : {}),
  };
}

export default function AddFoodPage() {
  const [savedFoods, setSavedFoods] = useState<CatalogItem[]>([]);
  const [hiddenSeedIds, setHiddenSeedIds] = useState<string[]>([]);
  const [date, setDate] = useState(todayDateKey());
  const [activeTab, setActiveTab] = useState<CatalogKind>("ingredient");
  const [sortMode, setSortMode] = useState<SortMode>("recent");
  const [search, setSearch] = useState("");
  const [barcode, setBarcode] = useState("");
  const [showScanner, setShowScanner] = useState(false);
  const [selectedItem, setSelectedItem] = useState<CatalogItem | null>(null);
  const [logState, setLogState] = useState<LogState>(() => defaultLogState(todayDateKey()));
  const [savedEntryName, setSavedEntryName] = useState("");
  const [openMenuId, setOpenMenuId] = useState("");
  const [sortMenuOpen, setSortMenuOpen] = useState(false);

  const refreshSavedFoods = useCallback((): void => {
    setSavedFoods([
      ...readFoodProducts().map((product) => productToCatalogItem(product, "ingredient")),
      ...readRecipeProducts().map((product) => productToCatalogItem(product, "recipe")),
    ]);
  }, []);

  useEffect(() => {
    const initialDate = dateFromUrl();
    setDate(initialDate);
    setLogState(defaultLogState(initialDate));
    setHiddenSeedIds(JSON.parse(window.localStorage.getItem(hiddenSeedStorageKey) || "[]"));
    refreshSavedFoods();
  }, [refreshSavedFoods]);

  function deleteOption(item: CatalogItem): void {
    const isSavedOption = savedFoods.some((saved) => saved.id === item.id);

    if (!isSavedOption) {
      const nextHiddenIds = [...hiddenSeedIds, item.id];
      setHiddenSeedIds(nextHiddenIds);
      window.localStorage.setItem(hiddenSeedStorageKey, JSON.stringify(nextHiddenIds));
      return;
    }

    if (item.kind === "recipe") {
      deleteRecipeProduct(item.id);
    } else {
      deleteFoodProduct(item.id);
    }

    refreshSavedFoods();
  }

  function editHref(item: CatalogItem): string {
    const params = new URLSearchParams({
      date,
      name: item.name,
      brand: item.brand || "",
      barcode: item.barcode || "",
      servingQuantity: String(item.servingQuantity),
      servingUnit: item.servingUnit,
      calories: String(item.nutrients.calories),
      protein: String(item.nutrients.proteinGrams),
      carbs: String(item.nutrients.carbohydrateGrams),
      fat: String(item.nutrients.fatGrams),
    });

    if (savedFoods.some((saved) => saved.id === item.id)) {
      params.set("edit", item.id);
    }

    return item.kind === "recipe" ? `/add/recipe?${params.toString()}` : `/add/new?${params.toString()}`;
  }

  const results = useMemo(() => {
    const normalizedSearch = search.trim().toLowerCase();
    const normalizedBarcode = barcode.trim();
    const isSearching = normalizedSearch.length > 0 || normalizedBarcode.length > 0;

    return [...savedFoods, ...catalogItems.filter((item) => !hiddenSeedIds.includes(item.id))]
      .filter((item) => {
        const matchesTab = isSearching || item.kind === activeTab;
        const matchesSearch =
          !normalizedSearch ||
          item.name.toLowerCase().includes(normalizedSearch) ||
          item.brand?.toLowerCase().includes(normalizedSearch) ||
          item.ingredients?.some((ingredient) => ingredient.toLowerCase().includes(normalizedSearch));
        const matchesBarcode = !normalizedBarcode || item.barcode?.includes(normalizedBarcode);

        return matchesTab && matchesSearch && matchesBarcode;
      })
      .sort((a, b) => {
        if (sortMode === "frequency") {
          return b.frequency - a.frequency || a.name.localeCompare(b.name);
        }

        if (sortMode === "recent") {
          return a.lastUsedDaysAgo - b.lastUsedDaysAgo || a.name.localeCompare(b.name);
        }

        return a.name.localeCompare(b.name);
      });
  }, [activeTab, barcode, hiddenSeedIds, savedFoods, search, sortMode]);

  function selectItem(item: CatalogItem): void {
    setSelectedItem(item);
    setLogState({
      date,
      meal: "breakfast",
      servingAmount: String(item.servingQuantity),
      servingUnit: itemServingUnit(item),
    });
    setSavedEntryName("");
  }

  function logSelectedItem(addMore: boolean): void {
    if (!selectedItem) {
      return;
    }

    const servingAmount = Math.max(toNumber(logState.servingAmount), 0.1);
    const multiplier = servingAmount / selectedItem.servingQuantity;
    const now = new Date().toISOString();
    const product = makeProductFromItem(selectedItem, now, selectedItem.nutrients);

    writeDiaryEntry(product, logState.date, logState.meal, multiplier);
    setSavedEntryName(selectedItem.name);

    if (addMore) {
      setSelectedItem(null);
      setSearch("");
      setBarcode("");
    } else {
      window.location.href = `/?date=${logState.date}`;
    }
  }

  function FoodTypeIcon({ kind }: { kind: CatalogKind }) {
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

  if (selectedItem) {
    const servingAmount = Math.max(toNumber(logState.servingAmount), 0.1);
    const multiplier = servingAmount / selectedItem.servingQuantity;
    const adjustedNutrients = scaleNutrients(selectedItem.nutrients, multiplier);

    return (
      <main className={styles.compactPage}>
        <header className={styles.mobileTopbar}>
          <button aria-label="Back to search" className={styles.backButton} type="button" onClick={() => setSelectedItem(null)}>
            &lt;
          </button>
          <h1>Add food</h1>
        </header>

        <section className={styles.foodDetail}>
          <h2>{selectedItem.name}</h2>
          <p className={styles.nutritionLabel}>Nutrition facts</p>

          <div className={styles.servingRow}>
            <span>Serving size</span>
            <input
              aria-label="Serving amount"
              min="0.1"
              step="0.1"
              type="number"
              value={logState.servingAmount}
              onChange={(event) => setLogState((current) => ({ ...current, servingAmount: event.target.value }))}
            />
            <select
              aria-label="Serving unit"
              value={logState.servingUnit}
              onChange={(event) => setLogState((current) => ({ ...current, servingUnit: event.target.value }))}
            >
              <option value={itemServingUnit(selectedItem)}>{itemServingUnit(selectedItem)}</option>
              <option value="serving">serving</option>
            </select>
            <strong>{adjustedNutrients.calories} cals.</strong>
          </div>

          <div className={styles.choiceBlock}>
            <h3>Meal & Snacks Time</h3>
            <div className={styles.mealChoiceGrid}>
              {mealOrder.map((meal) => (
                <label className={styles.radioLabel} key={meal}>
                  <input
                    checked={logState.meal === meal}
                    name="meal"
                    type="radio"
                    value={meal}
                    onChange={() => setLogState((current) => ({ ...current, meal }))}
                  />
                  {mealLabels[meal]}
                </label>
              ))}
            </div>
          </div>

          {selectedItem.ingredients ? (
            <div className={styles.recipeIngredients}>
              <strong>Recipe ingredients</strong>
              <ul>
                {selectedItem.ingredients.map((ingredient) => {
                  const label = splitIngredientLabel(ingredient);

                  return (
                    <li key={ingredient}>
                      <span>{label.name}</span>
                      {label.serving ? <small>Serving: {label.serving}</small> : null}
                    </li>
                  );
                })}
              </ul>
            </div>
          ) : null}

          <div className={styles.dayPickRow}>
            <strong>Day</strong>
            <div className={styles.dayStepper}>
              <button className="secondary" type="button" onClick={() => setLogState((current) => ({ ...current, date: shiftDateKey(current.date, -1) }))}>
                &lt;
              </button>
              <input
                aria-label="Log date"
                type="date"
                value={logState.date}
                onChange={(event) => setLogState((current) => ({ ...current, date: event.target.value }))}
              />
              <button className="secondary" type="button" onClick={() => setLogState((current) => ({ ...current, date: shiftDateKey(current.date, 1) }))}>
                &gt;
              </button>
            </div>
          </div>

          <div className={styles.bottomActions}>
            <button className="secondary" type="button" onClick={() => logSelectedItem(true)}>
              Log & add more
            </button>
            <button type="button" onClick={() => logSelectedItem(false)}>
              Log this
            </button>
          </div>
        </section>
      </main>
    );
  }

  return (
    <main className={styles.page}>
      <div className={styles.shell}>
        <header className={styles.topbar}>
          <div className={styles.brand}>
            <h1>Add Food</h1>
            <p>
              {formatDateHeading(date)} - {date}
            </p>
          </div>
          <Link className={styles.textButton} href={`/?date=${date}`}>
            Back to diary
          </Link>
        </header>

        {savedEntryName ? <p className={styles.success}>Saved {savedEntryName}.</p> : null}

        <section className={styles.addGrid}>
          <div className={styles.panel}>
            <div className={styles.panelHeader}>
              <h2>Search foods</h2>
              <div className={styles.headerActions}>
                <Link aria-label="Quick calories" className={styles.iconLink} href={`/add/quick?date=${date}`}>
                  +
                </Link>
                <button className="secondary" type="button" onClick={() => setShowScanner((current) => !current)}>
                  Barcode
                </button>
              </div>
            </div>

            {showScanner ? (
              <div className={styles.scannerPanel}>
                <div className={styles.scannerFrame}>
                  <span>Barcode scanner</span>
                </div>
                <input
                  inputMode="numeric"
                  placeholder="Scan or type barcode"
                  value={barcode}
                  onChange={(event) => setBarcode(event.target.value)}
                />
              </div>
            ) : null}

            <div className={styles.searchWrap}>
              <input
                aria-label="Search ingredients or recipes"
                placeholder="Search ingredients or recipes"
                value={search}
                onChange={(event) => setSearch(event.target.value)}
              />
              {search ? (
                <button aria-label="Clear search" className={styles.clearSearch} type="button" onClick={() => setSearch("")}>
                  x
                </button>
              ) : null}
            </div>

            <div className={styles.tabs} role="tablist" aria-label="Food item type">
              <button
                aria-selected={activeTab === "ingredient"}
                className={activeTab === "ingredient" ? styles.tabActive : styles.tab}
                role="tab"
                type="button"
                onClick={() => setActiveTab("ingredient")}
              >
                Ingredients
              </button>
              <button
                aria-selected={activeTab === "recipe"}
                className={activeTab === "recipe" ? styles.tabActive : styles.tab}
                role="tab"
                type="button"
                onClick={() => setActiveTab("recipe")}
              >
                Recipes
              </button>
            </div>

            <div className={styles.resultToolbar}>
              <div className={styles.contextMenu}>
                <button
                  aria-label={`Sort foods by ${sortMode}`}
                  className={styles.filterButton}
                  type="button"
                  onClick={() => setSortMenuOpen((current) => !current)}
                >
                  <svg aria-hidden="true" viewBox="0 0 24 24">
                    <path d="M4 6h16" />
                    <path d="M7 12h10" />
                    <path d="M10 18h4" />
                  </svg>
                </button>
                {sortMenuOpen ? (
                  <div className={styles.menuPanel}>
                    <button
                      type="button"
                      onClick={() => {
                        setSortMode("recent");
                        setSortMenuOpen(false);
                      }}
                    >
                      Recent
                    </button>
                    <button
                      type="button"
                      onClick={() => {
                        setSortMode("frequency");
                        setSortMenuOpen(false);
                      }}
                    >
                      Frequency
                    </button>
                    <button
                      type="button"
                      onClick={() => {
                        setSortMode("alphabetical");
                        setSortMenuOpen(false);
                      }}
                    >
                      Alphabetical
                    </button>
                  </div>
                ) : null}
              </div>
              <div className={styles.headerActions}>
                <Link
                  className={styles.textButton}
                  href={`/add/new?date=${date}&name=${encodeURIComponent(search.trim())}&barcode=${encodeURIComponent(barcode.trim())}`}
                >
                  Add ingredient
                </Link>
                <Link className={styles.textButton} href={`/add/recipe?date=${date}&name=${encodeURIComponent(search.trim())}`}>
                  Add recipe
                </Link>
              </div>
            </div>

            <div className={styles.resultList}>
              {results.length === 0 ? (
                <div className={styles.emptyAction}>
                  <p>No matching foods.</p>
                  <div className={styles.emptyActionButtons}>
                    <Link
                      className={styles.textButton}
                      href={`/add/new?date=${date}&name=${encodeURIComponent(search.trim())}&barcode=${encodeURIComponent(barcode.trim())}`}
                    >
                      Add ingredient
                    </Link>
                    <Link className={styles.textButton} href={`/add/recipe?name=${encodeURIComponent(search.trim())}`}>
                      Add recipe
                    </Link>
                  </div>
                </div>
              ) : (
                results.map((item) => (
                    <article className={styles.foodResult} key={item.id}>
                      <button className={styles.foodResultMain} type="button" onClick={() => selectItem(item)}>
                        <span className={styles.foodIdentity}>
                          <span className={styles.foodTypeIcon} aria-hidden="true">
                            <FoodTypeIcon kind={item.kind} />
                          </span>
                          <span className={styles.foodText}>
                            <strong>{item.name}</strong>
                            {item.brand ? <small>{item.brand}</small> : null}
                            {item.ingredients?.length ? <small className={styles.componentList}>{item.ingredients.join(", ")}</small> : null}
                          </span>
                        </span>
                        <span className={styles.foodMeta}>
                          <small>{item.servingLabel}</small>
                          <strong>{round(item.nutrients.calories)} cal</strong>
                        </span>
                      </button>
                      <div className={styles.contextMenu}>
                        <button
                          aria-label={`Options for ${item.name}`}
                          className={styles.menuButton}
                          type="button"
                          onClick={() => setOpenMenuId((current) => (current === item.id ? "" : item.id))}
                        >
                          ⋮
                        </button>
                        {openMenuId === item.id ? (
                          <div className={styles.menuPanel}>
                            <Link href={editHref(item)}>Edit</Link>
                            <button type="button" onClick={() => deleteOption(item)}>
                              Delete
                            </button>
                          </div>
                        ) : null}
                      </div>
                    </article>
                ))
              )}
            </div>
          </div>
        </section>
      </div>
    </main>
  );
}
