import { useForm } from 'react-hook-form';

import { ApiError } from '@/shared/api';
import { fieldMessages, FormAlert, sentence, TextField } from '@/shared/ui/form';
import { queryErrorMessage } from '@/shared/ui/queryErrorMessage';

import { normalizeTag, tagViolation } from './rules';
import type { LocalPokemon } from './types';
import { useTagChange } from './useLocal';

const CONFLICT =
  'Someone else changed this record, so your tag change was undone. The latest version is shown.';

/** Why a tag change was refused, in words: a conflict, the API's tag rule, or the shared wording. */
function tagErrorMessage(error: unknown): string {
  if (error instanceof ApiError && error.status === 409) {
    return CONFLICT;
  }
  const tagErrors = error instanceof ApiError ? error.fieldErrors.internalTags : undefined;
  return tagErrors?.[0] === undefined ? queryErrorMessage(error) : sentence(tagErrors[0]);
}

/** US04: tags added and removed in place, shown at once and undone if the API refuses. */
export function TagEditor({ record }: { record: LocalPokemon }) {
  const change = useTagChange(record.id);
  const busy = change.isPending;
  const {
    register,
    handleSubmit,
    setError,
    reset,
    formState: { errors },
  } = useForm({ defaultValues: { tag: '' } });

  const save = (internalTags: string[]) => {
    change.mutate({ version: record.version, internalTags });
  };

  const onAdd = handleSubmit(({ tag }) => {
    const violation = tagViolation(tag);
    const normalized = normalizeTag(tag);
    if (violation !== null) {
      setError('tag', { type: 'rule', message: violation });
    } else if (record.internalTags.includes(normalized)) {
      setError('tag', { type: 'rule', message: `'${normalized}' is already a tag` });
    } else {
      save([...record.internalTags, normalized]);
      reset();
    }
  });

  return (
    <div className="space-y-3">
      {change.isError && <FormAlert>{tagErrorMessage(change.error)}</FormAlert>}
      {record.internalTags.length === 0 ? (
        <p className="text-slate-600">No tags yet</p>
      ) : (
        <ul aria-label="Tags" className="flex flex-wrap gap-2">
          {record.internalTags.map((tag) => (
            <li
              key={tag}
              className="flex items-center gap-1 rounded-full bg-slate-100 py-0.5 pr-1 pl-3 text-sm text-slate-800"
            >
              <span>{tag}</span>
              <button
                type="button"
                aria-label={`Remove tag ${tag}`}
                disabled={busy}
                onClick={() => {
                  save(record.internalTags.filter((other) => other !== tag));
                }}
                className="rounded-full px-1.5 text-slate-600 hover:bg-slate-200 hover:text-slate-900 disabled:opacity-50"
              >
                <span aria-hidden="true">×</span>
              </button>
            </li>
          ))}
        </ul>
      )}
      <form
        noValidate
        onSubmit={(event) => {
          void onAdd(event);
        }}
        className="flex items-start gap-2"
      >
        <div className="flex-1">
          <TextField
            id="add-tag"
            label="Add a tag"
            hint="Lower-case letters, digits and hyphens, such as starter or gen-1."
            autoComplete="off"
            disabled={busy}
            errors={fieldMessages(errors.tag)}
            {...register('tag')}
          />
        </div>
        <button
          type="submit"
          disabled={busy}
          className="mt-7 rounded-md border border-slate-300 bg-white px-3 py-2 font-semibold hover:border-red-600 disabled:opacity-50"
        >
          Add
        </button>
      </form>
    </div>
  );
}
