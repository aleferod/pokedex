import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { useNavigate } from 'react-router';
import { z } from 'zod';

import { fieldMessages, TextField } from '@/shared/ui/form';

import { IDENTIFIER_RULE, parsePokemonIdentifier } from './identifier';

const findSchema = z.object({
  identifier: z.string().refine((value) => parsePokemonIdentifier(value) !== null, IDENTIFIER_RULE),
});

type FindValues = z.infer<typeof findSchema>;

/** Goes straight to a Pokémon by name or Pokédex number (the API's identifier rule). */
export function FindPokemonForm() {
  const navigate = useNavigate();
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<FindValues>({ resolver: zodResolver(findSchema), defaultValues: { identifier: '' } });

  const onSubmit = handleSubmit((values) => {
    const identifier = parsePokemonIdentifier(values.identifier);
    if (identifier !== null) {
      void navigate(`/pokemon/${identifier}`);
    }
  });

  return (
    <form
      role="search"
      noValidate
      onSubmit={(event) => {
        void onSubmit(event);
      }}
      className="flex w-full max-w-sm items-start gap-2"
    >
      <div className="flex-1">
        <TextField
          id="find-pokemon"
          label="Find a Pokémon"
          hint="A name or a Pokédex number, such as pikachu or 25."
          autoComplete="off"
          errors={fieldMessages(errors.identifier)}
          {...register('identifier')}
        />
      </div>
      <button
        type="submit"
        className="mt-7 rounded-md bg-red-600 px-4 py-2 font-semibold text-white hover:bg-red-700"
      >
        Find
      </button>
    </form>
  );
}
