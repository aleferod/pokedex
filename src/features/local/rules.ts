import { z } from 'zod';

import type { LocalPokemon } from './types';

/*
 * The proprietary fields' rules, mirrored from pokedex-api (ProprietaryData, InternalTags,
 * LocalizedNames) with the same messages, so the forms reject exactly what the API would. The API
 * stays the authority: its 400s are shown on the fields too.
 */

export const REGION_MAX = 50;
export const HABITAT_NOTES_MAX = 500;
export const NOTES_MAX = 2000;
export const MAX_TAGS = 20;
export const MAX_LOCALES = 10;
const NAME_MAX = 50;
const LOCALE_MAX = 35;
const POKEAPI_ID_MAX = 99_999;

const TAG = /^[a-z0-9][a-z0-9-]{0,29}$/;
const LOCALE = /^[a-z]{2,3}(-[A-Za-z0-9]{2,8}){0,2}$/;
const DIGITS = /^\d{1,9}$/;

const TAG_RULE =
  'must be 1 to 30 lower-case letters, digits or hyphens, starting with a letter or digit';
const LOCALE_RULE = 'is not a locale such as en, pt-BR or zh-Hant-TW';
const REQUIRED = 'is required';
const POKEAPI_ID_RULE = `must be a whole number from 1 to ${String(POKEAPI_ID_MAX)}`;

const proprietaryFields = z.object({
  region: z.string(),
  habitatNotes: z.string(),
  notes: z.string(),
  /** Comma-separated, as typed. */
  tags: z.string(),
  localizedNames: z.array(z.object({ locale: z.string(), name: z.string() })),
});

/** The proprietary fields as the form holds them: text as typed, tags as one line. */
export type ProprietaryFormValues = z.input<typeof proprietaryFields>;

interface Issue {
  path: (string | number)[];
  message: string;
}

/** Characters as Java counts them (code points), like the backend's rules. */
function length(value: string): number {
  return Array.from(value).length;
}

function textOrNull(value: string): string | null {
  const trimmed = value.trim();
  return trimmed === '' ? null : trimmed;
}

/** The pieces of the tags line, trimmed, without empty ones. */
function tagPieces(line: string): string[] {
  return line
    .split(',')
    .map((piece) => piece.trim())
    .filter((piece) => piece !== '');
}

export function normalizeTag(raw: string): string {
  return raw.trim().toLowerCase();
}

/** Why a tag typed on its own can't be added, or null when it can. */
export function tagViolation(raw: string): string | null {
  const piece = raw.trim();
  if (piece === '') {
    return 'Enter a tag';
  }
  return TAG.test(normalizeTag(piece)) ? null : `'${piece}' ${TAG_RULE}`;
}

/** Rows with a locale or a name; a row left completely empty is ignored. */
function filledRows(rows: ProprietaryFormValues['localizedNames']) {
  return rows
    .map((row, index) => ({ index, locale: row.locale.trim(), name: row.name.trim() }))
    .filter((row) => row.locale !== '' || row.name !== '');
}

