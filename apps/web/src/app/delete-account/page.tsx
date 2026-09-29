import Link from "next/link";
import styles from "../page.module.css";

export const metadata = {
  title: "Delete your BiteWise account",
  description: "How to request deletion of your BiteWise account and associated data.",
};

export default function DeleteAccountPage() {
  return (
    <main className={styles.page}>
      <div className={styles.shell}>
        <header className={styles.topbar}>
          <div className={styles.brand}>
            <h1>Delete your BiteWise account</h1>
            <p>Account and data deletion information from Cosgrave Labs.</p>
          </div>
          <Link className={styles.textButton} href="/">
            Back to BiteWise
          </Link>
        </header>

        <section className={styles.panel}>
          <h2>How to request deletion</h2>
          <ol>
            <li>Open the BiteWise app and sign in to the account you want to delete.</li>
            <li>Open <strong>Settings</strong>.</li>
            <li>Choose <strong>Delete account</strong> and confirm the deletion.</li>
          </ol>
          <p>
            You can also open the web app&apos;s <Link href="/settings">Sync Settings</Link> page,
            sign in, and choose <strong>Delete account</strong>.
          </p>
          <p>
            If you cannot sign in, use the support contact shown on the BiteWise Google Play
            listing and include the email address associated with your account so Cosgrave Labs
            can verify and process the request.
          </p>
        </section>

        <section className={styles.panel}>
          <h2>Data deleted</h2>
          <p>Confirming deletion removes the following account-associated data:</p>
          <ul>
            <li>Your BiteWise account and Cognito identity.</li>
            <li>Cloud-synced food products and barcode aliases.</li>
            <li>Diary entries, weight entries, and sync records.</li>
            <li>Account settings and device sync data.</li>
          </ul>
        </section>

        <section className={styles.panel}>
          <h2>Data kept and retention</h2>
          <p>
            Data stored only on your device is not deleted remotely. After requesting deletion,
            uninstall BiteWise or clear its app storage to remove remaining local data.
          </p>
          <p>
            Operational logs and encrypted infrastructure backups may retain limited information
            for their configured retention period before automatic expiry. Deleted account data is
            not restored or used to provide the service during that period.
          </p>
        </section>
      </div>
    </main>
  );
}
