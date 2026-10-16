import { apiRequest } from '@/shared/api';

import type { RegisterValues, SignInValues } from './schemas';
import type { User } from './types';

/** pokedex-api's answer to a successful sign-in. */
export interface TokenResponse {
  accessToken: string;
  tokenType: string;
  expiresAt: string;
  user: User;
}

export function signInRequest(values: SignInValues): Promise<TokenResponse> {
  return apiRequest<TokenResponse>('/auth/login', { method: 'POST', body: values });
}

export function registerRequest(values: RegisterValues): Promise<User> {
  return apiRequest<User>('/auth/register', { method: 'POST', body: values });
}
