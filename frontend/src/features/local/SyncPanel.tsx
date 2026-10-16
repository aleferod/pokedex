import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useNavigate } from 'react-router';
import { z } from 'zod';

import { displayName, IDENTIFIER_RULE, parsePokemonIdentifier } from '@/features/catalog';
import { ApiError } from '@/shared/api';
import { withFlash } from '@/shared/ui/flash';
import { fieldMessages, FormAlert, TextField } from '@/shared/ui/form';
import { queryErrorMessage } from '@/shared/ui/queryErrorMessage';

import { useSyncFirst, useSyncOne } from './useLocal';

const MAX_BULK = 20;
const BULK_RULE = `must be a whole number from 1 to ${String(MAX_BULK)}`;

const oneSchema = z.object({
  identifier: z.string().refine((value) => parsePokemonIdentifier(value) !== null, IDENTIFIER_RULE),
});

const firstSchema = z.object({
  limit: z.string().refine((value) => {
    const trimmed = value.trim();
    return /^\d{1,2}$/.test(trimmed) && Number(trimmed) >= 1 && Number(trimmed) <= MAX_BULK;
  }, BULK_RULE),
});

const buttonClass =
  'rounded-md bg-red-600 px-4 py-2 font-semibold text-white hover:bg-red-700 disabled:cursor-wait disabled:opacity-70';

/** US03: copy Pokémon from PokeAPI into the local Pokédex, one by name or number, or the first N. */
export function SyncPanel() {
  return (
    <section
      aria-labelledby="sync-title"
      className="space-y-5 rounded-lg border border-slate-200 bg-white p-4"
    >
      <div className="space-y-1">
        <h2 id="sync-title" className="text-xl font-semibold">
          Sync from PokeAPI
        </h2>
        <p className="text-sm text-slate-600">
          Syncing a stored Pokémon refreshes its PokeAPI data and keeps your names, notes and tags.
        </p>
      </div>
      <SyncOneForm />
      <SyncFirstForm />
    </section>
  );
}

function SyncOneForm() {
  const navigate = useNavigate();
  const sync = useSyncOne();
  const [formError, setFormError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm({ resolver: zodResolver(oneSchema), defaultValues: { identifier: '' } });

  const onSubmit = handleSubmit(async ({ identifier }) => {
    setFormError(null);
    try {
      const { record, imported } = await sync.mutateAsync(
        parsePokemonIdentifier(identifier) ?? identifier,
      );
      const name = displayName(record.name);
      const message = imported
        ? `${name} was imported from PokeAPI.`
        : `${name} was re-synced from PokeAPI. Its proprietary fields were kept.`;
      void navigate(`/local/${record.id}`, { state: withFlash(message) });
    } catch (error) {
      if (error instanceof ApiError && error.status === 404) {
        setError('identifier', { type: 'server', message: error.message });
      } else {
        setFormError(queryErrorMessage(error));
      }
    }
  });

  return (
    <form
      noValidate
      onSubmit={(event) => {
        void onSubmit(event);
      }}
      className="space-y-3"
    >
      {formError !== null && <FormAlert>{formError}</FormAlert>}
      <TextField
        id="sync-one"
        label="Name or number"
        hint="Such as pikachu or 25."
        autoComplete="off"
        errors={fieldMessages(errors.identifier)}
        {...register('identifier')}
      />
      <button type="submit" disabled={sync.isPending} className={buttonClass}>
        {sync.isPending ? 'Syncing…' : 'Sync'}
      </button>
    </form>
  );
}

function SyncFirstForm() {
  const sync = useSyncFirst();
  const [formError, setFormError] = useState<string | null>(null);
  const [summary, setSummary] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm({ resolver: zodResolver(firstSchema), defaultValues: { limit: '10' } });

  const onSubmit = handleSubmit(async ({ limit }) => {
    setFormError(null);
    setSummary(null);
    try {
      const result = await sync.mutateAsync(Number(limit.trim()));
      setSummary(
        `Synced ${String(result.results.length)} Pokémon: ${String(result.created)} imported, ${String(result.updated)} updated.`,
      );
    } catch (error) {
      setFormError(queryErrorMessage(error));
    }
  });

  return (
    <form
      noValidate
      onSubmit={(event) => {
        void onSubmit(event);
      }}
      className="space-y-3 border-t border-slate-200 pt-4"
    >
      {formError !== null && <FormAlert>{formError}</FormAlert>}
      <TextField
        id="sync-first"
        type="number"
        min={1}
        max={MAX_BULK}
        label="How many from the start of the catalog"
        hint={`From 1 to ${String(MAX_BULK)}, in catalog order.`}
        errors={fieldMessages(errors.limit)}
        {...register('limit')}
      />
      <button type="submit" disabled={sync.isPending} className={buttonClass}>
        {sync.isPending ? 'Syncing…' : 'Sync from the start'}
      </button>
      <p role="status" className="text-sm text-green-800">
        {summary}
      </p>
    </form>
  );
}
