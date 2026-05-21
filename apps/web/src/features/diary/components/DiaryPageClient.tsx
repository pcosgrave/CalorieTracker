"use client";

import styles from "@/app/page.module.css";
import Link from "next/link";
import { useEffect } from "react";
import { DateNavigator } from "./DateNavigator";
import { DaySummaryPanel } from "./DaySummaryPanel";
import { EditEntryPanel } from "./EditEntryPanel";
import { MealSections } from "./MealSections";
import { useDiaryPage } from "../hooks/useDiaryPage";
import { maybeAutoSync } from "@/lib/sync/service";

export function DiaryPageClient() {
  useEffect(() => {
    void maybeAutoSync();
  }, []);

  const {
    dateOptions,
    editForm,
    groupedEntries,
    openMenuId,
    selectedDate,
    selectedEntries,
    totals,
    clearSelectedDay,
    removeEntry,
    saveEditedEntry,
    setEditForm,
    setOpenMenuId,
    setSelectedDate,
    startEditingEntry,
    updateEditForm,
  } = useDiaryPage();

  return (
    <main className={styles.page}>
      <div className={styles.shell}>
        <header className={styles.topbar}>
          <div className={styles.brand}>
            <h1>CalorieTracker</h1>
            <p>Manual labels, private barcode shortcuts, cloud sync when signed in.</p>
          </div>
          <div className={styles.headerActions}>
            <Link className={styles.textButton} href="/settings">
              Sync
            </Link>
            <button type="button">Sign in with Google</button>
          </div>
        </header>

        <DateNavigator dateOptions={dateOptions} selectedDate={selectedDate} setSelectedDate={setSelectedDate} />

        <DaySummaryPanel
          selectedDate={selectedDate}
          selectedEntryCount={selectedEntries.length}
          totals={totals}
          onClearDay={clearSelectedDay}
        />

        {editForm ? (
          <EditEntryPanel editForm={editForm} onCancel={() => setEditForm(null)} onSave={saveEditedEntry} onUpdate={updateEditForm} />
        ) : null}

        <MealSections
          groupedEntries={groupedEntries}
          openMenuId={openMenuId}
          onEdit={startEditingEntry}
          onRemove={removeEntry}
          onToggleMenu={(entryId) => setOpenMenuId((current) => (current === entryId ? "" : entryId))}
        />
      </div>
    </main>
  );
}
