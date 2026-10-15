import type { RouteObject } from 'react-router';

import { LoginPage, RegisterPage } from '@/features/auth';
import { CatalogPage } from '@/features/catalog';

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
      { path: 'login', Component: LoginPage },
      { path: 'register', Component: RegisterPage },
      { path: '*', Component: NotFoundPage },
    ],
  },
];
