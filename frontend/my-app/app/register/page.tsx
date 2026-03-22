"use client";

import { useState, FormEvent } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { motion } from "framer-motion";
import { Loader2, UserPlus } from "lucide-react";
import { registerUser } from "@/lib/api";
import { setToken } from "@/lib/auth";

export default function RegisterPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const pendingUrl = searchParams.get("url");

  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState("");

  const handleRegister = async (e: FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setError("");

    try {
      const { token } = await registerUser({ username, password, role: "REGISTERED" });
      setToken(token);
      
      if (pendingUrl) router.push(`/?url=${encodeURIComponent(pendingUrl)}`);
      else router.push("/");
    } catch (err: any) {
      setError(err.message || "Registration failed. Please try again.");
      setIsLoading(false);
    }
  };

  return (
    <main className="min-h-[calc(100vh-80px)] flex items-center justify-center p-6 transition-colors bg-[#F3F4F6] dark:bg-gray-950">
      
      <motion.div 
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="w-full max-w-md bg-white/70 dark:bg-gray-900/70 backdrop-blur-lg rounded-3xl shadow-glass p-10 border border-white/10 dark:border-gray-800"
      >
        <div className="text-center mb-8 flex flex-col items-center">
          <div className="bg-brand-500/10 p-3 rounded-xl mb-4 text-brand-500">
            <UserPlus className="w-8 h-8" />
          </div>
          <h1 className="text-3xl font-bold tracking-tight mb-2 text-gray-900 dark:text-white">Create an account</h1>
          <p className="text-gray-500 dark:text-gray-400">Sign up to access premium compression</p>
        </div>

        <form onSubmit={handleRegister} className="flex flex-col gap-5">
          {error && <div className="p-4 bg-red-50 text-red-600 rounded-xl text-sm font-medium">{error}</div>}

          <div>
            <label className="block text-sm font-semibold text-gray-700 dark:text-gray-300 mb-2">Username</label>
            <input 
              type="text" 
              required
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              className="w-full px-5 py-4 bg-white dark:bg-gray-800 border-2 border-gray-100 dark:border-gray-700 focus:border-brand-500 rounded-xl outline-none transition-all"
              placeholder="Choose a username"
            />
          </div>

          <div>
            <label className="block text-sm font-semibold text-gray-700 dark:text-gray-300 mb-2">Password</label>
            <input 
              type="password" 
              required
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full px-5 py-4 bg-white dark:bg-gray-800 border-2 border-gray-100 dark:border-gray-700 focus:border-brand-500 rounded-xl outline-none transition-all"
              placeholder="Create a strong password"
            />
          </div>

          <button 
            type="submit" 
            disabled={isLoading}
            className="mt-6 w-full flex items-center justify-center gap-2 bg-brand-500 hover:bg-brand-600 disabled:bg-sky-200 text-white text-lg font-semibold py-4 rounded-xl shadow-md transition-all"
          >
            {isLoading ? <Loader2 className="w-6 h-6 animate-spin" /> : "Sign up"}
          </button>
        </form>

        <p className="text-center mt-8 text-gray-600 dark:text-gray-400">
          Already have an account?{" "}
          <Link href={pendingUrl ? `/login?url=${encodeURIComponent(pendingUrl)}` : "/login"} className="text-brand-500 font-bold hover:underline">
            Log in
          </Link>
        </p>
      </motion.div>
    </main>
  );
}