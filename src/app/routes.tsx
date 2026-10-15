import type { RouteObject } from 'react-router';

import { LoginPage, ProtectedRoute, RegisterPage } from '@/features/auth';
import { CatalogPage } from '@/features/catalog';
import { PokemonDetailPage } from '@/features/detail';
import { LocalCreatePage, LocalEditPage, LocalListPage, LocalRecordPage } from '@/features/local';

import { HomePage } from './HomePage';
import { Layout } from './Layout';
import { NotFoundPage } from './NotFoundPage';

/** The route tree: every page renders inside the layout; unknown paths get the not-found page. */
export const routes: RouteObject[] = [
  {
    path: '/',
    Component: Layout,
    children: [
      { index: true, Component: HomePage },
      { path: 'pokemon', Component: CatalogPage },
      { path: 'pokemon/:idOrName', Component: PokemonDetailPage },
      {
        // US03/US04: the local Pokédex needs a session.
        Component: ProtectedRoute,
        children: [
          { path: 'local', Component: LocalListPage },
          { path: 'local/new', Component: LocalCreatePage },
          { path: 'local/:id', Component: LocalRecordPage },
          { path: 'local/:id/edit', Component: LocalEditPage },
        ],
      },
      { path: 'login', Component: LoginPage },
      { path: 'register', Component: RegisterPage },
      { path: '*', Component: NotFoundPage },
    ],
  },
];
