import {
  type Control,
  type FieldErrors,
  useFieldArray,
  type UseFormRegister,
} from 'react-hook-form';

import { fieldMessages, sentence, TextAreaField, TextField } from '@/shared/ui/form';

import { localizedNamePath } from './formPaths';
import {
  HABITAT_NOTES_MAX,
  MAX_LOCALES,
  MAX_TAGS,
  NOTES_MAX,
  REGION_MAX,
  type RecordFormValues,
} from './rules';

/** The proprietary fields, shared by the create and edit forms. */
export function ProprietaryFields<TOutput>({
  register,
  control,
  errors,
}: {
  register: UseFormRegister<RecordFormValues>;
  control: Control<RecordFormValues, unknown, TOutput>;
  errors: FieldErrors<RecordFormValues>;
}) {
  const { fields, append, remove } = useFieldArray({ control, name: 'localizedNames' });
  // A rule about the whole list (at most 10) sits on the array or its root, not on a row.
  const listErrors = [
    ...new Set(
      [errors.localizedNames?.root?.message, errors.localizedNames?.message].filter(
        (message): message is string => typeof message === 'string' && message !== '',
      ),
    ),
  ];

  return (
    <>
      <TextField
        id="region"
        label="Region"
        hint={`At most ${String(REGION_MAX)} characters, such as Kanto.`}
        errors={fieldMessages(errors.region)}
        {...register('region')}
      />
      <TextAreaField
        id="habitat-notes"
        label="Habitat notes"
        hint={`At most ${String(HABITAT_NOTES_MAX)} characters.`}
        errors={fieldMessages(errors.habitatNotes)}
        {...register('habitatNotes')}
      />
      <TextAreaField
        id="notes"
        label="Notes"
        rows={5}
        hint={`At most ${String(NOTES_MAX)} characters.`}
        errors={fieldMessages(errors.notes)}
        {...register('notes')}
      />
      <TextField
        id="tags"
        label="Tags"
        hint={`Separate tags with commas, such as starter, kanto. At most ${String(MAX_TAGS)}.`}
        autoComplete="off"
        errors={fieldMessages(errors.tags)}
        {...register('tags')}
      />
      <fieldset className="space-y-3">
        <legend className="font-medium">Localized names</legend>
        <p className="text-sm text-slate-600">
          At most {MAX_LOCALES}, with a locale such as pt-BR, ja or zh-Hant-TW.
        </p>
        {listErrors.length > 0 && (
          <ul className="space-y-0.5 text-sm text-red-700">
            {listErrors.map((message) => (
              <li key={message}>{sentence(message)}</li>
            ))}
          </ul>
        )}
        {fields.map((field, index) => {
          const n = String(index + 1);
          const rowErrors = errors.localizedNames?.[index];
          return (
            <fieldset
              key={field.id}
              className="grid gap-3 rounded-md border border-slate-200 p-3 sm:grid-cols-[10rem_minmax(0,1fr)_auto] sm:items-start"
            >
              <legend className="px-1 text-sm text-slate-600">Localized name {n}</legend>
              <TextField
                id={`locale-${n}`}
                label="Locale"
                autoComplete="off"
                errors={fieldMessages(rowErrors?.locale)}
                {...register(localizedNamePath(index, 'locale'))}
              />
              <TextField
                id={`localized-name-${n}`}
                label="Name"
                autoComplete="off"
                errors={fieldMessages(rowErrors?.name)}
                {...register(localizedNamePath(index, 'name'))}
              />
              <button
                type="button"
                aria-label={`Remove localized name ${n}`}
                onClick={() => {
                  remove(index);
                }}
                className="rounded-md border border-slate-300 bg-white px-3 py-2 font-semibold hover:border-red-600 sm:mt-7"
              >
                Remove
              </button>
            </fieldset>
          );
        })}
        <button
          type="button"
          onClick={() => {
            append({ locale: '', name: '' });
          }}
          className="rounded-md border border-slate-300 bg-white px-3 py-2 font-semibold hover:border-red-600"
        >
          Add a localized name
        </button>
      </fieldset>
    </>
  );
}
