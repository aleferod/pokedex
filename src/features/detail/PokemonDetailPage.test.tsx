import { screen, waitFor, within } from '@testing-library/react';
import { userEvent } from '@testing-library/user-event';
import { delay, http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';

import { problemResponse } from '@/test/authApi';
import { ARTWORK, pokemonDetail, stage } from '@/test/pokemonDetails';
import { renderRoute } from '@/test/renderRoute';
import { server } from '@/test/server';

const BULBASAUR_CHAIN = stage(1, 'bulbasaur', null, [
  stage(2, 'ivysaur', 'Level 16', [stage(3, 'venusaur', 'Level 32')]),
]);
const PIKACHU_CHAIN = stage(172, 'pichu', null, [
  stage(25, 'pikachu', 'Level up with high friendship', [stage(26, 'raichu', 'Use Thunder Stone')]),
]);
const EEVEE_CHAIN = stage(133, 'eevee', null, [
  stage(134, 'vaporeon', 'Use Water Stone'),
  stage(135, 'jolteon', 'Use Thunder Stone'),
  stage(136, 'flareon', 'Use Fire Stone'),
  stage(196, 'espeon', 'Level up with high friendship during the day'),
  stage(197, 'umbreon', 'Level up with high friendship at night'),
  stage(470, 'leafeon', 'Use Leaf Stone'),
  stage(471, 'glaceon', 'Use Ice Stone'),
  stage(700, 'sylveon', 'Level up with high affection knowing a Fairy move'),
]);

const POKEMON = [
  pokemonDetail(1, 'bulbasaur', BULBASAUR_CHAIN),
  pokemonDetail(2, 'ivysaur', BULBASAUR_CHAIN, {
    description: 'When the bulb on its back grows large, it appears to lose the ability to stand.',
    height: 1,
    weight: 13,
  }),
  pokemonDetail(25, 'pikachu', PIKACHU_CHAIN, { category: 'Mouse Pokémon', types: ['electric'] }),
  pokemonDetail(133, 'eevee', EEVEE_CHAIN, { category: 'Evolution Pokémon', types: ['normal'] }),
  pokemonDetail(132, 'ditto', stage(132, 'ditto', null), { types: ['normal'] }),
  pokemonDetail(10001, 'deoxys-attack', stage(10001, 'deoxys-attack', null), {
    imageUrl: null,
    category: null,
    description: null,
  }),
];

/** The detail endpoint over the fixtures above, recording every identifier it was asked for. */
function detailApi() {
  const requests: string[] = [];
  server.use(
    http.get('/api/v1/pokemon/:idOrName', ({ params }) => {
      const idOrName = String(params.idOrName);
      requests.push(idOrName);
      const pokemon = POKEMON.find((p) => String(p.id) === idOrName || p.name === idOrName);
      return pokemon
        ? HttpResponse.json(pokemon)
        : problemResponse(404, 'not-found', `No Pokémon found for '${idOrName}'`);
    }),
  );
  return requests;
}

async function findName(name: string) {
  return screen.findByRole('heading', { level: 1, name });
}

function statRow(table: HTMLElement, label: string) {
  return within(table).getByRole('rowheader', { name: label }).closest('tr') as HTMLElement;
}

function evolution() {
  return screen.getByRole('region', { name: 'Evolution' });
}

describe('Pokémon detail page', () => {
  it('shows everything US02 asks for', async () => {
    const requests = detailApi();
    renderRoute('/pokemon/1');

    await findName('Bulbasaur');
    expect(screen.getByRole('img', { name: 'Bulbasaur' })).toHaveAttribute(
      'src',
      `${ARTWORK}/official-artwork/1.png`,
    );
    expect(screen.getByText('#1')).toBeInTheDocument();
    const types = within(screen.getByRole('list', { name: 'Types' })).getAllByRole('listitem');
    expect(types.map((type) => type.textContent)).toEqual(['grass', 'poison']);
    expect(screen.getByText('Seed Pokémon')).toBeInTheDocument();
    expect(
      screen.getByText('A strange seed was planted on its back at birth.'),
    ).toBeInTheDocument();
    expect(screen.getByText('0.7 m')).toBeInTheDocument();
    expect(screen.getByText('6.9 kg')).toBeInTheDocument();
    const abilities = within(screen.getByRole('list', { name: 'Abilities' })).getAllByRole(
      'listitem',
    );
    expect(abilities.map((ability) => ability.textContent)).toEqual(['overgrow', 'chlorophyll']);
    expect(requests).toEqual(['1']);
    await waitFor(() => {
      expect(document.title).toBe('Bulbasaur · Pokédex');
    });
  });

  it('shows the base stats as a table with their total', async () => {
    detailApi();
    renderRoute('/pokemon/1');
    await findName('Bulbasaur');

    const table = screen.getByRole('table', { name: 'Base stats' });
    const expected: [string, string][] = [
      ['HP', '45'],
      ['Attack', '49'],
      ['Defense', '49'],
      ['Special Attack', '65'],
      ['Special Defense', '65'],
      ['Speed', '45'],
      ['Total', '318'],
    ];
    for (const [label, value] of expected) {
      expect(within(statRow(table, label)).getByRole('cell', { name: value })).toBeInTheDocument();
    }
  });

  it('shows placeholders when the artwork, category and description are missing', async () => {
    detailApi();
    renderRoute('/pokemon/10001');
    await findName('Deoxys-attack');

    expect(screen.getByRole('img', { name: 'No image available' })).toBeInTheDocument();
    expect(screen.getByText('Unknown category')).toBeInTheDocument();
    expect(screen.getByText('No description available.')).toBeInTheDocument();
  });

  it('shows the evolution chain from the base form, with conditions and the current stage', async () => {
    detailApi();
    renderRoute('/pokemon/pikachu');
    await findName('Pikachu');

    const chain = evolution();
    expect(chain).toHaveTextContent(
      /Pichu.*Level up with high friendship.*Pikachu.*Use Thunder Stone.*Raichu/,
    );
    expect(within(chain).getByRole('link', { name: 'Pichu' })).toHaveAttribute(
      'href',
      '/pokemon/172',
    );
    const pikachu = within(chain).getByRole('link', { name: 'Pikachu' });
    expect(pikachu).toHaveAttribute('href', '/pokemon/25');
    expect(pikachu).toHaveAttribute('aria-current', 'page');
    const raichu = within(chain).getByRole('link', { name: 'Raichu' });
    expect(raichu).not.toHaveAttribute('aria-current');
    expect(
      within(screen.getByRole('list', { name: 'Evolutions of Pikachu' })).getByRole('link'),
    ).toBe(raichu);
  });

  it('shows every branch of a branching family', async () => {
    detailApi();
    renderRoute('/pokemon/eevee');
    await findName('Eevee');

    const branches = within(
      within(evolution()).getByRole('list', { name: 'Evolutions of Eevee' }),
    ).getAllByRole('listitem');
    expect(branches).toHaveLength(8);
    expect(branches[0]).toHaveTextContent('Use Water Stone');
    expect(branches[0]).toHaveTextContent('Vaporeon');
    expect(branches[7]).toHaveTextContent('Level up with high affection knowing a Fairy move');
    expect(branches[7]).toHaveTextContent('Sylveon');
  });

  it('says when a Pokémon does not evolve', async () => {
    detailApi();
    renderRoute('/pokemon/ditto');
    await findName('Ditto');

    expect(within(evolution()).getByText('Ditto does not evolve.')).toBeInTheDocument();
    expect(within(evolution()).queryByRole('link')).not.toBeInTheDocument();
  });

  it('follows the chain to another Pokémon and moves focus to its name', async () => {
    const user = userEvent.setup();
    const requests = detailApi();
    const { router } = renderRoute('/pokemon/1');
    await findName('Bulbasaur');

    await user.click(within(evolution()).getByRole('link', { name: 'Ivysaur' }));

    const ivysaur = await findName('Ivysaur');
    expect(router.state.location.pathname).toBe('/pokemon/2');
    expect(requests).toEqual(['1', '2']);
    expect(screen.getByText('13 kg')).toBeInTheDocument();
    await waitFor(() => {
      expect(ivysaur).toHaveFocus();
    });
    await waitFor(() => {
      expect(document.title).toBe('Ivysaur · Pokédex');
    });
  });

  it('leaves focus alone when the page is opened directly', async () => {
    detailApi();
    renderRoute('/pokemon/1');

    expect(await findName('Bulbasaur')).not.toHaveFocus();
  });

  it('shows a skeleton and says it is loading', async () => {
    server.use(
      http.get('/api/v1/pokemon/:idOrName', async () => {
        await delay('infinite');
        return HttpResponse.json({});
      }),
    );
    renderRoute('/pokemon/1');

    expect(await screen.findByRole('status')).toHaveTextContent('Loading Pokémon…');
    expect(screen.getByTestId('detail-skeleton')).toBeInTheDocument();
    expect(document.title).toBe('Pokémon · Pokédex');
  });

  it.each([
    ['/pokemon/Pikachu', '/pokemon/pikachu', 'pikachu'],
    ['/pokemon/025', '/pokemon/25', '25'],
  ])('replaces %s with its canonical address', async (path, canonical, requested) => {
    const requests = detailApi();
    const { router } = renderRoute(path);

    await findName('Pikachu');
    expect(router.state.location.pathname).toBe(canonical);
    expect(router.state.historyAction).toBe('REPLACE');
    expect(requests).toEqual([requested]);
  });

  it('rejects an identifier the API would reject, without asking it', async () => {
    const requests = detailApi();
    renderRoute('/pokemon/pika%20chu');

    expect(await findName('Pokémon not found')).toBeInTheDocument();
    expect(screen.getByText('“pika chu” isn’t a Pokédex number or a name.')).toBeInTheDocument();
    expect(screen.getByRole('search')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Back to the catalog' })).toHaveAttribute(
      'href',
      '/pokemon',
    );
    expect(requests).toEqual([]);
    await waitFor(() => {
      expect(document.title).toBe('Pokémon not found · Pokédex');
    });
  });

  it('says when no Pokémon has that name', async () => {
    detailApi();
    renderRoute('/pokemon/missingno');

    expect(await findName('Pokémon not found')).toBeInTheDocument();
    expect(screen.getByText('There is no Pokémon called “missingno”.')).toBeInTheDocument();
    expect(screen.getByRole('search')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Back to the catalog' })).toBeInTheDocument();
    await waitFor(() => {
      expect(document.title).toBe('Pokémon not found · Pokédex');
    });
  });

  it('says when no Pokémon has that number', async () => {
    detailApi();
    renderRoute('/pokemon/99999');

    expect(await findName('Pokémon not found')).toBeInTheDocument();
    expect(screen.getByText('There is no Pokémon number 99999.')).toBeInTheDocument();
  });

  it('says PokeAPI is down on a 503 and loads again on "Try again"', async () => {
    const user = userEvent.setup();
    let available = false;
    server.use(
      http.get('/api/v1/pokemon/:idOrName', () =>
        available
          ? HttpResponse.json(POKEMON[0])
          : problemResponse(503, 'service-unavailable', 'The Pokémon catalog is unavailable.'),
      ),
    );
    renderRoute('/pokemon/1');

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('Couldn’t load this Pokémon');
    expect(alert).toHaveTextContent(
      'PokeAPI is temporarily unavailable. Please try again in a moment.',
    );
    available = true;
    await user.click(within(alert).getByRole('button', { name: 'Try again' }));

    expect(await findName('Bulbasaur')).toBeInTheDocument();
  });

  it('leads back to the catalog page it came from, also after following the chain', async () => {
    const user = userEvent.setup();
    detailApi();
    server.use(
      http.get('/api/v1/pokemon', () =>
        HttpResponse.json({
          content: [
            {
              id: 1,
              name: 'bulbasaur',
              spriteUrl: null,
              category: 'Seed Pokémon',
              weight: 6.9,
              abilities: ['overgrow'],
            },
          ],
          page: 2,
          size: 40,
          totalElements: 1351,
          totalPages: 34,
        }),
      ),
    );
    const { router } = renderRoute('/pokemon?page=3&size=40');

    const card = (await screen.findByRole('heading', { name: 'Bulbasaur' })).closest('a');
    await user.click(card as HTMLElement);
    await findName('Bulbasaur');
    expect(screen.getByRole('link', { name: 'Back to the catalog' })).toHaveAttribute(
      'href',
      '/pokemon?page=3&size=40',
    );

    await user.click(within(evolution()).getByRole('link', { name: 'Ivysaur' }));
    await findName('Ivysaur');
    await user.click(screen.getByRole('link', { name: 'Back to the catalog' }));

    await waitFor(() => {
      expect(router.state.location.pathname).toBe('/pokemon');
    });
    expect(router.state.location.search).toBe('?page=3&size=40');
  });

  it('leads back to the first catalog page when opened directly', async () => {
    detailApi();
    renderRoute('/pokemon/25');
    await findName('Pikachu');

    expect(screen.getByRole('link', { name: 'Back to the catalog' })).toHaveAttribute(
      'href',
      '/pokemon',
    );
  });
});
