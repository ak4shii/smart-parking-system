import axios from 'axios';
import Cookies from 'js-cookie';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

/**
 * Fetches the CSRF token from the backend
 * This should be called before making any state-changing requests (POST, PUT, DELETE)
 */
export const fetchCsrfToken = async (): Promise<string | null> => {
  try {
    const response = await axios.get(`${API_BASE_URL}/csrf-token`, {
      withCredentials: true,
    });
    
    const token = response.data.token;
    
    if (token) {
      // Store in sessionStorage for quick access
      sessionStorage.setItem('X-XSRF-TOKEN', token);
      return token;
    }
    
    return null;
  } catch (error) {
    console.error('Failed to fetch CSRF token:', error);
    return null;
  }
};

/**
 * Gets the CSRF token from sessionStorage or cookies
 */
export const getCsrfToken = (): string | null => {
  // Try sessionStorage first
  const sessionToken = sessionStorage.getItem('X-XSRF-TOKEN');
  if (sessionToken) {
    return sessionToken;
  }
  
  // Fallback to cookie (Spring Security default cookie name)
  const cookieToken = Cookies.get('XSRF-TOKEN');
  if (cookieToken) {
    sessionStorage.setItem('X-XSRF-TOKEN', cookieToken);
    return cookieToken;
  }
  
  return null;
};

/**
 * Clears the stored CSRF token
 */
export const clearCsrfToken = (): void => {
  sessionStorage.removeItem('X-XSRF-TOKEN');
  Cookies.remove('XSRF-TOKEN');
};

/**
 * Initializes CSRF token on app startup
 */
export const initializeCsrfToken = async (): Promise<void> => {
  await fetchCsrfToken();
};

