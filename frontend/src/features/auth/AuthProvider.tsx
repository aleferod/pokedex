import { useQueryClient } from '@tanstack/react-query';
import {
  useCallback,
  useEffect,
  useLayoutEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from 'react';

import { configureApiClient } from '@/shared/api';

import { registerRequest, signInRequest } from './api';
import { AuthContext, type AuthContextValue, type AuthNotice } from './authContext';
import type { RegisterValues, SignInValues } from './schemas';
import { clearSession, loadSession, saveSession } from './session';
import type { Session } from './types';

/** setTimeout's ceiling (about 24.8 days); longer delays would fire immediately. */
const MAX_TIMER_MS = 2_147_483_647;

/**
 * The signed-in session: restored synchronously from sessionStorage (no "signed out" flash on
 * reload), handed to the API client as the bearer token, and ended on sign-out, at expiry, or by
 * any 401 from the API. Ending it clears every cached query, so no protected data outlives it.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();
  const [session, setSession] = useState<Session | null>(() => loadSession());
  const [notice, setNotice] = useState<AuthNotice | null>(null);
  // The API client reads the token synchronously, outside React; event handlers keep it current.
  const tokenRef = useRef<string | null>(session?.token ?? null);

  const signOut = useCallback(
    (reason?: AuthNotice) => {
      clearSession();
      tokenRef.current = null;
      setSession(null);
      setNotice(reason ?? null);
      queryClient.clear();
    },
    [queryClient],
  );

  const signIn = useCallback(async (values: SignInValues) => {
    const response = await signInRequest(values);
    const next: Session = {
      token: response.accessToken,
      expiresAt: response.expiresAt,
      user: response.user,
    };
    saveSession(next);
    tokenRef.current = next.token;
    setNotice(null);
    setSession(next);
  }, []);

  const register = useCallback(
    async (values: RegisterValues) => {
      await registerRequest(values);
      await signIn({ username: values.username, password: values.password });
    },
    [signIn],
  );

  // A layout effect, not a plain one: React runs effects child-first, so a page's query would
  // otherwise fetch before the client knows the token, get a 401 and end a valid session.
  useLayoutEffect(() => {
    configureApiClient({
      getToken: () => tokenRef.current,
      // A 401 only ends a session that exists: a failed sign-in is not an expiry.
      onUnauthorized: () => {
        if (tokenRef.current !== null) {
          signOut('expired');
        }
      },
    });
    return () => {
      configureApiClient({});
    };
  }, [signOut]);

  useEffect(() => {
    if (session === null) {
      return undefined;
    }
    const remaining = Date.parse(session.expiresAt) - Date.now();
    const timer = setTimeout(
      () => {
        signOut('expired');
      },
      Math.min(Math.max(remaining, 0), MAX_TIMER_MS),
    );
    return () => {
      clearTimeout(timer);
    };
  }, [session, signOut]);

  const value = useMemo<AuthContextValue>(
    () => ({ session, notice, signIn, register, signOut }),
    [session, notice, signIn, register, signOut],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
