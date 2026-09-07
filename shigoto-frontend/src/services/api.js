import axios from 'axios';

// Send credentials so Spring Security can associate requests with the server session.
const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  withCredentials: true,
  withXSRFToken: true,
  xsrfCookieName: 'XSRF-TOKEN',
  xsrfHeaderName: 'X-XSRF-TOKEN',
});

const safeMethods = new Set(['get', 'head', 'options']);

/**
 * Ensures unsafe requests have a CSRF cookie before Axios adds the matching header.
 */
api.interceptors.request.use(async (config) => {
  const method = (config.method || 'get').toLowerCase();
  const hasCsrfCookie = document.cookie
    .split('; ')
    .some((cookie) => cookie.startsWith('XSRF-TOKEN='));

  if (!safeMethods.has(method) && !hasCsrfCookie) {
    // Prime Spring Security's token repository before the original mutation continues.
    await api.get('/auth/csrf');
  }
  return config;
});

export default api;
