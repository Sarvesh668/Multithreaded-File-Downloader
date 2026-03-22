"use client";

import { useState, useEffect, FormEvent } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { motion, AnimatePresence } from "framer-motion";
import { Link as LinkIcon, Loader2, CheckCircle, Download, FileDown, ArrowRight, Lock, Zap } from "lucide-react";
import { getToken } from "@/lib/auth";
import { processFile } from "@/lib/api";

type AppState = "idle" | "tier-selection" | "loading" | "success" | "error";
type CompressionTier = "NONE" | "STANDARD" | "MAX";

export default function HomePage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  
  const [url, setUrl] = useState("");
  const [status, setStatus] = useState<AppState>("idle");
  const [errorMessage, setErrorMessage] = useState("");
  const [isLoggedIn, setIsLoggedIn] = useState(false);

  useEffect(() => {
    setIsLoggedIn(!!getToken());
    const pendingUrl = searchParams.get("url");
    if (pendingUrl) {
      setUrl(pendingUrl);
      setStatus("tier-selection");
    }
  }, [searchParams]);

  const handleUrlSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (url) setStatus("tier-selection");
  };

  const executeTask = async (tier: CompressionTier) => {
  setStatus("loading");
  setErrorMessage("");
  try {
    const response = await processFile(url, tier); 
    
    // 1. Check if the backend actually sent a file (PDF) or just JSON
    const contentType = response.headers.get("content-type");
    
    if (contentType && contentType.includes("application/json")) {
      // If backend sends JSON, we need to extract the base64 or download link
      const data = await response.json();
      console.log("Backend sent JSON instead of a file:", data);
      throw new Error("Backend returned JSON, but frontend expected a raw PDF file. Check console.");
    }

    // 2. Safely parse the raw binary blob
    const blob = await response.blob();
    
    // Ensure it's treated as a PDF
    const pdfBlob = new Blob([blob], { type: 'application/pdf' });
    
    // 3. Create a temporary URL and trigger download
    const downloadUrl = window.URL.createObjectURL(pdfBlob);
    const link = document.createElement('a');
    link.href = downloadUrl;
    link.setAttribute('download', 'timesSixteen_processed_file.pdf'); 
    document.body.appendChild(link);
    link.click();
    
    // 4. Cleanup memory
    link.parentNode?.removeChild(link);
    window.URL.revokeObjectURL(downloadUrl);

    setStatus("success");
  } catch (error: any) {
    setStatus("error");
    setErrorMessage(error.message || "Task failed. Please try again.");
  }
};

  return (
    <main className="min-h-[calc(100vh-80px)] flex flex-col items-center justify-center p-6">
      
      <div className="text-center mb-10 max-w-2xl">
        <h1 className="text-4xl md:text-5xl font-bold mb-4 tracking-tight">
          Concurrent File Downloader
        </h1>
        <p className="text-lg text-gray-600 dark:text-gray-400">
          Download files at maximum speeds. Optionally compress PDF documents to reduce file size.
        </p>
      </div>

      <div className="w-full max-w-4xl bg-white dark:bg-gray-900 rounded-3xl shadow-xl p-8 md:p-14 min-h-[350px] flex flex-col justify-center relative overflow-hidden transition-colors duration-300">
        <AnimatePresence mode="wait">
          
          {/* STATE 1: IDLE (Enter URL) */}
          {(status === "idle" || status === "error") && (
            <motion.form
              key="form"
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.95 }}
              transition={{ duration: 0.3 }}
              onSubmit={handleUrlSubmit}
              className="flex flex-col items-center w-full"
            >
              <div className="w-full relative mb-8">
                <div className="absolute inset-y-0 left-0 pl-6 flex items-center pointer-events-none">
                  <LinkIcon className="h-8 w-8 text-gray-400" />
                </div>
                <input
                  type="url"
                  required
                  placeholder="Paste File URL here..."
                  value={url}
                  onChange={(e) => setUrl(e.target.value)}
                  className="w-full pl-20 pr-6 py-6 text-xl bg-gray-50 dark:bg-gray-800 border-2 border-transparent focus:border-[#E5322D] dark:focus:border-[#E5322D] rounded-2xl outline-none transition-all shadow-inner"
                />
              </div>
              {status === "error" && <p className="text-red-500 mb-6 font-medium">{errorMessage}</p>}
              <button
                type="submit"
                className="flex items-center gap-3 bg-gray-900 dark:bg-white text-white dark:text-gray-900 text-xl font-semibold py-5 px-10 rounded-full shadow-lg hover:scale-105 transition-transform"
              >
                Next Step <ArrowRight className="w-6 h-6" />
              </button>
            </motion.form>
          )}

          {/* STATE 2: TIER SELECTION */}
          {status === "tier-selection" && (
            <motion.div
              key="tier"
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -20 }}
              className="flex flex-col w-full"
            >
              <h2 className="text-2xl font-bold mb-6 text-center">Processing Options</h2>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                
                {/* Download Only */}
                <button 
                  onClick={() => executeTask("NONE")}
                  className="flex flex-col items-center justify-center p-8 border-2 border-gray-200 dark:border-gray-700 rounded-2xl hover:border-blue-500 hover:bg-blue-50 dark:hover:bg-blue-900/20 transition-all group"
                >
                  <Download className="w-10 h-10 text-gray-400 group-hover:text-blue-500 mb-4" />
                  <h3 className="text-xl font-bold mb-2">Download Only</h3>
                  <p className="text-sm text-gray-500 text-center">Fetch the file as-is with no modifications.</p>
                </button>

                {/* Standard Tier */}
                <button 
                  onClick={() => executeTask("STANDARD")}
                  className="flex flex-col items-center justify-center p-8 border-2 border-gray-200 dark:border-gray-700 rounded-2xl hover:border-[#E5322D] hover:bg-red-50 dark:hover:bg-red-950/20 transition-all group"
                >
                  <FileDown className="w-10 h-10 text-gray-400 group-hover:text-[#E5322D] mb-4" />
                  <h3 className="text-xl font-bold mb-2">Standard Compress</h3>
                  <p className="text-sm text-gray-500 text-center">Good quality, standard file size reduction.</p>
                </button>

                {/* Max Tier (Locked for Guests) */}
                {isLoggedIn ? (
                  <button 
                    onClick={() => executeTask("MAX")}
                    className="flex flex-col items-center justify-center p-8 border-2 border-[#E5322D] bg-red-50 dark:bg-red-900/10 rounded-2xl hover:shadow-lg transition-all"
                  >
                    <Zap className="w-10 h-10 text-[#E5322D] mb-4" />
                    <h3 className="text-xl font-bold mb-2">Extreme Compress</h3>
                    <p className="text-sm text-gray-500 text-center">Maximum size reduction. High quality.</p>
                  </button>
                ) : (
                  <button 
                    onClick={() => router.push(`/login?url=${encodeURIComponent(url)}`)}
                    className="flex flex-col items-center justify-center p-8 border-2 border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800 rounded-2xl hover:border-gray-400 transition-all opacity-80"
                  >
                    <Lock className="w-10 h-10 text-gray-400 mb-4" />
                    <h3 className="text-xl font-bold mb-2">Extreme Compress</h3>
                    <p className="text-sm text-[#E5322D] font-semibold text-center mt-2">Log in for Max Compression</p>
                  </button>
                )}
              </div>
              <button 
                onClick={() => setStatus("idle")}
                className="mt-6 text-gray-500 hover:text-gray-900 dark:hover:text-white underline text-center"
              >
                Change URL
              </button>
            </motion.div>
          )}

          {/* STATE 3: LOADING */}
          {status === "loading" && (
            <motion.div key="loading" initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="flex flex-col items-center justify-center">
              <Loader2 className="w-20 h-20 text-[#E5322D] animate-spin mb-6" />
              <h2 className="text-3xl font-bold mb-2">Processing File...</h2>
              <p className="text-gray-500">Please wait while we fetch and process your request.</p>
            </motion.div>
          )}

          {/* STATE 4: SUCCESS */}
          {status === "success" && (
            <motion.div key="success" initial={{ opacity: 0, scale: 0.9 }} animate={{ opacity: 1, scale: 1 }} className="flex flex-col items-center justify-center">
              <CheckCircle className="w-24 h-24 text-green-500 mb-6" />
              <h2 className="text-3xl font-bold mb-8">Process Complete!</h2>
              <div className="flex gap-4">
                <button className="flex items-center gap-2 bg-gray-900 dark:bg-white dark:text-gray-900 text-white font-semibold py-4 px-8 rounded-full">
                  <Download className="w-5 h-5" /> Download Result
                </button>
                <button onClick={() => setStatus("idle")} className="border-2 border-gray-200 py-4 px-8 rounded-full font-semibold">
                  Start Over
                </button>
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </main>
  );
}