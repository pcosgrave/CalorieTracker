import type { Dispatch, SetStateAction } from "react";
import Link from "next/link";
import styles from "@/app/page.module.css";
import { formatDateHeading, round } from "@/app/lib/diary";
import { FoodTypeIcon } from "./FoodTypeIcon";
import type { CatalogItem, CatalogKind, SortMode } from "../types";

type AddFoodSearchPageProps = {
  activeTab: CatalogKind;
  barcode: string;
  date: string;
  editHref: (item: CatalogItem) => string;
  openMenuId: string;
  results: CatalogItem[];
  savedEntryName: string;
  search: string;
  showScanner: boolean;
  sortMenuOpen: boolean;
  sortMode: SortMode;
  onBarcodeChange: (value: string) => void;
  onDeleteOption: (item: CatalogItem) => void;
  onOpenMenuChange: Dispatch<SetStateAction<string>>;
  onSearchChange: (value: string) => void;
  onSelectItem: (item: CatalogItem) => void;
  onShowScannerChange: Dispatch<SetStateAction<boolean>>;
  onSortMenuOpenChange: Dispatch<SetStateAction<boolean>>;
  onSortModeChange: (value: SortMode) => void;
  onTabChange: (value: CatalogKind) => void;
};

export function AddFoodSearchPage({
  activeTab,
  barcode,
  date,
  editHref,
  openMenuId,
  results,
  savedEntryName,
  search,
  showScanner,
  sortMenuOpen,
  sortMode,
  onBarcodeChange,
  onDeleteOption,
  onOpenMenuChange,
  onSearchChange,
  onSelectItem,
  onShowScannerChange,
  onSortMenuOpenChange,
  onSortModeChange,
  onTabChange,
}: AddFoodSearchPageProps) {
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
          <div className={styles.headerActions}>
            <Link className={styles.textButton} href="/settings">
              Sync
            </Link>
            <Link className={styles.textButton} href={`/?date=${date}`}>
              Back to diary
            </Link>
          </div>
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
                <button className="secondary" type="button" onClick={() => onShowScannerChange((current) => !current)}>
                  Barcode
                </button>
              </div>
            </div>

            {showScanner ? (
              <div className={styles.scannerPanel}>
                <div className={styles.scannerFrame}>
                  <span>Barcode scanner</span>
                </div>
                <input inputMode="numeric" placeholder="Scan or type barcode" value={barcode} onChange={(event) => onBarcodeChange(event.target.value)} />
              </div>
            ) : null}

            <div className={styles.searchWrap}>
              <input
                aria-label="Search ingredients or recipes"
                placeholder="Search ingredients or recipes"
                value={search}
                onChange={(event) => onSearchChange(event.target.value)}
              />
              {search ? (
                <button aria-label="Clear search" className={styles.clearSearch} type="button" onClick={() => onSearchChange("")}>
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
                onClick={() => onTabChange("ingredient")}
              >
                Ingredients
              </button>
              <button
                aria-selected={activeTab === "recipe"}
                className={activeTab === "recipe" ? styles.tabActive : styles.tab}
                role="tab"
                type="button"
                onClick={() => onTabChange("recipe")}
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
                  onClick={() => onSortMenuOpenChange((current) => !current)}
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
                        onSortModeChange("recent");
                        onSortMenuOpenChange(false);
                      }}
                    >
                      Recent
                    </button>
                    <button
                      type="button"
                      onClick={() => {
                        onSortModeChange("frequency");
                        onSortMenuOpenChange(false);
                      }}
                    >
                      Frequency
                    </button>
                    <button
                      type="button"
                      onClick={() => {
                        onSortModeChange("alphabetical");
                        onSortMenuOpenChange(false);
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
                    <button className={styles.foodResultMain} type="button" onClick={() => onSelectItem(item)}>
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
                        onClick={() => onOpenMenuChange((current) => (current === item.id ? "" : item.id))}
                      >
                        ⋮
                      </button>
                      {openMenuId === item.id ? (
                        <div className={styles.menuPanel}>
                          <Link href={editHref(item)}>Edit</Link>
                          <button type="button" onClick={() => onDeleteOption(item)}>
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
