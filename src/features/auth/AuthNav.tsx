import { NavLink, useNavigate } from 'react-router';

import { navLinkClass } from '@/shared/ui/navLinkClass';

import { useAuth } from './useAuth';

/** The header's account controls, as items of the main navigation list. */
export function AuthNav() {
  const { session, signOut } = useAuth();
  const navigate = useNavigate();

  if (session === null) {
    return (
      <>
        <li>
          <NavLink to="/login" className={navLinkClass}>
            Sign in
          </NavLink>
        </li>
        <li>
          <NavLink to="/register" className={navLinkClass}>
            Register
          </NavLink>
        </li>
      </>
    );
  }

  return (
    <>
      <li className="flex items-center gap-2 px-2 text-sm">
        <span>Signed in as {session.user.username}</span>
        {session.user.role === 'ADMIN' && (
          <span className="rounded bg-amber-300 px-1.5 py-0.5 text-xs font-bold text-slate-900">
            Admin
          </span>
        )}
      </li>
      <li>
        <button
          type="button"
          onClick={() => {
            // Leave first: signing out on a protected page would otherwise redirect to sign-in.
            void Promise.resolve(navigate('/')).then(() => {
              signOut();
            });
          }}
          className="rounded border border-white/70 px-2 py-1 font-medium hover:bg-red-700"
        >
          Sign out
        </button>
      </li>
    </>
  );
}
