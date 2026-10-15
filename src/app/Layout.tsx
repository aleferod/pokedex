import { useRef, type MouseEvent } from 'react';
import { Link, NavLink, Outlet, useLocation } from 'react-router';

import { AuthNav } from '@/features/auth';
import { ErrorBoundary } from '@/shared/ui/ErrorBoundary';
import { navLinkClass } from '@/shared/ui/navLinkClass';

/**
 * The page frame: skip link, header with brand and main navigation, one main landmark, footer.
 * Each page renders inside an error boundary keyed by the address, so navigating away from a
 * crashed page recovers.
 */
export function Layout() {
  const location = useLocation();
  const mainRef = useRef<HTMLElement>(null);

  const skipToContent = (event: MouseEvent<HTMLAnchorElement>) => {
    event.preventDefault();
    mainRef.current?.focus();
  };

  return (
    <div className="flex min-h-dvh flex-col bg-slate-50 text-slate-900">
      <a
        href="#main-content"
        onClick={skipToContent}
        className="sr-only z-50 rounded bg-white px-3 py-2 font-semibold text-red-700 focus:not-sr-only focus:absolute focus:top-2 focus:left-2"
      >
        Skip to content
      </a>
      <header className="bg-red-600 text-white shadow">
        <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-x-6 gap-y-2 px-4 py-3">
          <Link to="/" className="text-xl font-bold tracking-tight">
            Pokédex
          </Link>
          <nav aria-label="Main">
            <ul className="flex flex-wrap items-center gap-2">
              <li>
                <NavLink to="/" end className={navLinkClass}>
                  Home
                </NavLink>
              </li>
              <li>
                <NavLink to="/pokemon" className={navLinkClass}>
                  Catalog
                </NavLink>
              </li>
              <AuthNav />
            </ul>
          </nav>
        </div>
      </header>
      <main
        id="main-content"
        ref={mainRef}
        tabIndex={-1}
        className="mx-auto w-full max-w-6xl flex-1 px-4 py-6 focus:outline-none"
      >
        <ErrorBoundary key={location.pathname}>
          <Outlet />
        </ErrorBoundary>
      </main>
      <footer className="border-t border-slate-200 py-4 text-center text-sm text-slate-600">
        Data from{' '}
        <a href="https://pokeapi.co" className="underline">
          PokeAPI
        </a>
      </footer>
    </div>
  );
}
