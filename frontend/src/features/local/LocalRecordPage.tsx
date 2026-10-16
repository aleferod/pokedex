import { useState } from 'react';
import { Link, useParams } from 'react-router';

import { useAuth } from '@/features/auth';
import { displayName } from '@/features/catalog';
import { StatsTable, TypeBadges } from '@/features/detail';
import { ApiError } from '@/shared/api';
import { formatDateTime, formatKilograms, formatMetres } from '@/shared/format';
import { useDocumentTitle } from '@/shared/hooks/useDocumentTitle';
import { FormAlert } from '@/shared/ui/form';
import { QueryError } from '@/shared/ui/QueryError';
import { queryErrorMessage } from '@/shared/ui/queryErrorMessage';

import { DeleteRecord } from './DeleteRecord';
import { RecordNotFound } from './RecordNotFound';
import { TagEditor } from './TagEditor';
import type { LocalPokemon } from './types';
import { useLocalRecord, useSyncOne } from './useLocal';

const backLink = 'inline-block font-semibold text-red-700 underline';

/** US03/US04: one local record, its PokeAPI data read-only and its proprietary fields editable. */
export function LocalRecordPage() {
  const { id = '' } = useParams();
  const query = useLocalRecord(id);
  const notFound = query.error instanceof ApiError && query.error.status === 404;
  useDocumentTitle(
    query.data
      ? `${displayName(query.data.name)} · My Pokédex`
      : notFound
        ? 'Record not found'
        : 'My Pokédex',
  );

  if (query.isPending) {
    return (
      <>
        <p role="status" className="sr-only">
          Loading the record…
        </p>
        <div
          aria-hidden="true"
          className="h-96 animate-pulse rounded-lg border border-slate-200 bg-white"
        />
      </>
    );
  }
  if (query.isError) {
    return notFound ? (
      <RecordNotFound />
    ) : (
      <QueryError
        title="Couldn’t load this record"
        error={query.error}
        onRetry={() => {
          void query.refetch();
        }}
      />
    );
  }
  return <LocalRecordView record={query.data} />;
}

function LocalRecordView({ record }: { record: LocalPokemon }) {
  const { session } = useAuth();
  const name = displayName(record.name);
  const image = record.imageUrl ?? record.spriteUrl;
  const localizedNames = Object.entries(record.localizedNames);

  return (
    <article aria-labelledby="record-name" className="space-y-6">
      <Link to="/local" className={backLink}>
        <span aria-hidden="true">← </span>Back to My Pokédex
      </Link>

      <header className="flex flex-wrap items-center gap-6">
        {image === null ? (
          <div
            role="img"
            aria-label="No image available"
            className="h-40 w-40 rounded-full bg-slate-100"
          />
        ) : (
          <img src={image} alt={name} width={160} height={160} className="h-40 w-40" />
        )}
        <div className="space-y-2">
          <p className="text-sm text-slate-500">#{record.pokeapiId}</p>
          <h1 id="record-name" className="text-3xl font-bold">
            {name}
          </h1>
          <div className="flex flex-wrap gap-3">
            <Link
              to={`/local/${record.id}/edit`}
              className="rounded-md bg-red-600 px-4 py-2 font-semibold text-white hover:bg-red-700"
            >
              Edit
            </Link>
            <Link
              to={`/pokemon/${String(record.pokeapiId)}`}
              className="py-2 font-semibold underline"
            >
              View on the public page
            </Link>
          </div>
        </div>
      </header>

      <div className="grid gap-6 lg:grid-cols-2">
        <UpstreamSection record={record} />

        <section
          aria-labelledby="own-title"
          className="space-y-4 rounded-lg border border-slate-200 bg-white p-4"
        >
          <h2 id="own-title" className="text-xl font-semibold">
            Your data
          </h2>
          <dl className="space-y-3">
            <Fact label="Region" value={record.region} fallback="No region yet" />
            <Fact
              label="Habitat notes"
              value={record.habitatNotes}
              fallback="No habitat notes yet"
            />
            <Fact label="Notes" value={record.notes} fallback="No notes yet" />
            <div>
              <dt className="text-sm text-slate-600">Localized names</dt>
              <dd>
                {localizedNames.length === 0 ? (
                  <p className="text-slate-600">No localized names yet</p>
                ) : (
                  <ul aria-label="Localized names" className="space-y-0.5">
                    {localizedNames.map(([locale, localName]) => (
                      <li key={locale}>
                        <span className="font-mono text-sm text-slate-600">{locale}</span>:{' '}
                        <span lang={locale}>{localName}</span>
                      </li>
                    ))}
                  </ul>
                )}
              </dd>
            </div>
            <div>
              <dt className="text-sm text-slate-600">Tags</dt>
              <dd>
                <TagEditor record={record} />
              </dd>
            </div>
          </dl>
        </section>
      </div>

      <p className="text-sm text-slate-600">
        Added by {record.createdBy} on{' '}
        <time dateTime={record.createdAt}>{formatDateTime(record.createdAt)}</time>. Updated{' '}
        <time dateTime={record.updatedAt}>{formatDateTime(record.updatedAt)}</time>.
      </p>

      {session?.user.role === 'ADMIN' && <DeleteRecord record={record} />}
    </article>
  );
}

