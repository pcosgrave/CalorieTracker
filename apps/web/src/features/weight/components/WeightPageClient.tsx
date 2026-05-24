"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import type { SyncSettings, WeightEntry } from "@calorie-tracker/shared";
import styles from "@/app/dashboard.module.css";
import { currentClientAuthUser } from "@/lib/auth/client";
import { maybeAutoSync } from "@/lib/sync/service";
import { LocalStorageSyncStateRepository } from "@/lib/repositories/local-storage";
import { convertWeightFromKg, convertWeightToKg, createWeightEntry, deleteWeightEntry, readWeightEntries, writeWeightEntry } from "@/app/lib/weight";

type Range = "daily" | "weekly" | "monthly";

const defaultSettings: SyncSettings = {
  syncEnabled: false,
  backupMode: "disabled",
  apiBaseUrl: "",
  weightUnit: "kilograms",
};

function dateKey(date: Date): string {
  return date.toISOString().slice(0, 10);
}

function startOfWeek(date: Date): Date {
  const next = new Date(date);
  const day = next.getDay();
  const diff = day === 0 ? -6 : 1 - day;
  next.setDate(next.getDate() + diff);
  next.setHours(12, 0, 0, 0);
  return next;
}

function startOfMonth(date: Date): Date {
  const next = new Date(date);
  next.setDate(1);
  next.setHours(12, 0, 0, 0);
  return next;
}

function formatRangeLabel(range: Range, anchor: string): string {
  const date = new Date(`${anchor}T12:00:00`);
  if (range === "daily") {
    return new Intl.DateTimeFormat("en-CA", { weekday: "long", month: "short", day: "numeric" }).format(date);
  }
  if (range === "weekly") {
    const weekNumber = getWeekNumber(date);
    return `Week ${weekNumber} of ${date.getFullYear()}`;
  }
  return new Intl.DateTimeFormat("en-CA", { month: "long", year: "numeric" }).format(date);
}

function getWeekNumber(date: Date): number {
  const target = new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()));
  const dayNum = target.getUTCDay() || 7;
  target.setUTCDate(target.getUTCDate() + 4 - dayNum);
  const yearStart = new Date(Date.UTC(target.getUTCFullYear(), 0, 1));
  return Math.ceil((((target.getTime() - yearStart.getTime()) / 86400000) + 1) / 7);
}

function buildAnchors(range: Range, current: string): string[] {
  const base = new Date(`${current}T12:00:00`);
  const anchors: string[] = [];
  const count = range === "daily" ? 12 : range === "weekly" ? 10 : 8;
  for (let i = count - 1; i >= 0; i -= 1) {
    const next = new Date(base);
    if (range === "daily") next.setDate(base.getDate() - i);
    if (range === "weekly") next.setDate(base.getDate() - i * 7);
    if (range === "monthly") next.setMonth(base.getMonth() - i);
    anchors.push(dateKey(range === "weekly" ? startOfWeek(next) : range === "monthly" ? startOfMonth(next) : next));
  }
  return anchors;
}

function filterEntries(entries: WeightEntry[], range: Range, anchor: string): WeightEntry[] {
  const start = new Date(`${anchor}T12:00:00`);
  if (range === "daily") {
    return entries.filter((entry) => entry.loggedAt.slice(0, 10) === anchor);
  }

  const end = new Date(start);
  if (range === "weekly") end.setDate(start.getDate() + 6);
  if (range === "monthly") end.setMonth(start.getMonth() + 1, 0);

  const startKey = dateKey(start);
  const endKey = dateKey(end);
  return entries.filter((entry) => {
    const key = entry.loggedAt.slice(0, 10);
    return key >= startKey && key <= endKey;
  });
}

function yTicks(values: number[], goalValue?: number): number[] {
  const baseline = values[0] ?? goalValue ?? 0;
  const minValue = Math.min(...values, goalValue ?? baseline);
  const maxValue = Math.max(...values, goalValue ?? baseline);
  const span = Math.max(maxValue - minValue, 1);
  return [maxValue, minValue + span / 2, minValue];
}

function toDisplay(weightKg: number, unit: "kilograms" | "pounds"): number {
  return convertWeightFromKg(weightKg, unit);
}

