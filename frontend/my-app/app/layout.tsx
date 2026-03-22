import type { Metadata } from "next";
import { Inter } from "next/font/google";
import { ThemeProvider } from "next-themes";
import "./globals.css";
import Navbar from "@/components/layout/Navbar";

const inter = Inter({ subsets: ["latin"] });

export const metadata: Metadata = {
  title: "timesSixteen | Concurrent File Downloader & Compressor",
  description: "Download files at maximum speeds and optimize PDF documents.",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    // suppressHydrationWarning is required by next-themes
    <html lang="en" suppressHydrationWarning> 
      <body className={inter.className}>
        <ThemeProvider attribute="class" defaultTheme="light">
          <Navbar />
          {children}
        </ThemeProvider>
      </body>
    </html>
  );
}