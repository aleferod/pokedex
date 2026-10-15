import { describe, expect, it } from 'vitest';
import type { z } from 'zod';

import {
  emailSchema,
  passwordSchema,
  registerSchema,
  signInSchema,
  usernameSchema,
} from './schemas';

function messages(result: z.ZodSafeParseResult<unknown>): string[] {
  return result.success ? [] : result.error.issues.map((issue) => issue.message);
}

describe('username rule (mirrors pokedex-api Username)', () => {
  it('trims and lower-cases, like the backend', () => {
    expect(usernameSchema.parse('  Ash.K_99-  ')).toBe('ash.k_99-');
  });

  it.each(['as', 'a'.repeat(31), 'ash ketchum', 'ash!', ''])('rejects %j', (value) => {
    expect(messages(usernameSchema.safeParse(value))).toEqual([
      "must be 3 to 30 characters: letters, digits, '.', '_' or '-'",
    ]);
  });

  it('accepts exactly 30 characters', () => {
    expect(usernameSchema.safeParse('a'.repeat(30)).success).toBe(true);
  });
});

describe('email rule (mirrors pokedex-api EmailAddress)', () => {
  it('trims and lower-cases', () => {
    expect(emailSchema.parse(' Ash@Pallet.Town ')).toBe('ash@pallet.town');
  });

  it.each(['ash', 'ash@pallet', '@pallet.town', 'ash@@pallet.town', `${'a'.repeat(250)}@x.io`])(
    'rejects %j',
    (value) => {
      expect(messages(emailSchema.safeParse(value))).toEqual(['must be a valid email address']);
    },
  );
});

describe('password rule (mirrors pokedex-api PasswordPolicy)', () => {
  it('accepts a password meeting every rule', () => {
    expect(passwordSchema.safeParse('Pikachu@25').success).toBe(true);
  });

  it('lists every unmet rule at once, in the backend words', () => {
    expect(messages(passwordSchema.safeParse('abc'))).toEqual([
      'must be 8 to 72 characters',
      'must contain an upper-case letter',
      'must contain a digit',
      'must contain a character that is neither a letter nor a digit',
    ]);
  });

  it('counts characters, not UTF-16 units, for the minimum', () => {
    expect(passwordSchema.safeParse('Aa1!😀😀😀😀').success).toBe(true);
  });

  it('allows 72 characters but not 73', () => {
    const base = 'Aa1!';
    expect(passwordSchema.safeParse(base + 'x'.repeat(68)).success).toBe(true);
    expect(messages(passwordSchema.safeParse(base + 'x'.repeat(69)))).toEqual([
      'must be 8 to 72 characters',
    ]);
  });

  it('also caps the UTF-8 size at 72 bytes, as BCrypt needs', () => {
    // 39 characters, 75 bytes
    expect(messages(passwordSchema.safeParse('Aé1!' + 'é'.repeat(35)))).toEqual([
      'must be 8 to 72 characters',
    ]);
  });
});

describe('forms', () => {
  it('sign-in only requires both fields; the server judges the rest', () => {
    expect(signInSchema.safeParse({ username: 'x', password: 'y' }).success).toBe(true);
    expect(messages(signInSchema.safeParse({ username: '  ', password: '' }))).toEqual([
      'is required',
      'is required',
    ]);
  });

  it('registration combines the three rules and normalises the values', () => {
    expect(
      registerSchema.parse({
        username: ' Misty ',
        email: 'Misty@Cerulean.City',
        password: 'Starmie@121',
      }),
    ).toEqual({ username: 'misty', email: 'misty@cerulean.city', password: 'Starmie@121' });
  });
});
