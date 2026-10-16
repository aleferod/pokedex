import { screen, waitFor, within } from '@testing-library/react';
import { userEvent } from '@testing-library/user-event';
import { delay, http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';

import { problemResponse } from '@/test/authApi';
import { pokemonDetail } from '@/test/pokemonDetails';
import { renderRoute } from '@/test/renderRoute';
import { server } from '@/test/server';

const TOTAL = 1351;

function entry(id: number, name: string, overrides: object = {}) {
  return {
    id,
    name,
    spriteUrl: `https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/${String(id)}.png`,
    category: 'Seed Pokémon',
    weight: 6.9,
    abilities: ['overgrow', 'chlorophyll'],
    ...overrides,
  };
}

const PAGES: Record<number, object[]> = {
  0: [
    entry(1, 'bulbasaur'),
    entry(2, 'ivysaur', { weight: 13 }),
    entry(10001, 'deoxys-attack', { spriteUrl: null, category: null, abilities: ['pressure'] }),
  ],
  1: [entry(21, 'spearow', { category: 'Tiny Bird Pokémon', abilities: ['keen-eye'] })],
  2: [entry(41, 'zubat', { category: 'Bat Pokémon', abilities: ['inner-focus'] })],
};

/** What a search can find, in Pokédex order (a few of PokeAPI's real names). */
const SEARCHABLE = [
  entry(1, 'bulbasaur'),
  entry(2, 'ivysaur', { weight: 13 }),
  entry(3, 'venusaur', { weight: 100 }),
  entry(25, 'pikachu', { category: 'Mouse Pokémon', weight: 6, abilities: ['static'] }),
  entry(26, 'raichu', { category: 'Mouse Pokémon', weight: 30, abilities: ['static'] }),
  entry(10080, 'pikachu-rock-star', { category: 'Mouse Pokémon', weight: 6 }),
];

/**
 * The catalog endpoint, recording every (page, size) it was asked for, and the name of a search:
 * with `name` it answers like pokedex-api, with the entries whose name contains it.
 */
function catalogApi(options: { delayPage?: number } = {}) {
  const requests: { page: number; size: number; name?: string }[] = [];
  server.use(
    http.get('/api/v1/pokemon', async ({ request }) => {
      const url = new URL(request.url);
      const page = Number(url.searchParams.get('page'));
      const size = Number(url.searchParams.get('size'));
      const name = url.searchParams.get('name');
      requests.push(name === null ? { page, size } : { page, size, name });
      if (options.delayPage === page) {
        await delay('infinite');
      }
      if (name !== null) {
        const matches = SEARCHABLE.filter((match) => match.name.includes(name));
        return HttpResponse.json({
          content: matches.slice(page * size, page * size + size),
          page,
          size,
          totalElements: matches.length,
          totalPages: Math.ceil(matches.length / size),
        });
      }
      return HttpResponse.json({
        content: PAGES[page] ?? [],
        page,
        size,
        totalElements: TOTAL,
        totalPages: Math.ceil(TOTAL / size),
      });
    }),
  );
  return requests;
}

/**
 * Where the finder leads: Pikachu's page, which loads its detail. Recording the identifiers
 * asked for shows which address was opened.
 */
function pikachuDetailApi() {
  const requests: string[] = [];
  server.use(
    http.get('/api/v1/pokemon/:idOrName', ({ params }) => {
      requests.push(String(params.idOrName));
      return HttpResponse.json(pokemonDetail(25, 'pikachu'));
    }),
  );
  return requests;
}

/**
 * Waits until the result cards are exactly these names and no longer busy: after the URL changes,
 * the previous results stay on screen until the new ones arrive.
 */
async function expectResults(names: string[]): Promise<void> {
  await waitFor(() => {
    const list = screen.getByRole('list', { name: 'Pokémon' });
    expect(list).toHaveAttribute('aria-busy', 'false');
    expect(
      within(list)
        .getAllByRole('heading')
        .map((heading) => heading.textContent),
    ).toEqual(names);
  });
}

function searches(requests: { name?: string }[]): string[] {
  return requests.flatMap((request) => (request.name === undefined ? [] : [request.name]));
}

/** Longer than the search's pause, to show that nothing happened. */
function afterThePause(): Promise<void> {
  return new Promise((resolve) => {
    setTimeout(resolve, 500);
  });
}

describe('catalog page', () => {
  it('shows the first page as cards with everything US01 asks for', async () => {
    const requests = catalogApi();
    renderRoute('/pokemon');

    const card = (await screen.findByRole('heading', { name: 'Bulbasaur' })).closest('a');
    expect(card).toHaveAttribute('href', '/pokemon/1');
    const bulbasaur = within(card as HTMLElement);
    expect(bulbasaur.getByRole('img', { name: 'Bulbasaur' })).toHaveAttribute(
      'src',
      'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/1.png',
    );
    expect(bulbasaur.getByText('#1')).toBeInTheDocument();
    expect(bulbasaur.getByText('Seed Pokémon')).toBeInTheDocument();
    expect(bulbasaur.getByText('6.9 kg')).toBeInTheDocument();
    expect(bulbasaur.getByText('overgrow')).toBeInTheDocument();
    expect(bulbasaur.getByText('chlorophyll')).toBeInTheDocument();
    expect(screen.getByText('13 kg')).toBeInTheDocument();
    expect(requests[0]).toEqual({ page: 0, size: 20 });
    expect(screen.getByRole('link', { name: 'Catalog' })).toHaveAttribute('href', '/pokemon');
    await waitFor(() => {
      expect(document.title).toBe('Catalog · Pokédex');
    });
  });

  it('shows a placeholder when an entry has no sprite and no category', async () => {
    catalogApi();
    renderRoute('/pokemon');

    const card = (await screen.findByRole('heading', { name: 'Deoxys-attack' })).closest('a');
    const deoxys = within(card as HTMLElement);
    expect(deoxys.getByRole('img', { name: 'No sprite available' })).toBeInTheDocument();
    expect(deoxys.getByText('Unknown category')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Bulbasaur' })).toBeInTheDocument();
  });

  it('pages forward and back through the URL and moves focus to the results', async () => {
    const user = userEvent.setup();
    const requests = catalogApi();
    const { router } = renderRoute('/pokemon');
    await screen.findByRole('heading', { name: 'Bulbasaur' });

    await user.click(screen.getByRole('button', { name: 'Next page' }));

    expect(await screen.findByRole('heading', { name: 'Spearow' })).toBeInTheDocument();
    expect(router.state.location.search).toBe('?page=2');
    expect(requests).toContainEqual({ page: 1, size: 20 });
    expect(screen.getByRole('navigation', { name: 'Pagination' })).toHaveTextContent(
      'Page 2 of 68',
    );
    expect(screen.getByRole('heading', { name: 'Pokémon' })).toHaveFocus();

    await user.click(screen.getByRole('button', { name: 'Previous page' }));

    expect(await screen.findByRole('heading', { name: 'Bulbasaur' })).toBeInTheDocument();
    expect(router.state.location.search).toBe('');
  });

  it('opens a shared link on the page and size it names', async () => {
    const requests = catalogApi();
    renderRoute('/pokemon?page=3&size=40');

    expect(await screen.findByRole('heading', { name: 'Zubat' })).toBeInTheDocument();
    expect(requests[0]).toEqual({ page: 2, size: 40 });
    expect(screen.getByRole('navigation', { name: 'Pagination' })).toHaveTextContent(
      'Page 3 of 34',
    );
  });

  it('falls back to page 1 and size 20 for nonsense in the URL', async () => {
    const requests = catalogApi();
    renderRoute('/pokemon?page=abc&size=7');

    await screen.findByRole('heading', { name: 'Bulbasaur' });
    expect(requests[0]).toEqual({ page: 0, size: 20 });
  });

  it('goes back to page 1 when the page size changes', async () => {
    const user = userEvent.setup();
    const requests = catalogApi();
    const { router } = renderRoute('/pokemon?page=3');
    await screen.findByRole('heading', { name: 'Zubat' });

    await user.selectOptions(screen.getByRole('combobox', { name: 'Per page' }), '40');

    await waitFor(() => {
      expect(router.state.location.search).toBe('?size=40');
    });
    expect(requests).toContainEqual({ page: 0, size: 40 });
  });

  it('shows skeletons and says it is loading on the first visit', async () => {
    catalogApi({ delayPage: 0 });
    renderRoute('/pokemon');

    expect(await screen.findByRole('status')).toHaveTextContent('Loading Pokémon…');
    expect(screen.getAllByTestId('card-skeleton').length).toBeGreaterThan(0);
  });

  it('keeps the current page on screen, marked busy, while the next one loads', async () => {
    const user = userEvent.setup();
    catalogApi({ delayPage: 1 });
    renderRoute('/pokemon');
    await screen.findByRole('heading', { name: 'Bulbasaur' });

    await user.click(screen.getByRole('button', { name: 'Next page' }));

    expect(screen.getByRole('heading', { name: 'Bulbasaur' })).toBeInTheDocument();
    expect(screen.getByRole('list', { name: 'Pokémon' })).toHaveAttribute('aria-busy', 'true');
  });

  it('explains an empty page past the end and links to the last page', async () => {
    catalogApi();
    renderRoute('/pokemon?page=500');

    expect(await screen.findByText('There are no Pokémon on this page.')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Go to the last page' })).toHaveAttribute(
      'href',
      '/pokemon?page=68',
    );
  });

  it('says PokeAPI is down on a 503 and loads again on "Try again"', async () => {
    const user = userEvent.setup();
    let calls = 0;
    server.use(
      http.get('/api/v1/pokemon', () => {
        calls += 1;
        // The first load (two attempts: one retry for a 5xx) fails; "Try again" succeeds.
        return calls <= 2
          ? problemResponse(503, 'service-unavailable', 'The Pokémon catalog is unavailable.')
          : HttpResponse.json({
              content: PAGES[0],
              page: 0,
              size: 20,
              totalElements: TOTAL,
              totalPages: 68,
            });
      }),
    );
    renderRoute('/pokemon');

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('Couldn’t load Pokémon');
    expect(alert).toHaveTextContent(
      'PokeAPI is temporarily unavailable. Please try again in a moment.',
    );
    await user.click(within(alert).getByRole('button', { name: 'Try again' }));

    expect(await screen.findByRole('heading', { name: 'Bulbasaur' })).toBeInTheDocument();
  });

  it('shows the API message for other failures', async () => {
    server.use(http.get('/api/v1/pokemon', () => new HttpResponse('oops', { status: 500 })));
    renderRoute('/pokemon');

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'The server answered with an error (500). Please try again.',
    );
  });

  it('prefetches the next page once a page has loaded', async () => {
    const requests = catalogApi();
    renderRoute('/pokemon');
    await screen.findByRole('heading', { name: 'Bulbasaur' });

    await waitFor(() => {
      expect(requests).toContainEqual({ page: 1, size: 20 });
    });
  });

  // The finder's tests wait for the Pokémon's page itself: it loads its detail, so ending at the
  // address alone would leave that request racing the test's cleanup.
  it('searches at once when a name is submitted', async () => {
    const user = userEvent.setup();
    const requests = catalogApi();
    const { router } = renderRoute('/pokemon');

    await user.type(await screen.findByLabelText('Find a Pokémon'), 'Pikachu');
    await user.click(screen.getByRole('button', { name: 'Find' }));

    await waitFor(() => {
      expect(router.state.location.search).toBe('?name=pikachu');
    });
    await expectResults(['Pikachu', 'Pikachu-rock-star']);
    expect(requests).toContainEqual({ page: 0, size: 20, name: 'pikachu' });
    expect(router.state.location.pathname).toBe('/pokemon');
  });

  it('reads a Pokédex number with leading zeros', async () => {
    const user = userEvent.setup();
    catalogApi();
    const details = pikachuDetailApi();
    const { router } = renderRoute('/pokemon');

    await user.type(await screen.findByLabelText('Find a Pokémon'), '025{Enter}');

    expect(await screen.findByRole('heading', { level: 1, name: 'Pikachu' })).toBeInTheDocument();
    expect(router.state.location.pathname).toBe('/pokemon/25');
    expect(details).toEqual(['25']);
  });

  it('asks for at least 3 letters or a number when too little is submitted', async () => {
    const user = userEvent.setup();
    const requests = catalogApi();
    const { router } = renderRoute('/pokemon');

    await user.type(await screen.findByLabelText('Find a Pokémon'), 'pi{Enter}');

    const field = screen.getByLabelText('Find a Pokémon');
    await waitFor(() => {
      expect(field).toHaveAttribute('aria-invalid', 'true');
    });
    expect(field).toHaveAccessibleDescription(
      expect.stringContaining('Type at least 3 letters of a name, or a Pokédex number'),
    );
    expect(router.state.location.search).toBe('');
    expect(searches(requests)).toEqual([]);
  });

  it('is one click away from the home page', async () => {
    renderRoute('/');

    expect(await screen.findByRole('link', { name: 'Browse the catalog' })).toHaveAttribute(
      'href',
      '/pokemon',
    );
  });
});

describe('searching the catalog by name', () => {
  it('searches after a pause once 3 characters are typed, keeping focus in the field', async () => {
    const user = userEvent.setup();
    const requests = catalogApi();
    const { router } = renderRoute('/pokemon');
    const field = await screen.findByLabelText('Find a Pokémon');

    await user.type(field, 'Pika');

    await waitFor(() => {
      expect(router.state.location.search).toBe('?name=pika');
    });
    await expectResults(['Pikachu', 'Pikachu-rock-star']);
    expect(screen.getByText('2 Pokémon match “pika”')).toBeInTheDocument();
    expect(searches(requests)).toEqual(['pika']);
    expect(requests).toContainEqual({ page: 0, size: 20, name: 'pika' });
    expect(field).toHaveFocus();
    expect(field).toHaveValue('Pika');
  });

  it('does not search with fewer than 3 characters', async () => {
    const user = userEvent.setup();
    const requests = catalogApi();
    const { router } = renderRoute('/pokemon');

    await user.type(await screen.findByLabelText('Find a Pokémon'), 'pi');
    await afterThePause();

    expect(router.state.location.search).toBe('');
    expect(searches(requests)).toEqual([]);
    expect(screen.getByRole('heading', { name: 'Bulbasaur' })).toBeInTheDocument();
  });

  it('explains characters the API would not accept, without searching', async () => {
    const user = userEvent.setup();
    const requests = catalogApi();
    renderRoute('/pokemon');
    const field = await screen.findByLabelText('Find a Pokémon');

    await user.type(field, 'pika!');
    await afterThePause();

    expect(field).toHaveAttribute('aria-invalid', 'true');
    expect(field).toHaveAccessibleDescription(
      expect.stringContaining('Use letters, digits, hyphens or spaces'),
    );
    expect(searches(requests)).toEqual([]);
  });

  it('says when nothing matches, and "Clear search" shows the catalog again', async () => {
    const user = userEvent.setup();
    catalogApi();
    const { router } = renderRoute('/pokemon');
    const field = await screen.findByLabelText('Find a Pokémon');

    await user.type(field, 'zzz');

    expect(await screen.findByText('No Pokémon match “zzz”.')).toBeInTheDocument();
    await user.click(screen.getByRole('link', { name: 'Clear search' }));

    expect(await screen.findByRole('heading', { name: 'Bulbasaur' })).toBeInTheDocument();
    expect(router.state.location.search).toBe('');
    expect(field).toHaveValue('');
  });

  it('keeps the page size and starts from page 1', async () => {
    const user = userEvent.setup();
    const requests = catalogApi();
    const { router } = renderRoute('/pokemon?page=3&size=40');

    await user.type(await screen.findByLabelText('Find a Pokémon'), 'saur');

    await waitFor(() => {
      expect(router.state.location.search).toBe('?size=40&name=saur');
    });
    await waitFor(() => {
      expect(requests).toContainEqual({ page: 0, size: 40, name: 'saur' });
    });
    await expectResults(['Bulbasaur', 'Ivysaur', 'Venusaur']);
  });

  it('opens a shared search with the field filled in', async () => {
    const requests = catalogApi();
    renderRoute('/pokemon?name=pika&size=40');

    await expectResults(['Pikachu', 'Pikachu-rock-star']);
    expect(screen.getByLabelText('Find a Pokémon')).toHaveValue('pika');
    expect(requests[0]).toEqual({ page: 0, size: 40, name: 'pika' });
  });

  it('follows the address when it changes, as with Back', async () => {
    const user = userEvent.setup();
    catalogApi();
    const { router } = renderRoute('/pokemon?name=saur');
    await expectResults(['Bulbasaur', 'Ivysaur', 'Venusaur']);

    await user.click(screen.getByRole('link', { name: 'Clear search' }));
    await screen.findByRole('heading', { name: 'Bulbasaur' });
    await router.navigate(-1);

    await waitFor(() => {
      expect(screen.getByLabelText('Find a Pokémon')).toHaveValue('saur');
    });
    await expectResults(['Bulbasaur', 'Ivysaur', 'Venusaur']);
  });

  it('shows the whole catalog again when the field is emptied', async () => {
    const user = userEvent.setup();
    catalogApi();
    const { router } = renderRoute('/pokemon?name=pika');
    await expectResults(['Pikachu', 'Pikachu-rock-star']);

    await user.clear(screen.getByLabelText('Find a Pokémon'));

    await waitFor(() => {
      expect(router.state.location.search).toBe('');
    });
    expect(await screen.findByRole('heading', { name: 'Bulbasaur' })).toBeInTheDocument();
  });
});
