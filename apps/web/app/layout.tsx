import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "HUB Feat Creators",
  description: "Centralização operacional para assessorias de creators",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="pt-BR">
      <body className="bg-background text-foreground antialiased">{children}</body>
    </html>
  );
}
