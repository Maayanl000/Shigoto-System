import { createContext, useContext } from 'react';

export const AuthContext = createContext(null);

/**
 * Returns the authentication context and rejects use outside its provider.
 */
export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used inside AuthProvider');
  return context;
}
