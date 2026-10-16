import { z } from 'zod';

/*
 * The account rules, mirrored from pokedex-api (Username, EmailAddress, PasswordPolicy) with the
 * same messages, so the form rejects exactly what the API would and says it the same way. The API
 * stays the authority: its 400s are shown next to the fields too.
 */

export const REQUIRED = 'is required';
export const USERNAME_RULE = "must be 3 to 30 characters: letters, digits, '.', '_' or '-'";
export const EMAIL_RULE = 'must be a valid email address';
export const PASSWORD_MIN = 8;
export const PASSWORD_MAX = 72;

const USERNAME = /^[a-z0-9._-]{3,30}$/;
const EMAIL = /^[a-z0-9._%+-]+@[a-z0-9-]+(\.[a-z0-9-]+)+$/;
const EMAIL_MAX = 254;

export const usernameSchema = z.string().trim().toLowerCase().regex(USERNAME, USERNAME_RULE);

export const emailSchema = z
  .string()
  .trim()
  .toLowerCase()
  .max(EMAIL_MAX, EMAIL_RULE)
  .regex(EMAIL, EMAIL_RULE);

/** Every rule the password breaks, in pokedex-api's words and order. */
export function passwordViolations(password: string): string[] {
  const characters = codePointCount(password);
  const bytes = new TextEncoder().encode(password).length;
  const violations: string[] = [];
  if (characters < PASSWORD_MIN || characters > PASSWORD_MAX || bytes > PASSWORD_MAX) {
    violations.push(`must be ${String(PASSWORD_MIN)} to ${String(PASSWORD_MAX)} characters`);
  }
  if (!/\p{Lu}/u.test(password)) {
    violations.push('must contain an upper-case letter');
  }
  if (!/\p{Ll}/u.test(password)) {
    violations.push('must contain a lower-case letter');
  }
  if (!/\p{Nd}/u.test(password)) {
    violations.push('must contain a digit');
  }
  if (!/[^\p{L}\p{Nd}]/u.test(password)) {
    violations.push('must contain a character that is neither a letter nor a digit');
  }
  return violations;
}

/** Characters as Java counts them (code points, not UTF-16 units), like the backend's rule. */
function codePointCount(value: string): number {
  return Array.from(value).length;
}

export const passwordSchema = z.string().check((ctx) => {
  for (const message of passwordViolations(ctx.value)) {
    ctx.issues.push({ code: 'custom', message, input: ctx.value });
  }
});

/** Sign-in only needs both fields; whether they match is the server's call (401). */
export const signInSchema = z.object({
  username: z.string().trim().min(1, REQUIRED),
  password: z.string().min(1, REQUIRED),
});

export const registerSchema = z.object({
  username: usernameSchema,
  email: emailSchema,
  password: passwordSchema,
});

export type SignInValues = z.infer<typeof signInSchema>;
export type RegisterValues = z.infer<typeof registerSchema>;
