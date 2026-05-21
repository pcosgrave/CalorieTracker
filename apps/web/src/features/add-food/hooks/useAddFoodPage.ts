import { useCallback, useEffect, useMemo, useState } from "react";
import {
  deleteFoodProduct,
  deleteRecipeProduct,
  readFoodProducts,
  readRecipeProducts,
  todayDateKey,
  toNumber,
  writeDiaryEntry,
} from "@/app/lib/diary";
import { catalogItems, hiddenSeedStorageKey } from "../catalog";
import type { CatalogItem, CatalogKind, SortMode } from "../types";
import { dateFromUrl, defaultLogState, itemServingUnit, makeProductFromItem, productToCatalogItem } from "../utils";

export function useAddFoodPage() {
  const [savedFoods, setSavedFoods] = useState<CatalogItem[]>([]);
  const [hiddenSeedIds, setHiddenSeedIds] = useState<string[]>([]);
  const [date, setDate] = useState(todayDateKey());
  const [activeTab, setActiveTab] = useState<CatalogKind>("ingredient");
  const [sortMode, setSortMode] = useState<SortMode>("recent");
  const [search, setSearch] = useState("");
  const [barcode, setBarcode] = useState("");
  const [showScanner, setShowScanner] = useState(false);
  const [selectedItem, setSelectedItem] = useState<CatalogItem | null>(null);
  const [logState, setLogState] = useState(() => defaultLogState(todayDateKey()));
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

  return {
    activeTab,
    barcode,
    date,
    logState,
    openMenuId,
    results,
    savedEntryName,
    search,
    selectedItem,
    showScanner,
    sortMenuOpen,
    sortMode,
    deleteOption,
    editHref,
    logSelectedItem,
    selectItem,
    setActiveTab,
    setBarcode,
    setLogState,
    setOpenMenuId,
    setSearch,
    setSelectedItem,
    setShowScanner,
    setSortMenuOpen,
    setSortMode,
  };
}
