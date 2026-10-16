export type Role = 'USER' | 'ADMIN';

/** An account as pokedex-api returns it. */
export interface User {
  id: string;
  username: string;
  email: string;
  role: Role;
  createdAt: string;
}

/** What a successful sign-in gives the app. */
export interface Session {
  token: string;
  expiresAt: string;
  user: User;
}
