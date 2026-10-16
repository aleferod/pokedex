import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link, useNavigate } from 'react-router';

import { displayName } from '@/features/catalog';
import { useDocumentTitle } from '@/shared/hooks/useDocumentTitle';
import { withFlash } from '@/shared/ui/flash';
import {
  fieldMessages,
  FormAlert,
  showServerErrors,
  SubmitButton,
  TextField,
} from '@/shared/ui/form';

import { serverFieldFor } from './formPaths';
import { ProprietaryFields } from './ProprietaryFields';
import { type CreateBody, createSchema, EMPTY_PROPRIETARY, type RecordFormValues } from './rules';
import { useCreateRecord } from './useLocal';

/** US04: store a Pokémon by number, with its proprietary fields from the start. */
export function LocalCreatePage() {
  useDocumentTitle('Add a Pokémon');
  const navigate = useNavigate();
  const create = useCreateRecord();
  const [formError, setFormError] = useState<string | null>(null);
  const {
    register,
    control,
    handleSubmit,
    getValues,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<RecordFormValues, unknown, CreateBody>({
    resolver: zodResolver(createSchema),
    mode: 'onTouched',
    criteriaMode: 'all',
    defaultValues: { ...EMPTY_PROPRIETARY, pokeapiId: '' },
  });

  const onSubmit = handleSubmit(async (body) => {
    setFormError(null);
    try {
      const record = await create.mutateAsync(body);
      void navigate(`/local/${record.id}`, {
        state: withFlash(`${displayName(record.name)} was added to your local Pokédex.`),
      });
    } catch (error) {
      setFormError(showServerErrors(error, serverFieldFor(getValues()), setError));
    }
  });

  return (
    <section aria-labelledby="create-title" className="mx-auto max-w-2xl space-y-6">
      <div className="space-y-1">
        <h1 id="create-title" className="text-3xl font-bold">
          Add a Pokémon
        </h1>
        <p className="text-slate-700">
          Its PokeAPI data is copied in; everything below is yours to fill in now or later.
        </p>
      </div>
      {formError !== null && <FormAlert>{formError}</FormAlert>}
      <form
        noValidate
        onSubmit={(event) => {
          void onSubmit(event);
        }}
        className="space-y-5 rounded-lg border border-slate-200 bg-white p-4"
      >
        <TextField
          id="pokeapi-id"
          label="Pokédex number"
          inputMode="numeric"
          autoComplete="off"
          hint="From 1 to 99999, such as 25 for Pikachu."
          errors={fieldMessages(errors.pokeapiId)}
          {...register('pokeapiId')}
        />
        <ProprietaryFields register={register} control={control} errors={errors} />
        <SubmitButton busy={isSubmitting} busyLabel="Adding…">
          Add to my Pokédex
        </SubmitButton>
      </form>
      <Link to="/local" className="inline-block font-semibold text-red-700 underline">
        Cancel
      </Link>
    </section>
  );
}
