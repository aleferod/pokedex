import { FindPokemonForm } from '@/features/catalog';

/** No Pokémon at this address: say why and offer the finder. */
export function DetailNotFound({ message }: { message: string }) {
  return (
    <section aria-labelledby="not-found-title" className="space-y-4">
      <h1 id="not-found-title" className="text-2xl font-bold">
        Pokémon not found
      </h1>
      <p className="break-words text-slate-700">{message}</p>
      <FindPokemonForm />
    </section>
  );
}
