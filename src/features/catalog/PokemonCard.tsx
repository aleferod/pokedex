import { Link } from 'react-router';

import { formatKilograms } from '@/shared/format';

import type { CatalogReturnState } from './catalogReturn';
import { displayName } from './displayName';
import type { CatalogEntry } from './types';

/**
 * One catalog entry; the whole card links to the Pokémon's page, carrying the catalog place so the
 * detail page can lead back to it.
 */
export function PokemonCard({
  entry,
  linkState,
}: {
  entry: CatalogEntry;
  linkState: CatalogReturnState;
}) {
  const name = displayName(entry.name);
  return (
    <Link
      to={`/pokemon/${String(entry.id)}`}
      state={linkState}
      className="block h-full rounded-lg border border-slate-200 bg-white p-4 shadow-sm transition hover:shadow-md"
    >
      <article className="flex h-full flex-col gap-1">
        {entry.spriteUrl === null ? (
          <div
            role="img"
            aria-label="No sprite available"
            className="mx-auto h-24 w-24 rounded-full bg-slate-100"
          />
        ) : (
          <img
            src={entry.spriteUrl}
            alt={name}
            width={96}
            height={96}
            loading="lazy"
            className="mx-auto h-24 w-24 [image-rendering:pixelated]"
          />
        )}
        <p className="text-sm text-slate-500">#{entry.id}</p>
        <h3 className="text-lg font-semibold">{name}</h3>
        <p className="text-slate-700">{entry.category ?? 'Unknown category'}</p>
        <p className="text-slate-700">{formatKilograms(entry.weight)}</p>
        <ul aria-label="Abilities" className="mt-auto flex flex-wrap gap-1 pt-2">
          {entry.abilities.map((ability) => (
            <li key={ability} className="rounded bg-slate-100 px-2 py-0.5 text-sm text-slate-700">
              {ability}
            </li>
          ))}
        </ul>
      </article>
    </Link>
  );
}
