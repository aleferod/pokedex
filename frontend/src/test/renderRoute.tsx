import { render } from '@testing-library/react';
import { createMemoryRouter, type RouteObject } from 'react-router';
import { RouterProvider } from 'react-router/dom';

import { AppProviders } from '@/app/AppProviders';
import { createQueryClient } from '@/app/queryClient';
import { routes } from '@/app/routes';

/**
 * Renders the real route tree at `path`, with fresh providers (query cache, session). Any session
 * already in sessionStorage is picked up. `extraChildren` adds test-only pages under the layout
 * (for example one that throws, or a protected one).
 */
export function renderRoute(path: string, extraChildren: RouteObject[] = []) {
  const [root, ...rest] = routes;
  if (!root) {
    throw new Error('No root route');
  }
  const testRoutes: RouteObject[] = [
    { ...root, children: [...extraChildren, ...(root.children ?? [])] } as RouteObject,
    ...rest,
  ];
  const router = createMemoryRouter(testRoutes, { initialEntries: [path] });
  const queryClient = createQueryClient();
  // Same retry policy as production, without waiting between attempts.
  queryClient.setDefaultOptions({
    ...queryClient.getDefaultOptions(),
    queries: { ...queryClient.getDefaultOptions().queries, retryDelay: 0 },
  });
  return {
    router,
    queryClient,
    ...render(
      <AppProviders queryClient={queryClient}>
        <RouterProvider router={router} />
      </AppProviders>,
    ),
  };
}
