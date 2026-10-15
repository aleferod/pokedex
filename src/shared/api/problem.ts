/** One entry of a problem's `errors` array, as pokedex-api sends it. */
export interface ProblemFieldError {
  field: string;
  message: string;
}

/** An RFC 7807 problem document from pokedex-api. */
export interface ProblemDocument {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  errors?: ProblemFieldError[];
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function optionalString(value: unknown): string | undefined {
  return typeof value === 'string' && value.length > 0 ? value : undefined;
}

/** Reads a problem document defensively; anything unexpected is dropped, never trusted. */
export function parseProblem(body: unknown): ProblemDocument | undefined {
  if (!isRecord(body)) {
    return undefined;
  }
  const errors = Array.isArray(body.errors)
    ? body.errors.filter(
        (entry): entry is ProblemFieldError =>
          isRecord(entry) && typeof entry.field === 'string' && typeof entry.message === 'string',
      )
    : [];
  const problem: ProblemDocument = { errors };
  const type = optionalString(body.type);
  const title = optionalString(body.title);
  const detail = optionalString(body.detail);
  const instance = optionalString(body.instance);
  if (type !== undefined) problem.type = type;
  if (title !== undefined) problem.title = title;
  if (detail !== undefined) problem.detail = detail;
  if (instance !== undefined) problem.instance = instance;
  if (typeof body.status === 'number') problem.status = body.status;
  return problem;
}

/** The problem type's short name: `…/problems.md#conflict` → `conflict`; `about:blank` → none. */
export function problemSlug(type: string | undefined): string | undefined {
  if (type === undefined) {
    return undefined;
  }
  const fragment = type.split('#')[1];
  return fragment !== undefined && fragment.length > 0 ? fragment : undefined;
}

/** `[{field: 'title', message: 'a'}, {field: 'title', message: 'b'}]` → `{title: ['a', 'b']}`. */
export function groupFieldErrors(
  errors: readonly ProblemFieldError[],
): Record<string, readonly string[]> {
  const grouped: Record<string, string[]> = {};
  for (const { field, message } of errors) {
    (grouped[field] ??= []).push(message);
  }
  return grouped;
}
