"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import styles from "@/app/dashboard.module.css";
import { currentClientAuthUser } from "@/lib/auth/client";
import { maybeAutoSync } from "@/lib/sync/service";
import { readDiaryEntries, todayDateKey, totalsForEntries } from "@/app/lib/diary";
import { convertWeightFromKg, readWeightEntries } from "@/app/lib/weight";
import { LocalStorageSyncStateRepository } from "@/lib/repositories/local-storage";
import type { SyncSettings } from "@calorie-tracker/shared";

const defaultSettings: SyncSettings = {
  syncEnabled: false,
  backupMode: "disabled",
  apiBaseUrl: "",
  calorieTargetMin: 1800,
  calorieTargetMax: 2200,
  weightUnit: "kilograms",
};

export function HomePageClient() {
  const [authUser, setAuthUser] = useState(() => currentClientAuthUser());
  const [settings, setSettings] = useState<SyncSettings>(defaultSettings);
  const [isDark, setIsDark] = useState(false);
  const [todayCalories, setTodayCalories] = useState(0);
  const [latestWeightKg, setLatestWeightKg] = useState<number | null>(null);

  useEffect(() => {
    void (async () => {
      await maybeAutoSync();
      setAuthUser(currentClientAuthUser());
      setSettings(await new LocalStorageSyncStateRepository().getSettings());
      const entries = readDiaryEntries().filter((entry) => entry.loggedAt.slice(0, 10) === todayDateKey());
      setTodayCalories(totalsForEntries(entries).calories);
      setLatestWeightKg(readWeightEntries()[0]?.weightKg ?? null);
      setIsDark(document.documentElement.classList.contains("dark") || window.matchMedia("(prefers-color-scheme: dark)").matches);
    })();
  }, []);

  const weightUnit = settings.weightUnit ?? "kilograms";
  const calorieGoalLabel = useMemo(() => {
    if (!settings.calorieTargetMin || !settings.calorieTargetMax) {
      return "Set a target range in settings";
    }
    return `${settings.calorieTargetMin}-${settings.calorieTargetMax} kcal target`;
  }, [settings.calorieTargetMax, settings.calorieTargetMin]);

  return (
    <main className={`${styles.page} ${isDark ? styles.darkPage : ""}`}>
      <div className={styles.shell}>
        <header className={styles.topbar}>
          <div>
            <h1 className={styles.title}>Home</h1>
            <p className={styles.subtitle}>Your calorie and weight dashboard.</p>
          </div>
          <div className={styles.actions}>
            <Link className={styles.textButton} href="/settings">
              Settings
            </Link>
            <Link className={styles.textButton} href={authUser ? "/api/auth/logout" : "/api/auth/login?returnTo=/"}>
              {authUser ? authUser.email || "Sign out" : "Sign in"}
            </Link>
          </div>
        </header>

        <section className={`${styles.panel} ${styles.metricsGrid}`}>
          <Link className={styles.metricCard} href="/food">
            <span className={styles.metricLabel}>Calories Logged</span>
            <span className={styles.metricValue}>{Math.round(todayCalories)} cal</span>
            <span className={styles.metricHint}>{calorieGoalLabel}</span>
          </Link>

          <Link className={styles.metricCard} href="/weight">
            <span className={styles.metricLabel}>Weight</span>
            <span className={styles.metricValue}>
              {latestWeightKg == null ? "--" : `${convertWeightFromKg(latestWeightKg, weightUnit).toFixed(1)} ${weightUnit === "pounds" ? "lb" : "kg"}`}
            </span>
            <span className={styles.metricHint}>View history</span>
          </Link>
        </section>

        <section className={styles.section}>
          <h2 className={styles.sectionTitle}>Quick actions</h2>
          <div className={styles.actions}>
            <Link className={styles.primaryButton} href="/food">
              Open food log
            </Link>
            <Link className={styles.secondaryButton} href="/add">
              Add food
            </Link>
            <Link className={styles.secondaryButton} href="/weight">
              Log weight
            </Link>
          </div>
        </section>
      </div>
    </main>
  );
}
