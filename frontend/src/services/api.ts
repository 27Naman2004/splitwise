import axios from 'axios';

const api = axios.create({
  // In production (Vercel), VITE_API_BASE_URL points to the Render backend.
  // In local dev, it's empty and Vite proxy handles the /api/* forwarding.
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
});

// Interceptor to inject bearer token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Interceptor to redirect to login on 401 Unauthorized
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default api;
