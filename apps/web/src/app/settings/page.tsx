"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import type { SyncSettings } from "@calorie-tracker/shared";
import styles from "../page.module.css";
import { currentClientAuthUser } from "@/lib/auth/client";
import { getCognitoConfig } from "@/lib/auth/config";
import { getPendingSyncCount, getSyncSettings, saveSyncSettings, syncNow } from "@/lib/sync/service";

const defaultSettings: SyncSettings = {
  syncEnabled: false,
  backupMode: "disabled",
  apiBaseUrl: "",
  calorieTargetMin: 1800,
  calorieTargetMax: 2200,
  weightUnit: "kilograms",
};

export default function SettingsPage() {
  const [settings, setSettings] = useState<SyncSettings>(defaultSettings);
  const [pendingCount, setPendingCount] = useState(0);
  const [status, setStatus] = useState("");
  const [authUser, setAuthUser] = useState(() => currentClientAuthUser());

  useEffect(() => {
    void (async () => {
      setSettings(await getSyncSettings());
      setPendingCount(getPendingSyncCount());
      setAuthUser(currentClientAuthUser());
    })();
  }, []);

  async function save(): Promise<void> {
    await saveSyncSettings(settings);
    setStatus("Saved sync settings.");
  }

  async function runSync(): Promise<void> {
    try {
      const result = await syncNow();
      setPendingCount(getPendingSyncCount());
      setSettings(await getSyncSettings());
      setStatus(`Synced ${result.pushed} pushed, ${result.pulled} pulled.`);
    } catch (error) {
      setStatus(error instanceof Error ? error.message : "Sync failed.");
    }
  }

  return (
    <main className={styles.page}>
      <div className={styles.shell}>
        <header className={styles.topbar}>
          <div className={styles.brand}>
            <h1>Sync Settings</h1>
            <p>Keep local data primary and optionally back it up to your API.</p>
          </div>
          <Link className={styles.textButton} href="/">
            Back home
          </Link>
        </header>

        {status ? <p className={styles.success}>{status}</p> : null}

        <section className={styles.panel}>
          <div className={styles.form}>
            <label>
              <span>Enable sync</span>
              <select
                value={settings.syncEnabled ? "enabled" : "disabled"}
                onChange={(event) => setSettings((current) => ({ ...current, syncEnabled: event.target.value === "enabled" }))}
              >
                <option value="disabled">Disabled</option>
                <option value="enabled">Enabled</option>
              </select>
            </label>

            <label>
              API base URL
              <input
                placeholder={getCognitoConfig().apiBaseUrl}
                value={settings.apiBaseUrl || ""}
                onChange={(event) => setSettings((current) => ({ ...current, apiBaseUrl: event.target.value }))}
              />
            </label>

            <div className={styles.subtle}>
              {authUser ? (
                <p>Signed in as {authUser.email || authUser.name || authUser.userSub}</p>
              ) : (
                <p>Not signed in. Sign in to sync against your Cognito account.</p>
              )}
            </div>

            <label>
              Backup mode
              <select value={settings.backupMode} onChange={(event) => setSettings((current) => ({ ...current, backupMode: event.target.value as SyncSettings["backupMode"] }))}>
                <option value="disabled">Disabled</option>
                <option value="manual_backup">Manual backup</option>
                <option value="automatic_backup">Automatic backup</option>
              </select>
            </label>

            <label>
              Calorie target min
              <input
                value={settings.calorieTargetMin ?? 1800}
                onChange={(event) => setSettings((current) => ({ ...current, calorieTargetMin: Number(event.target.value) || 0 }))}
              />
            </label>

            <label>
              Calorie target max
              <input
                value={settings.calorieTargetMax ?? 2200}
                onChange={(event) => setSettings((current) => ({ ...current, calorieTargetMax: Number(event.target.value) || 0 }))}
              />
            </label>

            <label>
              Weight unit
              <select value={settings.weightUnit ?? "kilograms"} onChange={(event) => setSettings((current) => ({ ...current, weightUnit: event.target.value as SyncSettings["weightUnit"] }))}>
                <option value="kilograms">Kilograms</option>
                <option value="pounds">Pounds</option>
              </select>
            </label>

            <label>
              Goal weight (kg)
              <input
                value={settings.goalWeightKg ?? ""}
                onChange={(event) => setSettings((current) => ({ ...current, goalWeightKg: event.target.value ? Number(event.target.value) : undefined }))}
              />
            </label>

            <p className={styles.subtle}>Pending local changes: {pendingCount}</p>
            <p className={styles.subtle}>Last successful sync: {settings.lastSuccessfulSyncAt || "Never"}</p>

            <div className={styles.actions}>
              <Link className={styles.textButton} href={authUser ? "/api/auth/logout" : "/api/auth/login?returnTo=/settings"}>
                {authUser ? "Sign out" : "Sign in"}
              </Link>
              {!authUser ? (
                <Link className={styles.textButton} href="/api/auth/login?returnTo=/settings&provider=Google">
                  Sign in with Google
                </Link>
              ) : null}
              <button type="button" onClick={save}>
                Save
              </button>
              <button type="button" onClick={runSync} disabled={!authUser || !settings.syncEnabled || !settings.apiBaseUrl}>
                Sync now
              </button>
            </div>
          </div>
        </section>
      </div>
    </main>
  );
}
