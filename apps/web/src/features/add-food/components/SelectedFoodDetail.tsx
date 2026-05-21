import type { Dispatch, SetStateAction } from "react";
import styles from "@/app/page.module.css";
import { mealLabels, mealOrder, scaleNutrients, shiftDateKey, toNumber } from "@/app/lib/diary";
import type { CatalogItem, LogState } from "../types";
import { itemServingUnit, splitIngredientLabel } from "../utils";

type SelectedFoodDetailProps = {
  logState: LogState;
  selectedItem: CatalogItem;
  onBack: () => void;
  onLog: (addMore: boolean) => void;
  onLogStateChange: Dispatch<SetStateAction<LogState>>;
};

export function SelectedFoodDetail({ logState, selectedItem, onBack, onLog, onLogStateChange }: SelectedFoodDetailProps) {
  const servingAmount = Math.max(toNumber(logState.servingAmount), 0.1);
  const multiplier = servingAmount / selectedItem.servingQuantity;
  const adjustedNutrients = scaleNutrients(selectedItem.nutrients, multiplier);

  return (
    <main className={styles.compactPage}>
      <header className={styles.mobileTopbar}>
        <button aria-label="Back to search" className={styles.backButton} type="button" onClick={onBack}>
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
            onChange={(event) => onLogStateChange((current) => ({ ...current, servingAmount: event.target.value }))}
          />
          <select
            aria-label="Serving unit"
            value={logState.servingUnit}
            onChange={(event) => onLogStateChange((current) => ({ ...current, servingUnit: event.target.value }))}
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
                  onChange={() => onLogStateChange((current) => ({ ...current, meal }))}
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
            <button className="secondary" type="button" onClick={() => onLogStateChange((current) => ({ ...current, date: shiftDateKey(current.date, -1) }))}>
              &lt;
            </button>
            <input
              aria-label="Log date"
              type="date"
              value={logState.date}
              onChange={(event) => onLogStateChange((current) => ({ ...current, date: event.target.value }))}
            />
            <button className="secondary" type="button" onClick={() => onLogStateChange((current) => ({ ...current, date: shiftDateKey(current.date, 1) }))}>
              &gt;
            </button>
          </div>
        </div>

        <div className={styles.bottomActions}>
          <button className="secondary" type="button" onClick={() => onLog(true)}>
            Log & add more
          </button>
          <button type="button" onClick={() => onLog(false)}>
            Log this
          </button>
        </div>
      </section>
    </main>
  );
}
