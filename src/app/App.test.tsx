import { screen, waitFor } from '@testing-library/react';
import { userEvent } from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import { render } from '@testing-library/react';

import { renderRoute } from '@/test/renderRoute';

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
});
