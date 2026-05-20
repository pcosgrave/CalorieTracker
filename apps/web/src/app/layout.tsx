import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "CalorieTracker",
  description: "Manual calorie tracking with private barcode shortcuts.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
