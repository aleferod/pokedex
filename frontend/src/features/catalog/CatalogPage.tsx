import { useRef } from 'react';
import { Link } from 'react-router';

import { useDocumentTitle } from '@/shared/hooks/useDocumentTitle';
import { Pagination } from '@/shared/ui/Pagination';
import { QueryError } from '@/shared/ui/QueryError';

import { CardSkeleton } from './CardSkeleton';
import { catalogSearch, PAGE_SIZES } from './catalogParams';
import { catalogReturnState } from './catalogReturn';
import { FindPokemonForm } from './FindPokemonForm';
import { PokemonCard } from './PokemonCard';
import { useCatalogPage } from './useCatalogPage';
import { useCatalogParams } from './useCatalogParams';

const SKELETONS = 8;
const total = new Intl.NumberFormat('en');

/** "17 Pokémon match “pika”" (one Pokémon matches). */
function matchCount(count: number, name: string): string {
  return `${total.format(count)} Pokémon ${count === 1 ? 'matches' : 'match'} “${name}”`;
}

/** US01: the catalog, one page at a time, with the page, size and search in the URL. */
export function CatalogPage() {
  useDocumentTitle('Catalog');
  const { page, size, name, setPage, setSize } = useCatalogParams();
  const query = useCatalogPage(page, size, name);
  const resultsHeading = useRef<HTMLHeadingElement>(null);

  const moveFocusToResults = () => {
    resultsHeading.current?.focus();
  };

  return (
    <section aria-labelledby="catalog-title" className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="space-y-1">
          <h1 id="catalog-title" className="text-3xl font-bold">
            Catalog
          </h1>
          <p className="text-slate-700">Every Pokémon from PokeAPI, a page at a time.</p>
        </div>
        <FindPokemonForm autoSearch />
      </div>

      <div className="flex items-baseline justify-between gap-4">
        <h2
          ref={resultsHeading}
          id="results-title"
          tabIndex={-1}
          className="text-xl font-semibold focus:outline-none"
        >
          Pokémon
        </h2>
        {name === '' ? (
          query.data && (
            <p className="text-sm text-slate-600">
              {total.format(query.data.totalElements)} in total
            </p>
          )
        ) : (
          <div className="flex items-baseline gap-3">
            <p aria-live="polite" className="text-sm text-slate-600">
              {query.data && !query.isPlaceholderData && matchCount(query.data.totalElements, name)}
            </p>
            <Link
              to={`/pokemon${catalogSearch(1, size)}`}
              className="text-sm font-semibold text-red-700 underline"
            >
              Clear search
            </Link>
          </div>
        )}
      </div>

      {query.isPending && (
        <>
          <p role="status" className="sr-only">
            Loading Pokémon…
          </p>
          <ul
            aria-hidden="true"
            className="grid grid-cols-1 gap-4 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4"
          >
            {Array.from({ length: SKELETONS }, (_, index) => (
              <li key={index}>
                <CardSkeleton />
              </li>
            ))}
          </ul>
        </>
      )}

      {query.isError && !query.data && (
        <QueryError
          title="Couldn’t load Pokémon"
          error={query.error}
          onRetry={() => {
            void query.refetch();
          }}
        />
      )}

      {query.data?.content.length === 0 && (
        <div className="space-y-2 rounded-md border border-slate-200 bg-white p-6 text-center">
          {query.data.totalElements === 0 ? (
            <p>{name === '' ? 'No Pokémon to show.' : `No Pokémon match “${name}”.`}</p>
          ) : (
            <>
              <p>There are no Pokémon on this page.</p>
              <Link
                to={`/pokemon${catalogSearch(query.data.totalPages, size, name)}`}
                className="font-semibold text-red-700 underline"
              >
                Go to the last page
              </Link>
            </>
          )}
        </div>
      )}

      {query.data && query.data.content.length > 0 && (
        <>
          <ul
            aria-labelledby="results-title"
            aria-busy={query.isPlaceholderData}
            className={`grid grid-cols-1 gap-4 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 ${
              query.isPlaceholderData ? 'opacity-60' : ''
            }`}
          >
            {query.data.content.map((entry) => (
              <li key={entry.id}>
                <PokemonCard entry={entry} linkState={catalogReturnState(page, size, name)} />
              </li>
            ))}
          </ul>
          <Pagination
            page={page}
            totalPages={query.data.totalPages}
            size={size}
            sizes={PAGE_SIZES}
            onPageChange={(next) => {
              setPage(next);
              moveFocusToResults();
            }}
            onSizeChange={(next) => {
              setSize(next);
              moveFocusToResults();
            }}
          />
        </>
      )}
    </section>
  );
}
