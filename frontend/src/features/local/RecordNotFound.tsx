import { Link } from 'react-router';

/** A record id that isn't (or is no longer) in the local Pokédex. */
export function RecordNotFound() {
  return (
    <section aria-labelledby="record-missing" className="space-y-4">
      <h1 id="record-missing" className="text-2xl font-bold">
        Record not found
      </h1>
      <p className="text-slate-700">
        This record isn’t in your local Pokédex. It may have been deleted.
      </p>
      <Link to="/local" className="inline-block font-semibold text-red-700 underline">
        Back to My Pokédex
      </Link>
    </section>
  );
}
