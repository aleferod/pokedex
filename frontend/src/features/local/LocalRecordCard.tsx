import { Link } from 'react-router';

import { displayName } from '@/features/catalog';
import { formatDateTime } from '@/shared/format';

import type { LocalPokemon } from './types';

/** One local record in the list: its name links to the record, each tag to the list filtered by it. */
export function LocalRecordCard({
  record,
  tagHref,
}: {
  record: LocalPokemon;
  tagHref: (tag: string) => string;
}) {
  const name = displayName(record.name);
  return (
    <article className="flex h-full gap-4 rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
      {record.spriteUrl === null ? (
        <div
          role="img"
          aria-label="No sprite available"
          className="h-20 w-20 shrink-0 rounded-full bg-slate-100"
        />
      ) : (
        <img
          src={record.spriteUrl}
          alt={name}
          width={80}
          height={80}
          loading="lazy"
          className="h-20 w-20 shrink-0 [image-rendering:pixelated]"
        />
      )}
      <div className="min-w-0 flex-1 space-y-1">
        <p className="text-sm text-slate-500">#{record.pokeapiId}</p>
        <h3 className="text-lg font-semibold">
          <Link to={`/local/${record.id}`} className="hover:underline">
            {name}
          </Link>
        </h3>
        {record.region !== null && <p className="text-slate-700">{record.region}</p>}
        {record.internalTags.length > 0 && (
          <ul aria-label="Tags" className="flex flex-wrap gap-1">
            {record.internalTags.map((tag) => (
              <li key={tag}>
                <Link
                  to={tagHref(tag)}
                  aria-label={`Filter by tag ${tag}`}
                  className="rounded bg-slate-100 px-2 py-0.5 text-sm text-slate-700 hover:bg-slate-200"
                >
                  {tag}
                </Link>
              </li>
            ))}
          </ul>
        )}
        <p className="text-sm text-slate-600">
          Updated <time dateTime={record.updatedAt}>{formatDateTime(record.updatedAt)}</time>
        </p>
      </div>
    </article>
  );
}
