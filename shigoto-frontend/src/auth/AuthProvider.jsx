import { useEffect, useMemo, useState } from 'react';
import api from '../services/api';
import { AuthContext } from './authContext';

export default function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

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

  const value = useMemo(() => ({
    user,
    loading,
    login: async (credentials) => {
      const response = await api.post('/auth/login', credentials);
      setUser(response.data);
      return response.data;
    },
    register: async (details) => {
      await api.post('/auth/register', details);
      const response = await api.post('/auth/login', {
        email: details.email,
        password: details.password,
      });
      setUser(response.data);
      return response.data;
    },
    updateProfile: async (profile) => {
      const response = await api.put('/auth/me/profile', profile);
      setUser(response.data);
      return response.data;
    },
    logout: async () => {
      try {
        await api.post('/auth/logout');
      } catch {
        // Local auth state must still clear if the session already expired.
      } finally {
        setUser(null);
      }
    },
  }), [loading, user]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
