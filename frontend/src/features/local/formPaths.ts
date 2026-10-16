import type { Path } from 'react-hook-form';

import type { RecordFormValues } from './rules';

/** `localizedNames.<index>.<part>`, typed as the form path React Hook Form expects. */
export function localizedNamePath(
  index: number,
  part: 'locale' | 'name',
): `localizedNames.${number}.${typeof part}` {
  return `localizedNames.${String(index)}.${part}` as `localizedNames.${number}.${typeof part}`;
}

/**
 * Where an API field error belongs on a record form: `internalTags` on the tags line, and
 * `localizedNames.pt-BR` on the name of the row with that locale.
 */
export function serverFieldFor(values: RecordFormValues) {
  return (apiField: string): Path<RecordFormValues> | undefined => {
    switch (apiField) {
      case 'pokeapiId':
      case 'region':
      case 'habitatNotes':
      case 'notes':
      case 'localizedNames':
        return apiField;
      case 'internalTags':
        return 'tags';
    }
    const locale = apiField.startsWith('localizedNames.')
      ? apiField.slice('localizedNames.'.length)
      : null;
    const index = values.localizedNames.findIndex((row) => row.locale.trim() === locale);
    return index >= 0 ? localizedNamePath(index, 'name') : undefined;
  };
}
