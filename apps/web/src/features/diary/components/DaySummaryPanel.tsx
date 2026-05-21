import Link from "next/link";
import styles from "@/app/page.module.css";
import { formatDateHeading } from "@/app/lib/diary";

type DaySummaryPanelProps = {
  selectedDate: string;
  selectedEntryCount: number;
  totals: {
    calories: number;
    protein: number;
    carbs: number;
    fat: number;
  };
  onClearDay: () => void;
};

export function DaySummaryPanel({ selectedDate, selectedEntryCount, totals, onClearDay }: DaySummaryPanelProps) {
  return (
    <section className={styles.panel}>
      <div className={styles.panelHeader}>
        <div>
          <h2>{formatDateHeading(selectedDate)}</h2>
          <p className={styles.subtle}>{selectedDate}</p>
        </div>
        <button className="secondary" disabled={selectedEntryCount === 0} type="button" onClick={onClearDay}>
          Clear day
        </button>
      </div>

      <div className={styles.daySummary}>
        <div className={styles.metric}>
          <strong>{totals.calories}</strong>
          <span>Calories</span>
        </div>
        <div className={styles.metric}>
          <strong>{totals.protein}g</strong>
          <span>Protein</span>
        </div>
        <div className={styles.metric}>
          <strong>{totals.carbs}g</strong>
          <span>Carbs</span>
        </div>
        <div className={styles.metric}>
          <strong>{totals.fat}g</strong>
          <span>Fat</span>
        </div>
      </div>

      <div className={styles.addRow}>
        <Link aria-label={`Add food for ${selectedDate}`} className={styles.addButton} href={`/add?date=${selectedDate}`}>
          +
        </Link>
      </div>
    </section>
  );
}
