import { groupFieldErrors, parseProblem, problemSlug } from './problem';

export const NETWORK_ERROR_MESSAGE =
  'Cannot reach the server. Check your connection and try again.';

export interface ApiErrorDetails {
  type?: string | undefined;
  title?: string | undefined;
  detail?: string | undefined;
  fieldErrors?: Readonly<Record<string, readonly string[]>>;
  correlationId?: string | undefined;
  isNetworkError?: boolean;
  cause?: unknown;
}

/**
 * Every failed call to pokedex-api: an HTTP error (with the RFC 7807 problem's details when the
 * body is one) or a network failure (status 0). Screens branch on `status` and `type`, show
 * `message`, and place `fieldErrors` next to the matching inputs.
 */
export class ApiError extends Error {
  readonly status: number;
  readonly type: string | undefined;
  readonly title: string | undefined;
  readonly detail: string | undefined;
  readonly fieldErrors: Readonly<Record<string, readonly string[]>>;
  readonly correlationId: string | undefined;
  readonly isNetworkError: boolean;

  constructor(message: string, status: number, details: ApiErrorDetails = {}) {
    super(message, details.cause === undefined ? undefined : { cause: details.cause });
    this.name = 'ApiError';
    this.status = status;
    this.type = details.type;
    this.title = details.title;
    this.detail = details.detail;
    this.fieldErrors = details.fieldErrors ?? {};
    this.correlationId = details.correlationId;
    this.isNetworkError = details.isNetworkError ?? false;
  }

  /** The request never got an answer: offline, DNS, refused connection, CORS. */
  static network(cause: unknown): ApiError {
    return new ApiError(NETWORK_ERROR_MESSAGE, 0, { isNetworkError: true, cause });
  }

  /** An unsuccessful response; its body is read as a problem document when it is one. */
  static async fromResponse(response: Response): Promise<ApiError> {
    const correlationId = response.headers.get('X-Correlation-Id') ?? undefined;
    const problem = isJson(response) ? parseProblem(await readJson(response)) : undefined;
    if (problem === undefined) {
      return new ApiError(
        `The server answered with an error (${String(response.status)}). Please try again.`,
        response.status,
        { correlationId },
      );
    }
    return new ApiError(
      problem.detail ?? problem.title ?? `Request failed (${String(response.status)}).`,
      response.status,
      {
        type: problemSlug(problem.type),
        title: problem.title,
        detail: problem.detail,
        fieldErrors: groupFieldErrors(problem.errors ?? []),
        correlationId,
      },
    );
  }
}

function isJson(response: Response): boolean {
  const contentType = response.headers.get('Content-Type') ?? '';
  return contentType.includes('json');
}

async function readJson(response: Response): Promise<unknown> {
  try {
    return (await response.json()) as unknown;
  } catch {
    return undefined;
  }
}
