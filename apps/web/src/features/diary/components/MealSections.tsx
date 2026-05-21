import type { DiaryEntry, MealType } from "@calorie-tracker/shared";
import styles from "@/app/page.module.css";
import { mealLabels, mealOrder, scaleNutrients, totalsForEntries } from "@/app/lib/diary";

type MealSectionsProps = {
  groupedEntries: Record<MealType, DiaryEntry[]>;
  openMenuId: string;
  onEdit: (entry: DiaryEntry) => void;
  onRemove: (entryId: string) => void;
  onToggleMenu: (entryId: string) => void;
};

export function MealSections({ groupedEntries, openMenuId, onEdit, onRemove, onToggleMenu }: MealSectionsProps) {
  return (
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
                      <button className={styles.itemMain} type="button" onClick={() => onEdit(entry)}>
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
                          onClick={() => onToggleMenu(entry.entryId)}
                        >
                          ⋮
                        </button>
                        {openMenuId === entry.entryId ? (
                          <div className={styles.menuPanel}>
                            <button type="button" onClick={() => onEdit(entry)}>
                              Edit
                            </button>
                            <button type="button" onClick={() => onRemove(entry.entryId)}>
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
  );
}
