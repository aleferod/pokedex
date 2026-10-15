import { Navigate, Outlet, useLocation } from 'react-router';

import { useAuth } from './useAuth';

/**
 * Layout route for pages that need a session: visitors go to the sign-in page, which brings them
 * back here (path, search and hash) afterwards.
 */
export function ProtectedRoute() {
  const { session } = useAuth();
  const location = useLocation();
  if (session === null) {
    const here = `${location.pathname}${location.search}${location.hash}`;
    return <Navigate to={`/login?next=${encodeURIComponent(here)}`} replace />;
  }
  return <Outlet />;
}
