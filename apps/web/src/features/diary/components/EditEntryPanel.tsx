import styles from "@/app/page.module.css";
import { mealLabels, mealOrder } from "@/app/lib/diary";
import type { EntryEditForm } from "../types";

type EditEntryPanelProps = {
  editForm: EntryEditForm;
  onCancel: () => void;
  onSave: () => void;
  onUpdate: (field: keyof EntryEditForm, value: string) => void;
};

export function EditEntryPanel({ editForm, onCancel, onSave, onUpdate }: EditEntryPanelProps) {
  return (
    <section className={styles.panel}>
      <div className={styles.panelHeader}>
        <h2>Edit logged food</h2>
        <button className="secondary" type="button" onClick={onCancel}>
          Cancel
        </button>
      </div>
      <div className={styles.form}>
        <div className={styles.twoColumn}>
          <label>
            Name
            <input value={editForm.name} onChange={(event) => onUpdate("name", event.target.value)} />
          </label>
          <label>
            Brand
            <input value={editForm.brand} onChange={(event) => onUpdate("brand", event.target.value)} />
          </label>
        </div>
        <div className={styles.fourColumn}>
          <label>
            Calories
            <input min="0" type="number" value={editForm.calories} onChange={(event) => onUpdate("calories", event.target.value)} />
          </label>
          <label>
            Protein
            <input min="0" type="number" value={editForm.protein} onChange={(event) => onUpdate("protein", event.target.value)} />
          </label>
          <label>
            Carbs
            <input min="0" type="number" value={editForm.carbs} onChange={(event) => onUpdate("carbs", event.target.value)} />
          </label>
          <label>
            Fat
            <input min="0" type="number" value={editForm.fat} onChange={(event) => onUpdate("fat", event.target.value)} />
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
              onChange={(event) => onUpdate("servingMultiplier", event.target.value)}
            />
          </label>
          <label>
            Meal
            <select value={editForm.meal} onChange={(event) => onUpdate("meal", event.target.value)}>
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
          <input type="date" value={editForm.date} onChange={(event) => onUpdate("date", event.target.value)} />
        </label>
        <div className={styles.actions}>
          <button type="button" onClick={onSave}>
            Save changes
          </button>
        </div>
      </div>
    </section>
  );
}
