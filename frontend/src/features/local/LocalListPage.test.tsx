import { screen, waitFor, within } from '@testing-library/react';
import { userEvent } from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';

import { saveSession } from '@/features/auth/session';
import { problemResponse } from '@/test/authApi';
import { localApi, localRecord, recordId } from '@/test/localApi';
import { renderRoute } from '@/test/renderRoute';
import { server } from '@/test/server';
import { testSession } from '@/test/sessions';

const PIKACHU = localRecord({ id: recordId(9001) });
const BULBASAUR = localRecord({
  id: recordId(9002),
  pokeapiId: 1,
  name: 'bulbasaur',
  region: null,
  internalTags: ['starter', 'kanto'],
  updatedAt: '2026-10-12T08:30:00Z',
});
const IVYSAUR = localRecord({
  id: recordId(9003),
  pokeapiId: 2,
  name: 'ivysaur',
  internalTags: [],
});

/** The list requests (GET /local-pokemon), as query strings. */
function listQueries(api: ReturnType<typeof localApi>): string[] {
  return api.requests
    .filter((r) => r.method === 'GET' && r.path === '/api/v1/local-pokemon')
    .map((r) => r.search.toString());
}

function card(name: string): HTMLElement {
  const article = screen.getByRole('link', { name }).closest('article');
  if (article === null) {
    throw new Error(`No card for ${name}`);
  }
  return article;
}

describe('My Pokédex list', () => {
  beforeEach(() => {
    saveSession(testSession());
  });
  afterEach(() => {
    sessionStorage.clear();
  });

  it('lists the local records with what US04 asks for, in Pokédex order', async () => {
    const api = localApi([PIKACHU, BULBASAUR]);
    renderRoute('/local');

    expect(await screen.findByRole('link', { name: 'Pikachu' })).toHaveAttribute(
      'href',
      `/local/${PIKACHU.id}`,
    );
    const pikachu = within(card('Pikachu'));
    expect(pikachu.getByText('#25')).toBeInTheDocument();
    expect(pikachu.getByText('Kanto')).toBeInTheDocument();
    expect(pikachu.getByRole('img', { name: 'Pikachu' })).toHaveAttribute('src', PIKACHU.spriteUrl);
    expect(pikachu.getByRole('link', { name: 'Filter by tag starter-line' })).toHaveAttribute(
      'href',
      '/local?tag=starter-line',
    );
    expect(pikachu.getByText(/Updated/).querySelector('time')).toHaveAttribute(
      'dateTime',
      PIKACHU.updatedAt,
    );
    const names = within(screen.getByRole('list', { name: 'Records' }))
      .getAllByRole('heading')
      .map((heading) => heading.textContent);
    expect(names).toEqual(['Bulbasaur', 'Pikachu']);
    expect(screen.getByText('2 records')).toBeInTheDocument();
    expect(listQueries(api)).toEqual(['page=0&size=20']);
    await waitFor(() => {
      expect(document.title).toBe('My Pokédex · Pokédex');
    });
  });

  it('pages through the records with the URL', async () => {
    const user = userEvent.setup();
    const many = Array.from({ length: 25 }, (_, i) =>
      localRecord({ id: recordId(100 + i), pokeapiId: 100 + i, name: `mon-${String(i)}` }),
    );
    const api = localApi(many);
    const { router } = renderRoute('/local');
    await screen.findByRole('link', { name: 'Mon-0' });

    await user.click(screen.getByRole('button', { name: 'Next page' }));

    expect(await screen.findByRole('link', { name: 'Mon-20' })).toBeInTheDocument();
    expect(router.state.location.search).toBe('?page=2');
    expect(listQueries(api)).toContain('page=1&size=20');
    expect(screen.getByRole('navigation', { name: 'Pagination' })).toHaveTextContent('Page 2 of 2');
  });

  it('changes the page size through the URL, from page 1', async () => {
    const user = userEvent.setup();
    const api = localApi([PIKACHU, BULBASAUR]);
    const { router } = renderRoute('/local?page=2&tag=kanto');
    await screen.findByRole('search', { name: 'Filter my Pokédex' });

    await user.click(await screen.findByRole('link', { name: 'Go to the last page' }));
    await user.selectOptions(await screen.findByRole('combobox', { name: 'Per page' }), '40');

    await waitFor(() => {
      expect(router.state.location.search).toBe('?size=40&tag=kanto');
    });
    expect(listQueries(api)).toContain('page=0&size=40&tag=kanto');
  });

  it('filters by name and tag through the URL, from page 1', async () => {
    const user = userEvent.setup();
    const api = localApi([PIKACHU, BULBASAUR, IVYSAUR]);
    const { router } = renderRoute('/local?page=2');
    const filters = within(await screen.findByRole('search', { name: 'Filter my Pokédex' }));

    await user.type(filters.getByLabelText('Name contains'), 'saur');
    await user.type(filters.getByLabelText('Tag'), 'starter');
    await user.click(filters.getByRole('button', { name: 'Filter' }));

    await waitFor(() => {
      expect(router.state.location.search).toBe('?name=saur&tag=starter');
    });
    expect(await screen.findByRole('link', { name: 'Bulbasaur' })).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Ivysaur' })).not.toBeInTheDocument();
    expect(listQueries(api)).toContain('page=0&size=20&name=saur&tag=starter');
  });

  it('opens a shared filtered view with the filters filled in', async () => {
    const api = localApi([PIKACHU, BULBASAUR]);
    renderRoute('/local?tag=electric&name=pika');

    expect(await screen.findByRole('link', { name: 'Pikachu' })).toBeInTheDocument();
    expect(screen.getByLabelText('Name contains')).toHaveValue('pika');
    expect(screen.getByLabelText('Tag')).toHaveValue('electric');
    expect(listQueries(api)).toEqual(['page=0&size=20&name=pika&tag=electric']);
  });

  it('follows a tag to the records that have it', async () => {
    const user = userEvent.setup();
    localApi([PIKACHU, BULBASAUR]);
    const { router } = renderRoute('/local');

    await user.click(await screen.findByRole('link', { name: 'Filter by tag kanto' }));

    await waitFor(() => {
      expect(router.state.location.search).toBe('?tag=kanto');
    });
    expect(screen.getByLabelText('Tag')).toHaveValue('kanto');
  });

  it('says when nothing matches and clears the filters', async () => {
    const user = userEvent.setup();
    localApi([PIKACHU]);
    const { router } = renderRoute('/local?tag=legendary');

    expect(await screen.findByText('No records match these filters.')).toBeInTheDocument();
    await user.click(screen.getByRole('link', { name: 'Clear filters' }));

    expect(await screen.findByRole('link', { name: 'Pikachu' })).toBeInTheDocument();
    expect(router.state.location.search).toBe('');
    expect(screen.getByLabelText('Tag')).toHaveValue('');
  });

  it('says when the local Pokédex is empty and how to fill it', async () => {
    localApi([]);
    renderRoute('/local');

    expect(await screen.findByText('Your local Pokédex is empty.')).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Clear filters' })).not.toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Add a Pokémon' })).toHaveAttribute(
      'href',
      '/local/new',
    );
    expect(screen.getByRole('region', { name: 'Sync from PokeAPI' })).toBeInTheDocument();
  });

  it('shows an error with "Try again"', async () => {
    const user = userEvent.setup();
    let available = false;
    server.use(
      http.get('/api/v1/local-pokemon', () =>
        available
          ? HttpResponse.json({ content: [], page: 0, size: 20, totalElements: 0, totalPages: 0 })
          : problemResponse(500, 'internal-error', 'Something broke on our side.'),
      ),
    );
    renderRoute('/local');

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('Couldn’t load your Pokédex');
    expect(alert).toHaveTextContent('Something broke on our side.');
    available = true;
    await user.click(within(alert).getByRole('button', { name: 'Try again' }));

    expect(await screen.findByText('Your local Pokédex is empty.')).toBeInTheDocument();
  });
});