function proprietaryIssues(values: ProprietaryFormValues): Issue[] {
  const issues: Issue[] = [];
  const text: ['region' | 'habitatNotes' | 'notes', string, number][] = [
    ['region', values.region, REGION_MAX],
    ['habitatNotes', values.habitatNotes, HABITAT_NOTES_MAX],
    ['notes', values.notes, NOTES_MAX],
  ];
  for (const [field, value, max] of text) {
    if (length(value.trim()) > max) {
      issues.push({ path: [field], message: `must be at most ${String(max)} characters` });
    }
  }

  const pieces = tagPieces(values.tags);
  for (const piece of pieces) {
    if (!TAG.test(normalizeTag(piece))) {
      issues.push({ path: ['tags'], message: `'${piece}' ${TAG_RULE}` });
    }
  }
  if (new Set(pieces.map(normalizeTag)).size > MAX_TAGS) {
    issues.push({ path: ['tags'], message: `must have at most ${String(MAX_TAGS)} tags` });
  }

  const rows = filledRows(values.localizedNames);
  if (rows.length > MAX_LOCALES) {
    issues.push({
      path: ['localizedNames'],
      message: `must have at most ${String(MAX_LOCALES)} entries`,
    });
  }
  const seen = new Set<string>();
  for (const row of rows) {
    if (row.locale.length > LOCALE_MAX || !LOCALE.test(row.locale)) {
      issues.push({
        path: ['localizedNames', row.index, 'locale'],
        message: `'${row.locale}' ${LOCALE_RULE}`,
      });
    } else if (seen.has(row.locale)) {
      issues.push({ path: ['localizedNames', row.index, 'locale'], message: 'is listed twice' });
    }
    seen.add(row.locale);
    if (length(row.name) < 1 || length(row.name) > NAME_MAX) {
      issues.push({
        path: ['localizedNames', row.index, 'name'],
        message: `must be 1 to ${String(NAME_MAX)} characters`,
      });
    }
  }
  return issues;
}

/** The request body's proprietary fields, normalised as the API would normalise them. */
function proprietaryBody(values: ProprietaryFormValues) {
  return {
    region: textOrNull(values.region),
    habitatNotes: textOrNull(values.habitatNotes),
    notes: textOrNull(values.notes),
    internalTags: [...new Set(tagPieces(values.tags).map(normalizeTag))],
    localizedNames: Object.fromEntries(
      filledRows(values.localizedNames).map((row) => [row.locale, row.name]),
    ) as Record<string, string>,
  };
}

function report(ctx: z.core.ParsePayload, issues: Issue[]): void {
  for (const issue of issues) {
    ctx.issues.push({ code: 'custom', input: ctx.value, ...issue });
  }
}

/** The edit form: every proprietary field, as `PUT` replaces them all. */
export const proprietarySchema = proprietaryFields
  .check((ctx) => {
    report(ctx, proprietaryIssues(ctx.value));
  })
  .transform(proprietaryBody);

export type ProprietaryBody = z.output<typeof proprietarySchema>;

/** The create form: a Pokédex number and the proprietary fields. */
export const createSchema = proprietaryFields
  .extend({ pokeapiId: z.string() })
  .check((ctx) => {
    const number = ctx.value.pokeapiId.trim();
    const issues = proprietaryIssues(ctx.value);
    if (number === '') {
      issues.unshift({ path: ['pokeapiId'], message: REQUIRED });
    } else if (!DIGITS.test(number) || Number(number) < 1 || Number(number) > POKEAPI_ID_MAX) {
      issues.unshift({ path: ['pokeapiId'], message: POKEAPI_ID_RULE });
    }
    report(ctx, issues);
  })
  .transform((values) => ({
    pokeapiId: Number(values.pokeapiId.trim()),
    ...proprietaryBody(values),
  }));

export type CreateBody = z.output<typeof createSchema>;

/** What both record forms hold: the Pokédex number (sent only when creating) and the fields. */
export type RecordFormValues = z.input<typeof createSchema>;

/** The edit form: the same values as the create form; the number is shown, not sent. */
export const editSchema = proprietaryFields
  .extend({ pokeapiId: z.string() })
  .check((ctx) => {
    report(ctx, proprietaryIssues(ctx.value));
  })
  .transform(proprietaryBody);

export const EMPTY_PROPRIETARY: ProprietaryFormValues = {
  region: '',
  habitatNotes: '',
  notes: '',
  tags: '',
  localizedNames: [],
};

/** The edit form's starting values: the record's proprietary fields. */
export function toFormValues(record: LocalPokemon): ProprietaryFormValues {
  return {
    region: record.region ?? '',
    habitatNotes: record.habitatNotes ?? '',
    notes: record.notes ?? '',
    tags: record.internalTags.join(', '),
    localizedNames: Object.entries(record.localizedNames).map(([locale, name]) => ({
      locale,
      name,
    })),
  };
}
