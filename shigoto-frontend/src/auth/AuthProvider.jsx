import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';
import { AuthContext } from './authContext';

/**
 * Provides session-backed authentication state and login, logout, and profile operations.
 */
export default function AuthProvider({ children }) {
  const navigate = useNavigate();
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [loggingOut, setLoggingOut] = useState(false);

  // Restore authenticated user state from the existing server session when the provider mounts.
  useEffect(() => {
    let active = true;

    api.get('/auth/me')
      .then((response) => {
        if (active) setUser(response.data);
      })
      .catch(() => {
        if (active) setUser(null);
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, []);

  // Expose the current authentication state and operations through one stable context value.
  const value = useMemo(() => ({
    user,
    loading,
    loggingOut,
    /** Authenticates the supplied credentials and stores the returned user in local state. */
    login: async (credentials) => {
      const response = await api.post('/auth/login', credentials);
      setUser(response.data);
      return response.data;
    },
    /** Registers a candidate, starts their session, and stores the authenticated user. */
    register: async (details) => {
      await api.post('/auth/register', details);
      const response = await api.post('/auth/login', {
        email: details.email,
        password: details.password,
      });
      setUser(response.data);
      return response.data;
    },
    /** Updates the candidate profile and synchronizes the local authenticated user. */
    updateProfile: async (profile) => {
      try {
        const response = await api.put('/auth/me/profile', profile);
        setUser(response.data);
        return response.data;
      } catch (error) {
        if (error.response?.status === 401) setUser(null);
        throw error;
      }
    },
    /** Ends the authenticated session while keeping local navigation and auth state consistent. */
    logout: async () => {
      setLoggingOut(true);
      // Leave protected UI and clear local authentication before requesting server-side logout.
      navigate('/', { replace: true, flushSync: true });
      setUser(null);
      try {
        // End the server session; local cleanup remains authoritative if it already expired.
        await api.post('/auth/logout');
      } catch {
        // Local auth state must still clear if the session already expired.
      } finally {
        setLoggingOut(false);
      }
    },
  }), [loading, loggingOut, navigate, user]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