describe('Syncing from PokeAPI', () => {
  beforeEach(() => {
    saveSession(testSession());
  });
  afterEach(() => {
    sessionStorage.clear();
  });

  function syncPanel() {
    return within(screen.getByRole('region', { name: 'Sync from PokeAPI' }));
  }

  it('imports a Pokémon and opens its record', async () => {
    const user = userEvent.setup();
    const api = localApi([]);
    const { router } = renderRoute('/local');
    await screen.findByText('Your local Pokédex is empty.');

    await user.type(syncPanel().getByLabelText('Name or number'), 'Pikachu');
    await user.click(syncPanel().getByRole('button', { name: 'Sync' }));

    expect(await screen.findByText('Pikachu was imported from PokeAPI.')).toBeInTheDocument();
    const stored = [...api.records.values()].find((r) => r.pokeapiId === 25);
    expect(router.state.location.pathname).toBe(`/local/${String(stored?.id)}`);
    expect(api.writes().map((r) => `${r.method} ${r.path}`)).toEqual([
      'POST /api/v1/local-pokemon/sync/pikachu',
    ]);
  });

  it('shows the message once: not after leaving, not again on Back', async () => {
    const user = userEvent.setup();
    localApi([]);
    const { router } = renderRoute('/local');
    await screen.findByText('Your local Pokédex is empty.');
    await user.type(syncPanel().getByLabelText('Name or number'), 'pikachu{Enter}');
    expect(await screen.findByText('Pikachu was imported from PokeAPI.')).toBeInTheDocument();

    await user.click(screen.getByRole('link', { name: 'Back to My Pokédex' }));
    expect(await screen.findByRole('link', { name: 'Pikachu' })).toBeInTheDocument();
    expect(screen.queryByText('Pikachu was imported from PokeAPI.')).not.toBeInTheDocument();

    await router.navigate(-1);
    expect(await screen.findByRole('heading', { level: 1, name: 'Pikachu' })).toBeInTheDocument();
    expect(screen.queryByText('Pikachu was imported from PokeAPI.')).not.toBeInTheDocument();
  });

  it('re-syncs a stored Pokémon and says its proprietary fields were kept', async () => {
    const user = userEvent.setup();
    localApi([PIKACHU]);
    const { router } = renderRoute('/local');
    await screen.findByRole('link', { name: 'Pikachu' });

    await user.type(syncPanel().getByLabelText('Name or number'), '025{Enter}');

    expect(
      await screen.findByText(
        'Pikachu was re-synced from PokeAPI. Its proprietary fields were kept.',
      ),
    ).toBeInTheDocument();
    expect(router.state.location.pathname).toBe(`/local/${PIKACHU.id}`);
  });

  it('checks the name or number before asking the API', async () => {
    const user = userEvent.setup();
    const api = localApi([]);
    renderRoute('/local');
    await screen.findByText('Your local Pokédex is empty.');

    await user.type(syncPanel().getByLabelText('Name or number'), 'pika chu{Enter}');

    expect(
      await syncPanel().findByText('Enter a Pokédex number or a name such as pikachu'),
    ).toBeInTheDocument();
    expect(api.writes()).toEqual([]);
  });

  it('puts the API’s answer for an unknown Pokémon on the field', async () => {
    const user = userEvent.setup();
    localApi([]);
    renderRoute('/local');
    await screen.findByText('Your local Pokédex is empty.');

    const field = syncPanel().getByLabelText('Name or number');
    await user.type(field, 'missingno{Enter}');

    await waitFor(() => {
      expect(field).toHaveAccessibleDescription(
        expect.stringContaining("No Pokémon found for 'missingno'."),
      );
    });
    expect(field).toHaveValue('missingno');
  });

  it('says PokeAPI is down when the sync gets a 503', async () => {
    const user = userEvent.setup();
    localApi([]);
    server.use(
      http.post('/api/v1/local-pokemon/sync/:idOrName', () =>
        problemResponse(503, 'service-unavailable', 'PokeAPI is unavailable.'),
      ),
    );
    renderRoute('/local');
    await screen.findByText('Your local Pokédex is empty.');

    await user.type(syncPanel().getByLabelText('Name or number'), 'pikachu{Enter}');

    expect(await syncPanel().findByRole('alert')).toHaveTextContent(
      'PokeAPI is temporarily unavailable. Please try again in a moment.',
    );
  });

  it('syncs the first entries of the catalog and reports the counts', async () => {
    const user = userEvent.setup();
    const api = localApi([IVYSAUR]);
    renderRoute('/local');
    await screen.findByRole('link', { name: 'Ivysaur' });

    const howMany = syncPanel().getByLabelText('How many from the start of the catalog');
    expect(howMany).toHaveValue(10);
    await user.clear(howMany);
    await user.type(howMany, '3');
    await user.click(syncPanel().getByRole('button', { name: 'Sync from the start' }));

    expect(
      await syncPanel().findByText('Synced 3 Pokémon: 2 imported, 1 updated.'),
    ).toBeInTheDocument();
    expect(api.writes()[0]?.search.toString()).toBe('limit=3');
    expect(await screen.findByRole('link', { name: 'Venusaur' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Bulbasaur' })).toBeInTheDocument();
  });

  it('says PokeAPI is down when syncing the first entries gets a 503', async () => {
    const user = userEvent.setup();
    localApi([]);
    server.use(
      http.post('/api/v1/local-pokemon/sync', () =>
        problemResponse(503, 'service-unavailable', 'PokeAPI is unavailable.'),
      ),
    );
    renderRoute('/local');
    await screen.findByText('Your local Pokédex is empty.');

    await user.click(syncPanel().getByRole('button', { name: 'Sync from the start' }));

    expect(await syncPanel().findByRole('alert')).toHaveTextContent(
      'PokeAPI is temporarily unavailable. Please try again in a moment.',
    );
  });

  it('allows from 1 to 20 entries at a time', async () => {
    const user = userEvent.setup();
    const api = localApi([]);
    renderRoute('/local');
    await screen.findByText('Your local Pokédex is empty.');

    const howMany = syncPanel().getByLabelText('How many from the start of the catalog');
    await user.clear(howMany);
    await user.type(howMany, '21');
    await user.click(syncPanel().getByRole('button', { name: 'Sync from the start' }));

    await waitFor(() => {
      expect(howMany).toHaveAccessibleDescription(
        expect.stringContaining('Must be a whole number from 1 to 20'),
      );
    });
    expect(api.writes()).toEqual([]);
  });
});