function Fact({
  label,
  value,
  fallback,
}: {
  label: string;
  value: string | null;
  fallback: string;
}) {
  return (
    <div>
      <dt className="text-sm text-slate-600">{label}</dt>
      <dd className={value === null ? 'text-slate-600' : 'whitespace-pre-line'}>
        {value ?? fallback}
      </dd>
    </div>
  );
}

/** The PokeAPI half: read-only, refreshed only by a re-sync. */
function UpstreamSection({ record }: { record: LocalPokemon }) {
  const sync = useSyncOne();
  const [synced, setSynced] = useState<string | null>(null);

  const resync = async () => {
    setSynced(null);
    try {
      const { record: fresh } = await sync.mutateAsync(String(record.pokeapiId));
      setSynced(
        `${displayName(fresh.name)} was re-synced from PokeAPI. Its proprietary fields were kept.`,
      );
    } catch {
      // The error shows below, from the mutation's state.
    }
  };

  return (
    <section
      aria-labelledby="upstream-title"
      className="space-y-4 rounded-lg border border-slate-200 bg-white p-4"
    >
      <div className="space-y-1">
        <h2 id="upstream-title" className="text-xl font-semibold">
          From PokeAPI
        </h2>
        <p className="text-sm text-slate-600">
          Read-only: this comes from PokeAPI and changes only when the record is re-synced.
        </p>
      </div>
      <TypeBadges types={record.types} />
      <dl className="grid grid-cols-2 gap-x-6 gap-y-3">
        <div>
          <dt className="text-sm text-slate-600">Category</dt>
          <dd className="font-medium">{record.category ?? 'Unknown category'}</dd>
        </div>
        <div>
          <dt className="text-sm text-slate-600">Height</dt>
          <dd className="font-medium">{formatMetres(record.height)}</dd>
        </div>
        <div>
          <dt className="text-sm text-slate-600">Weight</dt>
          <dd className="font-medium">{formatKilograms(record.weight)}</dd>
        </div>
        <div>
          <dt className="text-sm text-slate-600">Abilities</dt>
          <dd className="font-medium">{record.abilities.join(', ')}</dd>
        </div>
      </dl>
      <StatsTable stats={record.stats} />
      <div className="flex flex-wrap items-center gap-3 border-t border-slate-200 pt-3">
        <p className="text-sm text-slate-600">
          Synced <time dateTime={record.syncedAt}>{formatDateTime(record.syncedAt)}</time>
        </p>
        <button
          type="button"
          disabled={sync.isPending}
          onClick={() => {
            void resync();
          }}
          className="rounded-md border border-slate-300 bg-white px-3 py-1.5 font-semibold hover:border-red-600 disabled:cursor-wait disabled:opacity-70"
        >
          {sync.isPending ? 'Re-syncing…' : 'Re-sync from PokeAPI'}
        </button>
      </div>
      {sync.isError && <FormAlert>{queryErrorMessage(sync.error)}</FormAlert>}
      <p role="status" className="text-sm text-green-800">
        {synced}
      </p>
    </section>
  );
}
