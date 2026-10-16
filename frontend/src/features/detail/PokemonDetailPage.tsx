import { Link, Navigate, useLocation, useParams } from 'react-router';

import {
  type CatalogReturnState,
  catalogReturnSearch,
  displayName,
  parsePokemonIdentifier,
} from '@/features/catalog';
import { ApiError } from '@/shared/api';
import { useDocumentTitle } from '@/shared/hooks/useDocumentTitle';
import { QueryError } from '@/shared/ui/QueryError';

import { DetailNotFound } from './DetailNotFound';
import { DetailSkeleton } from './DetailSkeleton';
import { PokemonDetailView } from './PokemonDetailView';
import { usePokemonDetail } from './usePokemonDetail';

const NOT_FOUND = 'Pokémon not found';

/**
 * US02: one Pokémon in full, by Pokédex number or name. A non-canonical identifier (`Pikachu`,
 * `025`) is replaced by its canonical form, so each Pokémon has one address and one cache entry.
 */
export function PokemonDetailPage() {
  const { idOrName = '' } = useParams();
  const location = useLocation();
  const identifier = parsePokemonIdentifier(idOrName);

  if (identifier !== null && identifier !== idOrName) {
    return <Navigate to={`/pokemon/${identifier}`} replace state={location.state as unknown} />;
  }

  const linkState: CatalogReturnState = { catalogSearch: catalogReturnSearch(location.state) };
  return (
    <div className="space-y-6">
      <Link
        to={`/pokemon${linkState.catalogSearch}`}
        className="inline-block font-semibold text-red-700 underline"
      >
        <span aria-hidden="true">← </span>Back to the catalog
      </Link>
      {identifier === null ? (
        <InvalidIdentifier raw={idOrName} />
      ) : (
        <PokemonDetailContent identifier={identifier} linkState={linkState} />
      )}
    </div>
  );
}

function InvalidIdentifier({ raw }: { raw: string }) {
  useDocumentTitle(NOT_FOUND);
  return <DetailNotFound message={`“${raw}” isn’t a Pokédex number or a name.`} />;
}

function PokemonDetailContent({
  identifier,
  linkState,
}: {
  identifier: string;
  linkState: CatalogReturnState;
}) {
  const query = usePokemonDetail(identifier);
  const notFound = query.error instanceof ApiError && query.error.status === 404;
  useDocumentTitle(query.data ? displayName(query.data.name) : notFound ? NOT_FOUND : 'Pokémon');

  if (query.isPending) {
    return (
      <>
        <p role="status" className="sr-only">
          Loading Pokémon…
        </p>
        <DetailSkeleton />
      </>
    );
  }
  if (query.isError) {
    return notFound ? (
      <DetailNotFound message={notFoundMessage(identifier)} />
    ) : (
      <QueryError
        title="Couldn’t load this Pokémon"
        error={query.error}
        onRetry={() => {
          void query.refetch();
        }}
      />
    );
  }
  return <PokemonDetailView pokemon={query.data} linkState={linkState} />;
}

function notFoundMessage(identifier: string): string {
  return /^\d+$/.test(identifier)
    ? `There is no Pokémon number ${identifier}.`
    : `There is no Pokémon called “${identifier}”.`;
}
