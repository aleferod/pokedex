import { useRef } from 'react';
import { Link } from 'react-router';

import { PAGE_SIZES } from '@/features/catalog';
import { useDocumentTitle } from '@/shared/hooks/useDocumentTitle';
import { Pagination } from '@/shared/ui/Pagination';
import { QueryError } from '@/shared/ui/QueryError';

import { LocalFilters } from './LocalFilters';
import { localSearch, useLocalParams } from './localParams';
import { LocalRecordCard } from './LocalRecordCard';
import { SyncPanel } from './SyncPanel';
import { useLocalPage } from './useLocal';

const SKELETONS = 4;

/** US03/US04: the local Pokédex a page at a time, filtered by name and tag in the URL. */
export function LocalListPage() {
  useDocumentTitle('My Pokédex');
  const { params, setPage, setSize, setFilters } = useLocalParams();
  const query = useLocalPage(params);
  const resultsHeading = useRef<HTMLHeadingElement>(null);
  const filtered = params.name !== '' || params.tag !== '';
  const href = (changes: Partial<typeof params>) =>
    `/local${localSearch({ ...params, page: 1, ...changes })}`;

  const moveFocusToResults = () => {
    resultsHeading.current?.focus();
  };

  return (
    <section aria-labelledby="local-title" className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="space-y-1">
          <h1 id="local-title" className="text-3xl font-bold">
            My Pokédex
          </h1>
          <p className="text-slate-700">
            Pokémon copied from PokeAPI, with your own names, notes and tags.
          </p>
        </div>
        <Link
          to="/local/new"
          className="rounded-md bg-red-600 px-4 py-2 font-semibold text-white hover:bg-red-700"
        >
          Add a Pokémon
        </Link>
      </div>

      <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_20rem]">
        <div className="space-y-4">
          <LocalFilters
            key={`${params.name}\n${params.tag}`}
            name={params.name}
            tag={params.tag}
            clearHref={href({ name: '', tag: '' })}
            onApply={setFilters}
          />

          <div className="flex items-baseline justify-between gap-4">
            <h2
              ref={resultsHeading}
              id="records-title"
              tabIndex={-1}
              className="text-xl font-semibold focus:outline-none"
            >
              Records
            </h2>
            {query.data && (
              <p className="text-sm text-slate-600">
                {query.data.totalElements === 1
                  ? '1 record'
                  : `${String(query.data.totalElements)} records`}
              </p>
            )}
          </div>

          {query.isPending && (
            <>
              <p role="status" className="sr-only">
                Loading your Pokédex…
              </p>
              <ul aria-hidden="true" className="grid gap-4 md:grid-cols-2">
                {Array.from({ length: SKELETONS }, (_, index) => (
                  <li
                    key={index}
                    className="h-32 animate-pulse rounded-lg border border-slate-200 bg-white"
                  />
                ))}
              </ul>
            </>
          )}

          {query.isError && !query.data && (
            <QueryError
              title="Couldn’t load your Pokédex"
              error={query.error}
              onRetry={() => {
                void query.refetch();
              }}
            />
          )}

          {query.data?.content.length === 0 && (
            <div className="space-y-2 rounded-md border border-slate-200 bg-white p-6 text-center">
              {query.data.totalElements > 0 ? (
                <>
                  <p>There are no records on this page.</p>
                  <Link
                    to={href({ page: query.data.totalPages })}
                    className="font-semibold text-red-700 underline"
                  >
                    Go to the last page
                  </Link>
                </>
              ) : filtered ? (
                <p>No records match these filters.</p>
              ) : (
                <>
                  <p className="font-semibold">Your local Pokédex is empty.</p>
                  <p className="text-slate-700">Sync a Pokémon from PokeAPI, or add one.</p>
                </>
              )}
            </div>
          )}

          {query.data && query.data.content.length > 0 && (
            <>
              <ul
                aria-labelledby="records-title"
                aria-busy={query.isPlaceholderData}
                className={`grid gap-4 md:grid-cols-2 ${query.isPlaceholderData ? 'opacity-60' : ''}`}
              >
                {query.data.content.map((record) => (
                  <li key={record.id}>
                    <LocalRecordCard record={record} tagHref={(tag) => href({ name: '', tag })} />
                  </li>
                ))}
              </ul>
              <Pagination
                page={params.page}
                totalPages={query.data.totalPages}
                size={params.size}
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
        </div>

        <SyncPanel />
      </div>
    </section>
  );
}
