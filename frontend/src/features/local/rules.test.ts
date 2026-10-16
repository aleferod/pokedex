import { describe, expect, it } from 'vitest';

import {
  createSchema,
  normalizeTag,
  proprietarySchema,
  type ProprietaryFormValues,
  tagViolation,
  toFormValues,
} from './rules';
import type { LocalPokemon } from './types';

const TAG_RULE =
  'must be 1 to 30 lower-case letters, digits or hyphens, starting with a letter or digit';

function form(overrides: Partial<ProprietaryFormValues> = {}): ProprietaryFormValues {
  return { region: '', habitatNotes: '', notes: '', tags: '', localizedNames: [], ...overrides };
}

/** Every message, keyed by the form path it belongs to (`localizedNames.0.locale`). */
function issues(result: {
  success: boolean;
  error?: { issues: { path: PropertyKey[]; message: string }[] };
}): Record<string, string[]> {
  const byPath: Record<string, string[]> = {};
  for (const issue of result.error?.issues ?? []) {
    const path = issue.path.map(String).join('.');
    byPath[path] = [...(byPath[path] ?? []), issue.message];
  }
  return byPath;
}

describe('proprietary field rules', () => {
  it('turns an empty form into an empty body', () => {
    expect(proprietarySchema.parse(form())).toEqual({
      region: null,
      habitatNotes: null,
      notes: null,
      internalTags: [],
      localizedNames: {},
    });
  });

  it('trims text, lower-cases and de-duplicates tags, and drops blank values, like the API', () => {
    const body = proprietarySchema.parse(
      form({
        region: '  Kanto  ',
        habitatNotes: '   ',
        notes: ' Loves ketchup ',
        tags: 'Starter, starter,  KANTO , ,',
        localizedNames: [
          { locale: ' pt-BR ', name: '  Pikachu ' },
          { locale: '', name: '' },
        ],
      }),
    );

    expect(body).toEqual({
      region: 'Kanto',
      habitatNotes: null,
      notes: 'Loves ketchup',
      internalTags: ['starter', 'kanto'],
      localizedNames: { 'pt-BR': 'Pikachu' },
    });
  });

  it('limits the text fields with the API’s messages, counting characters as code points', () => {
    expect(issues(proprietarySchema.safeParse(form({ region: 'a'.repeat(51) })))).toEqual({
      region: ['must be at most 50 characters'],
    });
    expect(issues(proprietarySchema.safeParse(form({ habitatNotes: 'a'.repeat(501) })))).toEqual({
      habitatNotes: ['must be at most 500 characters'],
    });
    expect(issues(proprietarySchema.safeParse(form({ notes: 'a'.repeat(2001) })))).toEqual({
      notes: ['must be at most 2000 characters'],
    });
    expect(proprietarySchema.safeParse(form({ region: '🔥'.repeat(50) })).success).toBe(true);
    expect(proprietarySchema.safeParse(form({ region: ` ${'a'.repeat(50)} ` })).success).toBe(true);
  });

  it('names every tag that breaks the rule', () => {
    expect(issues(proprietarySchema.safeParse(form({ tags: 'ok, Bad Tag!, -dash' })))).toEqual({
      tags: [`'Bad Tag!' ${TAG_RULE}`, `'-dash' ${TAG_RULE}`],
    });
    expect(issues(proprietarySchema.safeParse(form({ tags: 'a'.repeat(31) })))).toEqual({
      tags: [`'${'a'.repeat(31)}' ${TAG_RULE}`],
    });
    expect(
      proprietarySchema.safeParse(form({ tags: `a, 9-lives, ${'a'.repeat(30)}` })).success,
    ).toBe(true);
  });

  it('allows at most 20 distinct tags', () => {
    const tags = (count: number) => Array.from({ length: count }, (_, i) => `t${String(i)}`);
    expect(proprietarySchema.safeParse(form({ tags: tags(20).join(', ') })).success).toBe(true);
    expect(
      proprietarySchema.safeParse(form({ tags: [...tags(20), 'T0', 't1'].join(', ') })).success,
    ).toBe(true);
    expect(issues(proprietarySchema.safeParse(form({ tags: tags(21).join(', ') })))).toEqual({
      tags: ['must have at most 20 tags'],
    });
  });

  it('checks each localized name on its own row', () => {
    const result = proprietarySchema.safeParse(
      form({
        localizedNames: [
          { locale: 'pt_BR', name: 'Pikachu' },
          { locale: 'fr', name: '' },
          { locale: 'ja', name: 'ピ'.repeat(51) },
          { locale: 'PT-br', name: 'Pikachu' },
        ],
      }),
    );

    expect(issues(result)).toEqual({
      'localizedNames.0.locale': ["'pt_BR' is not a locale such as en, pt-BR or zh-Hant-TW"],
      'localizedNames.1.name': ['must be 1 to 50 characters'],
      'localizedNames.2.name': ['must be 1 to 50 characters'],
      'localizedNames.3.locale': ["'PT-br' is not a locale such as en, pt-BR or zh-Hant-TW"],
    });
  });

  it('accepts the locale shapes the API accepts', () => {
    const rows = ['en', 'pt-BR', 'zh-Hant-TW', 'yue', 'es-419'].map((locale) => ({
      locale,
      name: 'Pikachu',
    }));
    expect(proprietarySchema.safeParse(form({ localizedNames: rows })).success).toBe(true);
  });

  it('rejects a locale listed twice and more than 10 locales', () => {
    expect(
      issues(
        proprietarySchema.safeParse(
          form({
            localizedNames: [
              { locale: 'pt-BR', name: 'Pikachu' },
              { locale: 'pt-BR', name: 'Pikachu 2' },
            ],
          }),
        ),
      ),
    ).toEqual({ 'localizedNames.1.locale': ['is listed twice'] });

    const eleven = Array.from({ length: 11 }, (_, i) => ({
      locale: `l${'abcdefghijk'.charAt(i)}`,
      name: 'Name',
    }));
    expect(issues(proprietarySchema.safeParse(form({ localizedNames: eleven })))).toEqual({
      localizedNames: ['must have at most 10 entries'],
    });
  });

  it('lists every broken rule at once', () => {
    const result = proprietarySchema.safeParse(
      form({ region: 'a'.repeat(51), tags: 'Bad Tag!', notes: 'a'.repeat(2001) }),
    );

    expect(Object.keys(issues(result)).sort()).toEqual(['notes', 'region', 'tags']);
  });
});

