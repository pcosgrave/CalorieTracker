import type { Dispatch, SetStateAction } from "react";
import styles from "@/app/page.module.css";
import { formatDateChip, shiftDateKey } from "@/app/lib/diary";

type DateNavigatorProps = {
  dateOptions: string[];
  selectedDate: string;
  setSelectedDate: Dispatch<SetStateAction<string>>;
};

export function DateNavigator({ dateOptions, selectedDate, setSelectedDate }: DateNavigatorProps) {
  return (
    <section className={styles.panel}>
      <div className={styles.dayControls}>
        <button
          aria-label="Previous day"
          className="secondary"
          type="button"
          onClick={() => setSelectedDate((current) => shiftDateKey(current, -1))}
        >
          &lt;
        </button>
        <div className={styles.dateScroller} aria-label="Select day">
          {dateOptions.map((dateKey) => {
            const chip = formatDateChip(dateKey);
            const isSelected = dateKey === selectedDate;

            return (
              <button
                className={isSelected ? styles.dateChipSelected : styles.dateChip}
                key={dateKey}
                type="button"
                onClick={() => setSelectedDate(dateKey)}
              >
                <span>{chip.day}</span>
                <strong>{chip.label}</strong>
              </button>
            );
          })}
        </div>
        <button
          aria-label="Next day"
          className="secondary"
          type="button"
          onClick={() => setSelectedDate((current) => shiftDateKey(current, 1))}
        >
          &gt;
        </button>
      </div>
    </section>
  );
}
