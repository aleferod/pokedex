import { useDocumentTitle } from '@/shared/hooks/useDocumentTitle';

export function HomePage() {
  useDocumentTitle('Home');
  return (
    <section aria-labelledby="home-title" className="space-y-3 py-6">
      <h1 id="home-title" className="text-3xl font-bold">
        Welcome to the Pokédex
      </h1>
      <p className="max-w-prose text-slate-700">
        Browse every Pokémon from PokeAPI, and keep your own notes, tags and localized names for the
        ones your team cares about.
      </p>
    </section>
  );
}