describe('creating a record', () => {
  it('needs a Pokédex number from 1 to 99999', () => {
    expect(issues(createSchema.safeParse({ ...form(), pokeapiId: '' }))).toEqual({
      pokeapiId: ['is required'],
    });
    for (const value of ['abc', '0', '100000', '1.5', '-3']) {
      expect(issues(createSchema.safeParse({ ...form(), pokeapiId: value }))).toEqual({
        pokeapiId: ['must be a whole number from 1 to 99999'],
      });
    }
  });

  it('sends the number with the proprietary fields', () => {
    expect(createSchema.parse({ ...form({ region: 'Kanto' }), pokeapiId: ' 001 ' })).toEqual({
      pokeapiId: 1,
      region: 'Kanto',
      habitatNotes: null,
      notes: null,
      internalTags: [],
      localizedNames: {},
    });
  });
});

describe('a tag added in place', () => {
  it('is normalised like the API does', () => {
    expect(normalizeTag('  Starter ')).toBe('starter');
  });

  it('is checked with the tag rule', () => {
    expect(tagViolation('Starter')).toBeNull();
    expect(tagViolation('Bad Tag!')).toBe(`'Bad Tag!' ${TAG_RULE}`);
    expect(tagViolation('   ')).toBe('Enter a tag');
  });
});

describe('the edit form', () => {
  it('starts from the record’s proprietary fields', () => {
    const record = {
      region: 'Kanto',
      habitatNotes: null,
      notes: 'Loves ketchup',
      internalTags: ['starter', 'kanto'],
      localizedNames: { ja: 'ピカチュウ', 'pt-BR': 'Pikachu' },
    } as Pick<
      LocalPokemon,
      'region' | 'habitatNotes' | 'notes' | 'internalTags' | 'localizedNames'
    > as LocalPokemon;

    expect(toFormValues(record)).toEqual({
      region: 'Kanto',
      habitatNotes: '',
      notes: 'Loves ketchup',
      tags: 'starter, kanto',
      localizedNames: [
        { locale: 'ja', name: 'ピカチュウ' },
        { locale: 'pt-BR', name: 'Pikachu' },
      ],
    });
  });
});
