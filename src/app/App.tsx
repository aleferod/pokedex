import { createBrowserRouter } from 'react-router';
import { RouterProvider } from 'react-router/dom';

import { AppProviders } from './AppProviders';
import { createQueryClient } from './queryClient';
import { routes } from './routes';

const router = createBrowserRouter(routes);
const queryClient = createQueryClient();

/** The application root: providers (server-state cache, session) and the router. */
export function App() {
  return (
    <AppProviders queryClient={queryClient}>
      <RouterProvider router={router} />
    </AppProviders>
  );
}
