import axios from 'axios';

// הגדרת כתובת הבסיס של שרת ה-Spring Boot שלנו
const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  withCredentials: true,
  withXSRFToken: true,
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN',
});

const safeMethods = new Set(['get', 'head', 'options']);

api.interceptors.request.use(async (config) => {
  const method = (config.method || 'get').toLowerCase();
  const hasCsrfCookie = document.cookie
    .split('; ')
    .some((cookie) => cookie.startsWith('XSRF-TOKEN='));

  if (!safeMethods.has(method) && !hasCsrfCookie) {
    await api.get('/auth/csrf');
  }
  return config;
});

export default api;
