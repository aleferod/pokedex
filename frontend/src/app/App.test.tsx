import { screen, waitFor, within } from '@testing-library/react';
import { userEvent } from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { render } from '@testing-library/react';

import { saveSession } from '@/features/auth/session';
import { problemResponse } from '@/test/authApi';
import { renderRoute } from '@/test/renderRoute';
import { server } from '@/test/server';
import { testSession } from '@/test/sessions';

import { App } from './App';

describe('application shell', () => {
  it('renders the landmarks and titles the home page', async () => {
    renderRoute('/');

    expect(await screen.findByRole('banner')).toBeInTheDocument();
    expect(screen.getByRole('navigation', { name: 'Main' })).toBeInTheDocument();
    expect(screen.getAllByRole('main')).toHaveLength(1);
    expect(screen.getByRole('link', { name: 'Pokédex' })).toHaveAttribute('href', '/');
    await waitFor(() => {
      expect(document.title).toBe('Home · Pokédex');
    });
  });

  it('marks "Catalog" as the current page on the catalog', async () => {
    server.use(
      http.get('/api/v1/pokemon', () =>
        HttpResponse.json({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }),
      ),
    );
    renderRoute('/pokemon');
    await screen.findByText('No Pokémon to show.');

    const nav = screen.getByRole('navigation', { name: 'Main' });
    expect(within(nav).getByRole('link', { name: 'Catalog' })).toHaveAttribute(
      'aria-current',
      'page',
    );
  });

  it('marks "Catalog" as the current section, not the current page, on a Pokémon', async () => {
    server.use(
      http.get('/api/v1/pokemon/:idOrName', () =>
        problemResponse(404, 'not-found', "No Pokémon found for '25'"),
      ),
    );
    renderRoute('/pokemon/25');
    await screen.findByRole('heading', { name: 'Pokémon not found' });

    const nav = screen.getByRole('navigation', { name: 'Main' });
    expect(within(nav).getByRole('link', { name: 'Catalog' })).toHaveAttribute(
      'aria-current',
      'true',
    );
    expect(within(nav).getByRole('link', { name: 'Home' })).not.toHaveAttribute('aria-current');
  });

  it('lets a keyboard user skip straight to the content', async () => {
    const user = userEvent.setup();
    renderRoute('/');
    await screen.findByRole('banner');

    await user.tab();
    const skipLink = screen.getByRole('link', { name: 'Skip to content' });
    expect(skipLink).toHaveFocus();

    await user.keyboard('{Enter}');
    expect(screen.getByRole('main')).toHaveFocus();
  });

  it('shows a not-found page with a way home for unknown routes', async () => {
    renderRoute('/no-such-page');

    expect(await screen.findByRole('heading', { name: 'Page not found' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Go to the home page' })).toHaveAttribute('href', '/');
    expect(screen.getByRole('banner')).toBeInTheDocument();
    await waitFor(() => {
      expect(document.title).toBe('Page not found · Pokédex');
    });
  });

  it('catches a crashing page inside the layout and recovers on "Try again"', async () => {
    // React reports errors caught by a boundary on console.error; this test expects exactly that.
    const consoleError = vi.spyOn(console, 'error').mockImplementation(() => undefined);
    let shouldThrow = true;
    function Fragile() {
      if (shouldThrow) {
        throw new Error('boom at Fragile');
      }
      return <p>Recovered</p>;
    }
    const user = userEvent.setup();
    renderRoute('/fragile', [{ path: 'fragile', Component: Fragile }]);

    expect(await screen.findByRole('heading', { name: 'Something went wrong' })).toBeVisible();
    expect(screen.getByRole('banner')).toBeInTheDocument();
    expect(document.body).not.toHaveTextContent('boom at Fragile');
    expect(consoleError).toHaveBeenCalled();

    shouldThrow = false;
    await user.click(screen.getByRole('button', { name: 'Try again' }));

    expect(await screen.findByText('Recovered')).toBeInTheDocument();
  });

  it('boots the real application with its browser router at the current address', async () => {
    window.history.pushState({}, '', '/');

    render(<App />);

    expect(await screen.findByRole('navigation', { name: 'Main' })).toBeInTheDocument();
  });

  describe('My Pokédex', () => {
    const RECORD = '/local/0b6f8a52-7c1e-4d55-9a8e-3f1c2d4b5a69';

    afterEach(() => {
      sessionStorage.clear();
    });

    function localApi() {
      server.use(
        http.get('/api/v1/local-pokemon', () =>
          HttpResponse.json({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 }),
        ),
        http.get('/api/v1/local-pokemon/:id', () =>
          problemResponse(404, 'not-found', 'No local Pokémon with this id.'),
        ),
      );
    }

    it('is the current page on the list', async () => {
      saveSession(testSession());
      localApi();
      renderRoute('/local');

      const nav = await screen.findByRole('navigation', { name: 'Main' });
      expect(within(nav).getByRole('link', { name: 'My Pokédex' })).toHaveAttribute(
        'aria-current',
        'page',
      );
      expect(within(nav).getByRole('link', { name: 'Catalog' })).not.toHaveAttribute(
        'aria-current',
      );
    });

    it('is the current section on a record', async () => {
      saveSession(testSession());
      localApi();
      renderRoute(RECORD);

      const nav = await screen.findByRole('navigation', { name: 'Main' });
      expect(within(nav).getByRole('link', { name: 'My Pokédex' })).toHaveAttribute(
        'aria-current',
        'true',
      );
    });

    it('sends a visitor to sign in and back', async () => {
      const { router } = renderRoute('/local?tag=starter');

      await waitFor(() => {
        expect(router.state.location.pathname).toBe('/login');
      });
      expect(router.state.location.search).toBe('?next=%2Flocal%3Ftag%3Dstarter');
      expect(screen.getByRole('link', { name: 'My Pokédex' })).toHaveAttribute('href', '/local');
    });
  });
});
