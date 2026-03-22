"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Zap, LogOut, Sun, Moon } from "lucide-react";
import { getToken, removeToken } from "@/lib/auth";

export default function Navbar() {
  const router = useRouter();
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [isDarkMode, setIsDarkMode] = useState(false);

  useEffect(() => {
    const checkAuth = () => setIsLoggedIn(!!getToken());
    checkAuth();
    window.addEventListener('auth-change', checkAuth);
    
    // Check dark mode preference on load
    const isDark = localStorage.getItem('theme') === 'dark' || (!('theme' in localStorage) && window.matchMedia('(prefers-color-scheme: dark)').matches);
    setIsDarkMode(isDark);
    if (isDark) document.documentElement.classList.add('dark');

    return () => window.removeEventListener('auth-change', checkAuth);
  }, []);

  const toggleDarkMode = () => {
    if (isDarkMode) {
      document.documentElement.classList.remove('dark');
      localStorage.setItem('theme', 'light');
    } else {
      document.documentElement.classList.add('dark');
      localStorage.setItem('theme', 'dark');
    }
    setIsDarkMode(!isDarkMode);
  };

  const handleLogout = () => {
    removeToken();
    router.push("/login");
  };

  return (
    // GLASS EFFECT: Semi-transparent bg + backdrop-blur
    <header className="h-20 bg-white/70 backdrop-blur-xl dark:bg-gray-950/70 border-b border-gray-100 dark:border-gray-800 shadow-sm sticky top-0 z-50 transition-colors duration-200">
      <div className="max-w-7xl mx-auto px-6 h-full flex items-center justify-between">
        
        {/* Logo Section */}
        <Link href="/" className="flex items-center gap-2 group">
          <div className="bg-brand-500 p-2 rounded-lg group-hover:scale-105 transition-transform">
            <Zap className="w-6 h-6 text-white" />
          </div>
          {/* New Logo Name: timesSixteen */}
          <span className="text-2xl font-bold tracking-tight text-gray-900 dark:text-white">
            times<span className="text-brand-500">Sixteen</span>
          </span>
        </Link>

        {/* Right Section: Toggle + Auth */}
        <div className="flex items-center gap-6">
          
          {/* Working Theme Toggle */}
          <button onClick={toggleDarkMode} className="p-2 rounded-lg text-gray-500 hover:text-brand-500 transition-colors">
            {isDarkMode ? <Sun className="w-5 h-5" /> : <Moon className="w-5 h-5" />}
          </button>

          {/* Dynamic Auth Section */}
          <nav className="flex items-center gap-6 border-l border-gray-200 dark:border-gray-700 pl-6">
            {isLoggedIn ? (
              <>
                <span className="text-gray-600 dark:text-gray-300 font-medium hidden sm:block">Welcome!</span>
                <button onClick={handleLogout} className="flex items-center gap-2 text-gray-600 dark:text-gray-300 hover:text-brand-500 font-semibold">
                  <LogOut className="w-5 h-5" />
                  Log out
                </button>
              </>
            ) : (
              <>
                <Link href="/login" className="text-gray-900 dark:text-white hover:text-brand-500 font-semibold text-lg">
                  Log in
                </Link>
                <Link href="/register" className="bg-brand-500 hover:bg-brand-600 text-white font-semibold text-lg py-2.5 px-6 rounded-full shadow-md">
                  Sign up
                </Link>
              </>
            )}
          </nav>
        </div>
      </div>
    </header>
  );
}