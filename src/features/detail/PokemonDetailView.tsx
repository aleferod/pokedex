import { useEffect, useRef } from 'react';
import { useLocation } from 'react-router';

import { type CatalogReturnState, displayName } from '@/features/catalog';
import { formatKilograms, formatMetres } from '@/shared/format';

import { EvolutionTree } from './EvolutionTree';
import { StatsTable } from './StatsTable';
import { TypeBadges } from './TypeBadges';
import type { PokemonDetail } from './types';

/** A loaded Pokémon: artwork, identity, description, measures, base stats and evolution family. */
export function PokemonDetailView({
  pokemon,
  linkState,
}: {
  pokemon: PokemonDetail;
  linkState: CatalogReturnState;
}) {
  const name = displayName(pokemon.name);
  const heading = useRef<HTMLHeadingElement>(null);
  // Reached inside the app (a card, the chain): start keyboard and screen-reader users at the new
  // name. The link that was followed is gone, so focus would otherwise fall back to the document.
  const navigatedInApp = useLocation().key !== 'default';

  useEffect(() => {
    if (navigatedInApp) {
      heading.current?.focus();
    }
  }, [pokemon.id, navigatedInApp]);

  return (
    <article aria-labelledby="pokemon-name" className="space-y-6">
      <div className="grid gap-6 md:grid-cols-[minmax(0,1fr)_minmax(0,1.2fr)]">
        <div className="flex items-center justify-center rounded-lg border border-slate-200 bg-white p-6">
          {pokemon.imageUrl === null ? (
            <div
              role="img"
              aria-label="No image available"
              className="aspect-square w-full max-w-64 rounded-full bg-slate-100"
            />
          ) : (
            <img
              src={pokemon.imageUrl}
              alt={name}
              width={256}
              height={256}
              className="aspect-square w-full max-w-64"
            />
          )}
        </div>

        <div className="space-y-4">
          <header className="space-y-2">
            <p className="text-sm text-slate-500">#{pokemon.id}</p>
            <h1
              ref={heading}
              id="pokemon-name"
              tabIndex={-1}
              className="text-3xl font-bold focus:outline-none"
            >
              {name}
            </h1>
            <TypeBadges types={pokemon.types} />
          </header>
          <p className="text-slate-800">{pokemon.description ?? 'No description available.'}</p>
          <dl className="grid grid-cols-2 gap-x-6 gap-y-3 rounded-lg border border-slate-200 bg-white p-4">
            <div>
              <dt className="text-sm text-slate-600">Category</dt>
              <dd className="font-medium">{pokemon.category ?? 'Unknown category'}</dd>
            </div>
            <div>
              <dt className="text-sm text-slate-600">Height</dt>
              <dd className="font-medium">{formatMetres(pokemon.height)}</dd>
            </div>
            <div>
              <dt className="text-sm text-slate-600">Weight</dt>
              <dd className="font-medium">{formatKilograms(pokemon.weight)}</dd>
            </div>
            <div>
              <dt className="text-sm text-slate-600">Abilities</dt>
              <dd>
                <ul aria-label="Abilities" className="flex flex-wrap gap-1">
                  {pokemon.abilities.map((ability) => (
                    <li
                      key={ability}
                      className="rounded bg-slate-100 px-2 py-0.5 text-sm text-slate-700"
                    >
                      {ability}
                    </li>
                  ))}
                </ul>
              </dd>
            </div>
          </dl>
        </div>
      </div>

      <div className="grid gap-6 md:grid-cols-2">
        <div className="rounded-lg border border-slate-200 bg-white p-4">
          <StatsTable stats={pokemon.stats} />
        </div>
        <section
          aria-labelledby="evolution-title"
          className="space-y-3 rounded-lg border border-slate-200 bg-white p-4"
        >
          <h2 id="evolution-title" className="text-xl font-semibold">
            Evolution
          </h2>
          <EvolutionTree
            chain={pokemon.evolutionChain}
            currentId={pokemon.id}
            linkState={linkState}
          />
        </section>
      </div>
    </article>
  );
}