export function WeightPageClient() {
  const [authUser, setAuthUser] = useState(() => currentClientAuthUser());
  const [settings, setSettings] = useState<SyncSettings>(defaultSettings);
  const [entries, setEntries] = useState<WeightEntry[]>([]);
  const [range, setRange] = useState<Range>("weekly");
  const [editingEntryId, setEditingEntryId] = useState<string | null>(null);
  const [weightText, setWeightText] = useState("");
  const [selectedAnchor, setSelectedAnchor] = useState(dateKey(startOfWeek(new Date())));
  const [isDark, setIsDark] = useState(false);

  useEffect(() => {
    void (async () => {
      await maybeAutoSync();
      setAuthUser(currentClientAuthUser());
      setSettings(await new LocalStorageSyncStateRepository().getSettings());
      setEntries(readWeightEntries());
      setIsDark(document.documentElement.classList.contains("dark") || window.matchMedia("(prefers-color-scheme: dark)").matches);
    })();
  }, []);

  const unit = settings.weightUnit ?? "kilograms";
  const currentAnchor = useMemo(() => {
    const now = new Date();
    if (range === "daily") return dateKey(now);
    if (range === "weekly") return dateKey(startOfWeek(now));
    return dateKey(startOfMonth(now));
  }, [range]);

  useEffect(() => {
    setSelectedAnchor(currentAnchor);
  }, [currentAnchor, range]);

  const anchors = useMemo(() => buildAnchors(range, currentAnchor), [currentAnchor, range]);
  const selectedEntries = useMemo(
    () => filterEntries(entries, range, selectedAnchor).sort((left, right) => left.loggedAt.localeCompare(right.loggedAt)),
    [entries, range, selectedAnchor],
  );

  const latestEntry = useMemo(() => [...entries].sort((a, b) => b.loggedAt.localeCompare(a.loggedAt))[0] ?? null, [entries]);
  const currentDayEntry = useMemo(() => {
    if (range !== "daily") return null;
    return [...entries]
      .filter((entry) => entry.loggedAt.slice(0, 10) <= selectedAnchor)
      .sort((a, b) => b.loggedAt.localeCompare(a.loggedAt))[0] ?? null;
  }, [entries, range, selectedAnchor]);

  const chartData = useMemo(
    () =>
      selectedEntries.map((entry) => ({
        label:
          range === "monthly"
            ? new Date(entry.loggedAt).getDate().toString()
            : new Intl.DateTimeFormat("en-CA", { weekday: "short" }).format(new Date(entry.loggedAt)),
        value: toDisplay(entry.weightKg, unit),
      })),
    [range, selectedEntries, unit],
  );

  const goalValue = settings.goalWeightKg ? toDisplay(settings.goalWeightKg, unit) : undefined;
  const ticks = chartData.length ? yTicks(chartData.map((item) => item.value), goalValue) : [];
  const chartBounds = useMemo(() => {
    const top = ticks[0];
    const bottom = ticks[ticks.length - 1];
    if (top == null || bottom == null) {
      return null;
    }
    return {
      maxValue: top,
      minValue: bottom,
      span: Math.max(top - bottom, 1),
    };
  }, [ticks]);

  const points = useMemo(() => {
    if (!chartData.length || !chartBounds) return "";
    return chartData
      .map((item, index) => {
        const x = chartData.length === 1 ? 50 : (index / (chartData.length - 1)) * 100;
        const y = 100 - (((item.value - chartBounds.minValue) / chartBounds.span) * 100);
        return `${x},${y}`;
      })
      .join(" ");
  }, [chartBounds, chartData]);

  const goalY = useMemo(() => {
    if (!chartData.length || goalValue == null || !chartBounds) return null;
    return 100 - (((goalValue - chartBounds.minValue) / chartBounds.span) * 100);
  }, [chartBounds, chartData.length, goalValue]);

  function resetForm(): void {
    setEditingEntryId(null);
    setWeightText("");
  }

  async function saveEntry(): Promise<void> {
    const parsed = Number(weightText);
    if (!Number.isFinite(parsed) || parsed <= 0) {
      return;
    }
    const loggedAt = `${range === "daily" ? selectedAnchor : dateKey(new Date())}T12:00:00.000Z`;
    const existing = entries.find((entry) => entry.entryId === editingEntryId);
    const next = createWeightEntry(loggedAt, convertWeightToKg(parsed, unit), existing?.entryId);
    if (existing) {
      next.createdAt = existing.createdAt;
    }
    writeWeightEntry(next);
    setEntries(readWeightEntries());
    resetForm();
    await maybeAutoSync();
  }

  async function removeEntry(entryId: string): Promise<void> {
    deleteWeightEntry(entryId);
    setEntries(readWeightEntries());
    if (editingEntryId === entryId) {
      resetForm();
    }
    await maybeAutoSync();
  }

  function startEditing(entry: WeightEntry): void {
    setEditingEntryId(entry.entryId);
    setWeightText(toDisplay(entry.weightKg, unit).toFixed(1).replace(/\.0$/, ""));
  }

  return (
    <main className={`${styles.page} ${isDark ? styles.darkPage : ""}`}>
      <div className={styles.shell}>
        <header className={styles.topbar}>
          <div>
            <h1 className={styles.title}>Weight</h1>
            <p className={styles.subtitle}>Track progress daily, weekly, or monthly.</p>
          </div>
          <div className={styles.actions}>
            <Link className={styles.textButton} href="/">
              Home
            </Link>
            <Link className={styles.textButton} href={authUser ? "/api/auth/logout" : "/api/auth/login?returnTo=/weight"}>
              {authUser ? authUser.email || "Sign out" : "Sign in"}
            </Link>
          </div>
        </header>

        <section className={`${styles.panel} ${styles.section}`}>
          <div className={styles.periodTabs}>
            {(["daily", "weekly", "monthly"] as const).map((option) => (
              <button
                key={option}
                className={range === option ? styles.activePill : styles.pill}
                type="button"
                onClick={() => setRange(option)}
              >
                {option[0]!.toUpperCase() + option.slice(1)}
              </button>
            ))}
          </div>

          <div className={styles.dateTabs}>
            {anchors.map((anchor) => (
              <button
                key={anchor}
                className={selectedAnchor === anchor ? styles.activePill : styles.pill}
                type="button"
                onClick={() => setSelectedAnchor(anchor)}
              >
                {range === "daily"
                  ? new Intl.DateTimeFormat("en-CA", { month: "short", day: "numeric" }).format(new Date(`${anchor}T12:00:00`))
                  : range === "weekly"
                    ? `W${getWeekNumber(new Date(`${anchor}T12:00:00`))}`
                    : new Intl.DateTimeFormat("en-CA", { month: "short" }).format(new Date(`${anchor}T12:00:00`))}
              </button>
            ))}
            {selectedAnchor !== currentAnchor ? (
              <button className={styles.pill} type="button" onClick={() => setSelectedAnchor(currentAnchor)}>
                {range === "daily" ? "Today" : range === "weekly" ? "This week" : "This month"}
              </button>
            ) : null}
          </div>

          <p className={styles.subtle}>{formatRangeLabel(range, selectedAnchor)}</p>
          <p className={styles.subtle}>
            Latest: {latestEntry ? `${toDisplay(latestEntry.weightKg, unit).toFixed(1)} ${unit === "pounds" ? "lb" : "kg"}` : "--"}
          </p>

          {range === "daily" ? (
            <section className={styles.panel}>
              {currentDayEntry ? (
                <>
                  <div className={styles.metricValue}>{toDisplay(currentDayEntry.weightKg, unit).toFixed(1)} {unit === "pounds" ? "lb" : "kg"}</div>
                  <p className={styles.subtle}>
                    {currentDayEntry.entryId === latestEntry?.entryId ? "Current weight" : "Logged weight"}
                  </p>
                  {goalValue != null ? <p className={styles.goalLine}>Goal: {goalValue.toFixed(1)} {unit === "pounds" ? "lb" : "kg"}</p> : null}
                  <div className={styles.actions}>
                    <button className={styles.secondaryButton} type="button" onClick={() => startEditing(currentDayEntry)}>
                      Edit
                    </button>
                    <button className={styles.dangerButton} type="button" onClick={() => void removeEntry(currentDayEntry.entryId)}>
                      Delete
                    </button>
                  </div>
                </>
              ) : (
                <p className={styles.subtle}>No weight logged for this day yet.</p>
              )}
            </section>
          ) : (
            <div className={styles.chartShell}>
              <div className={styles.chartPanel}>
                <div className={styles.yAxis}>
                  {ticks.map((tick, index) => (
                    <span key={`${tick}-${index}`}>{index === 0 ? `${tick.toFixed(1)} ${unit === "pounds" ? "lb" : "kg"}` : tick.toFixed(1)}</span>
                  ))}
                </div>
                <div className={styles.chartArea}>
                  <svg className={styles.chartSvg} viewBox="0 0 100 100" preserveAspectRatio="none">
                    <line x1="0" x2="100" y1="0" y2="0" stroke="rgba(148,159,179,0.25)" strokeWidth="0.6" />
                    <line x1="0" x2="100" y1="50" y2="50" stroke="rgba(148,159,179,0.25)" strokeWidth="0.6" />
                    <line x1="0" x2="100" y1="100" y2="100" stroke="rgba(148,159,179,0.25)" strokeWidth="0.6" />
                    {goalY != null ? <line x1="0" x2="100" y1={goalY} y2={goalY} stroke="#36c15b" strokeWidth="0.9" /> : null}
                    {points ? <polyline fill="none" stroke="#1677f0" strokeWidth="1.4" points={points} /> : null}
                    {chartData.map((item, index) => {
                      if (!chartBounds) {
                        return null;
                      }
                      const x = chartData.length === 1 ? 50 : (index / (chartData.length - 1)) * 100;
                      const y = 100 - (((item.value - chartBounds.minValue) / chartBounds.span) * 100);
                      return <circle key={`${item.label}-${x}`} cx={x} cy={y} r="1.5" fill="#1677f0" />;
                    })}
                  </svg>
                  <div className={styles.xAxis}>
                    {chartData.map((item) => (
                      <span key={item.label}>{item.label}</span>
                    ))}
                  </div>
                </div>
              </div>
              {goalValue != null ? <p className={styles.goalLine}>Goal baseline: {goalValue.toFixed(1)} {unit === "pounds" ? "lb" : "kg"}</p> : null}
            </div>
          )}

          <div className={styles.actions}>
            <button className={styles.primaryButton} type="button" onClick={saveEntry}>
              {editingEntryId ? "Update weight" : "Log weight"}
            </button>
            {editingEntryId ? (
              <button className={styles.secondaryButton} type="button" onClick={resetForm}>
                Cancel edit
              </button>
            ) : null}
          </div>

          <div className={styles.formGrid}>
            <label className={styles.field}>
              <span>{editingEntryId ? "Edit weight" : "New weight"} ({unit === "pounds" ? "lb" : "kg"})</span>
              <input value={weightText} onChange={(event) => setWeightText(event.target.value.replace(/[^\d.]/g, ""))} placeholder={unit === "pounds" ? "184.2" : "83.6"} />
            </label>
          </div>
        </section>

        <section className={`${styles.panel} ${styles.section}`}>
          <h2 className={styles.sectionTitle}>Logged weights</h2>
          <div className={styles.historyList}>
            {(range === "daily" ? entries.filter((entry) => entry.loggedAt.slice(0, 10) <= selectedAnchor) : selectedEntries)
              .sort((left, right) => right.loggedAt.localeCompare(left.loggedAt))
              .map((entry) => (
                <div className={styles.historyRow} key={entry.entryId}>
                  <div>
                    <div>{new Intl.DateTimeFormat("en-CA", { month: "short", day: "numeric", year: "numeric" }).format(new Date(entry.loggedAt))}</div>
                    <div className={styles.subtle}>{toDisplay(entry.weightKg, unit).toFixed(1)} {unit === "pounds" ? "lb" : "kg"}</div>
                  </div>
                  <div className={styles.historyActions}>
                    <button className={styles.secondaryButton} type="button" onClick={() => startEditing(entry)}>
                      Edit
                    </button>
                    <button className={styles.dangerButton} type="button" onClick={() => void removeEntry(entry.entryId)}>
                      Delete
                    </button>
                  </div>
                </div>
              ))}
          </div>
        </section>
      </div>
    </main>
  );
}
