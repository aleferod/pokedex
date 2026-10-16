import { useContext } from 'react';

import { AuthContext, type AuthContextValue } from './authContext';

/** The signed-in session and the actions that change it. */
export function useAuth(): AuthContextValue {
  const value = useContext(AuthContext);
  if (value === null) {
    throw new Error('useAuth must be used inside <AuthProvider>');
  }
  return value;
}
