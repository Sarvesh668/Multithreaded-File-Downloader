import { getToken } from './auth';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api';

export async function loginUser(credentials: Record<string, string>) {
  const res = await fetch(`${API_BASE_URL}/auth/authenticate`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(credentials),
  });
  if (!res.ok) throw new Error('Invalid credentials');
  return res.json(); 
}

export async function registerUser(userData: Record<string, string>) {
  const res = await fetch(`${API_BASE_URL}/auth/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(userData),
  });
  if (!res.ok) throw new Error('Registration failed');
  return res.json(); 
}

export async function processFile(fileUrl: string, compressionTier: "NONE" | "STANDARD" | "MAX") {
  const token = getToken();
  const headers: HeadersInit = { 
    'Content-Type': 'application/json',
    'Accept': 'application/pdf, application/json' // Tell backend we expect a file or JSON
  };
  
  if (token && (compressionTier === "MAX" || compressionTier === "STANDARD" || compressionTier === "NONE")) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const res = await fetch(`${API_BASE_URL}/downloads`, {
    method: 'POST',
    headers,
    body: JSON.stringify({
      url: fileUrl, 
      tier: compressionTier }),
  });

  if (!res.ok) {
    const errorData = await res.text();
    throw new Error(errorData || 'Failed to process file');
  }
  
  return res; 
}