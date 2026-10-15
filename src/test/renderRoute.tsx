import { QueryClientProvider } from '@tanstack/react-query';
import { render } from '@testing-library/react';
import { createMemoryRouter, RouterProvider, type RouteObject } from 'react-router';

import { createQueryClient } from '@/app/queryClient';
import { routes } from '@/app/routes';

/**
 * Renders the real route tree at `path`, with fresh providers. `extraChildren` adds test-only
 * pages under the layout (for example one that throws).
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
  return {
    router,
    ...render(
      <QueryClientProvider client={queryClient}>
        <RouterProvider router={router} />
      </QueryClientProvider>,
    ),
  };
}
