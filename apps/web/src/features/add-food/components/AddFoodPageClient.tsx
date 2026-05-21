"use client";

import { useEffect } from "react";
import { AddFoodSearchPage } from "./AddFoodSearchPage";
import { SelectedFoodDetail } from "./SelectedFoodDetail";
import { useAddFoodPage } from "../hooks/useAddFoodPage";
import { maybeAutoSync } from "@/lib/sync/service";

export function AddFoodPageClient() {
  useEffect(() => {
    void maybeAutoSync();
  }, []);

  const {
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
  } = useAddFoodPage();

  if (selectedItem) {
    return (
      <SelectedFoodDetail
        logState={logState}
        selectedItem={selectedItem}
        onBack={() => setSelectedItem(null)}
        onLog={logSelectedItem}
        onLogStateChange={setLogState}
      />
    );
  }

  return (
    <AddFoodSearchPage
      activeTab={activeTab}
      barcode={barcode}
      date={date}
      editHref={editHref}
      openMenuId={openMenuId}
      results={results}
      savedEntryName={savedEntryName}
      search={search}
      showScanner={showScanner}
      sortMenuOpen={sortMenuOpen}
      sortMode={sortMode}
      onBarcodeChange={setBarcode}
      onDeleteOption={deleteOption}
      onOpenMenuChange={setOpenMenuId}
      onSearchChange={setSearch}
      onSelectItem={selectItem}
      onShowScannerChange={setShowScanner}
      onSortMenuOpenChange={setSortMenuOpen}
      onSortModeChange={setSortMode}
      onTabChange={setActiveTab}
    />
  );
}
