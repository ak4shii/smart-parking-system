import axios from 'axios';
import Cookies from 'js-cookie';
import { getCsrfToken, fetchCsrfToken } from './csrfService';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true,
});

api.interceptors.request.use(
  async (config) => {
    // Add JWT token
    const token = Cookies.get('jwt_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    
    // Add CSRF token for state-changing requests
    if (config.method && ['post', 'put', 'delete', 'patch'].includes(config.method.toLowerCase())) {
      let csrfToken = getCsrfToken();
      
      // If no CSRF token, try to fetch it
      if (!csrfToken) {
        csrfToken = await fetchCsrfToken();
      }
      
      if (csrfToken) {
        config.headers['X-XSRF-TOKEN'] = csrfToken;
      }
    }
    
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      Cookies.remove('jwt_token');
      window.location.href = '/login';
    }
    
    // If CSRF token is invalid, try to fetch a new one and retry
    if (error.response?.status === 403 && error.config && !error.config._retry) {
      error.config._retry = true;
      const csrfToken = await fetchCsrfToken();
      if (csrfToken) {
        error.config.headers['X-XSRF-TOKEN'] = csrfToken;
        return api.request(error.config);
      }
    }
    
    return Promise.reject(error);
  }
);

export default api;

