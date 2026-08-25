import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';
import { AuthContext } from './authContext';

export default function AuthProvider({ children }) {
  const navigate = useNavigate();
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [loggingOut, setLoggingOut] = useState(false);

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
    loggingOut,
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
      try {
        const response = await api.put('/auth/me/profile', profile);
        setUser(response.data);
        return response.data;
      } catch (error) {
        if (error.response?.status === 401) setUser(null);
        throw error;
      }
    },
    logout: async () => {
      setLoggingOut(true);
      navigate('/', { replace: true, flushSync: true });
      setUser(null);
      try {
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
