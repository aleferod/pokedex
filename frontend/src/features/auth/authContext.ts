import { createContext } from 'react';

import type { RegisterValues, SignInValues } from './schemas';
import type { Session } from './types';

/** Why the session ended, when the user should be told. */
export type AuthNotice = 'expired';

export interface AuthContextValue {
  session: Session | null;
  notice: AuthNotice | null;
  signIn: (values: SignInValues) => Promise<void>;
  register: (values: RegisterValues) => Promise<void>;
  signOut: (notice?: AuthNotice) => void;
}

export const AuthContext = createContext<AuthContextValue | null>(null);
