export const getToken = (): string | null => {
  if (typeof window !== 'undefined') {
    return localStorage.getItem('jwt_token');
  }
  return null;
};

export const setToken = (token: string): void => {
  if (typeof window !== 'undefined') {
    localStorage.setItem('jwt_token', token);
    window.dispatchEvent(new Event('auth-change')); // Notify the app
  }
};

export const removeToken = (): void => {
  if (typeof window !== 'undefined') {
    localStorage.removeItem('jwt_token');
    window.dispatchEvent(new Event('auth-change')); // Notify the app
  }
};