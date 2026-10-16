import { useForm } from 'react-hook-form';
import { Link } from 'react-router';

import { TextField } from '@/shared/ui/form';

/**
 * The name and tag filters. Uncontrolled from the URL's values: the page re-mounts it (by key)
 * when they change, so a tag link or "Clear filters" shows in the fields too.
 */
export function LocalFilters({
  name,
  tag,
  clearHref,
  onApply,
}: {
  name: string;
  tag: string;
  clearHref: string;
  onApply: (name: string, tag: string) => void;
}) {
  const { register, handleSubmit } = useForm({ defaultValues: { name, tag } });
  const onSubmit = handleSubmit((values) => {
    onApply(values.name, values.tag);
  });

  return (
    <form
      role="search"
      aria-label="Filter my Pokédex"
      noValidate
      onSubmit={(event) => {
        void onSubmit(event);
      }}
      className="flex flex-wrap items-end gap-3 rounded-lg border border-slate-200 bg-white p-4"
    >
      <div className="min-w-40 flex-1">
        <TextField
          id="filter-name"
          label="Name contains"
          autoComplete="off"
          {...register('name')}
        />
      </div>
      <div className="min-w-40 flex-1">
        <TextField id="filter-tag" label="Tag" autoComplete="off" {...register('tag')} />
      </div>
      <button
        type="submit"
        className="rounded-md bg-red-600 px-4 py-2 font-semibold text-white hover:bg-red-700"
      >
        Filter
      </button>
      {(name !== '' || tag !== '') && (
        <Link to={clearHref} className="py-2 font-semibold text-red-700 underline">
          Clear filters
        </Link>
      )}
    </form>
  );
}
