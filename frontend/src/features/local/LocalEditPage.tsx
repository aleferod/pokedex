import { zodResolver } from '@hookform/resolvers/zod';
import { useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link, useNavigate, useParams } from 'react-router';

import { displayName } from '@/features/catalog';
import { ApiError } from '@/shared/api';
import { useDocumentTitle } from '@/shared/hooks/useDocumentTitle';
import { withFlash } from '@/shared/ui/flash';
import { FormAlert, showServerErrors, SubmitButton } from '@/shared/ui/form';
import { QueryError } from '@/shared/ui/QueryError';
import { queryErrorMessage } from '@/shared/ui/queryErrorMessage';

import { fetchLocalRecord } from './api';
import { serverFieldFor } from './formPaths';
import { ProprietaryFields } from './ProprietaryFields';
import { RecordNotFound } from './RecordNotFound';
import { editSchema, type ProprietaryBody, type RecordFormValues, toFormValues } from './rules';
import type { LocalPokemon } from './types';
import { localKeys, useLocalRecord, useReplaceRecord } from './useLocal';

const GONE = 'This record no longer exists. It may have been deleted.';

function formValues(record: LocalPokemon): RecordFormValues {
  return { ...toFormValues(record), pokeapiId: String(record.pokeapiId) };
}

/** US04: edit every proprietary field of a record, saved for the version that was loaded. */
export function LocalEditPage() {
  const { id = '' } = useParams();
  const query = useLocalRecord(id);
  const notFound = query.error instanceof ApiError && query.error.status === 404;
  useDocumentTitle(
    query.data ? `Edit ${displayName(query.data.name)}` : notFound ? 'Record not found' : 'Edit',
  );

  if (query.isPending) {
    return (
      <p role="status" className="text-slate-600">
        Loading the record…
      </p>
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
  return <EditForm key={query.data.id} record={query.data} />;
}

/**
 * The form keeps the record it started from (D3): a background refetch must not turn a stale
 * edit into an overwrite, so the version sent is the one loaded here, until the user asks for the
 * latest version.
 */
function EditForm({ record }: { record: LocalPokemon }) {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const save = useReplaceRecord(record.id);
  const [loaded, setLoaded] = useState(record);
  const [formError, setFormError] = useState<string | null>(null);
  const [conflict, setConflict] = useState(false);
  const name = displayName(loaded.name);
  const {
    register,
    control,
    handleSubmit,
    getValues,
    setError,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<RecordFormValues, unknown, ProprietaryBody>({
    resolver: zodResolver(editSchema),
    mode: 'onTouched',
    criteriaMode: 'all',
    defaultValues: formValues(record),
  });

  const onSubmit = handleSubmit(async (body) => {
    setFormError(null);
    setConflict(false);
    try {
      await save.mutateAsync({ version: loaded.version, body });
      void navigate(`/local/${loaded.id}`, { state: withFlash('Your changes were saved.') });
    } catch (error) {
      if (error instanceof ApiError && error.status === 409) {
        setConflict(true);
      } else if (error instanceof ApiError && error.status === 404) {
        setFormError(GONE);
      } else {
        setFormError(showServerErrors(error, serverFieldFor(getValues()), setError));
      }
    }
  });

  const loadLatest = async () => {
    try {
      const latest = await queryClient.query({
        queryKey: localKeys.record(loaded.id),
        queryFn: ({ signal }) => fetchLocalRecord(loaded.id, signal),
        staleTime: 0,
      });
      setLoaded(latest);
      reset(formValues(latest));
      setConflict(false);
    } catch (error) {
      setConflict(false);
      setFormError(
        error instanceof ApiError && error.status === 404 ? GONE : queryErrorMessage(error),
      );
    }
  };

  return (
    <section aria-labelledby="edit-title" className="mx-auto max-w-2xl space-y-6">
      <div className="space-y-1">
        <p className="text-sm text-slate-500">#{loaded.pokeapiId}</p>
        <h1 id="edit-title" className="text-3xl font-bold">
          Edit {name}
        </h1>
        <p className="text-slate-700">
          The PokeAPI data can’t be edited here; re-sync it from the record’s page.
        </p>
      </div>
      {conflict && (
        <FormAlert>
          <p>
            Someone else changed this record after you opened it, so your changes weren’t saved.
          </p>
          <p className="mt-1 text-sm">
            Loading the latest version replaces your edits in this form.
          </p>
          <button
            type="button"
            onClick={() => {
              void loadLatest();
            }}
            className="mt-2 rounded-md bg-red-600 px-3 py-1.5 font-semibold text-white hover:bg-red-700"
          >
            Load the latest version
          </button>
        </FormAlert>
      )}
      {formError !== null && <FormAlert>{formError}</FormAlert>}
      <form
        noValidate
        onSubmit={(event) => {
          void onSubmit(event);
        }}
        className="space-y-5 rounded-lg border border-slate-200 bg-white p-4"
      >
        <ProprietaryFields register={register} control={control} errors={errors} />
        <SubmitButton busy={isSubmitting} busyLabel="Saving…">
          Save changes
        </SubmitButton>
      </form>
      <Link
        to={`/local/${loaded.id}`}
        className="inline-block font-semibold text-red-700 underline"
      >
        Cancel
      </Link>
    </section>
  );
}
