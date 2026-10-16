import { queryErrorMessage } from './queryErrorMessage';

/** A failed data view: what went wrong and a way to try again. */
export function QueryError({
  title,
  error,
  onRetry,
}: {
  title: string;
  error: unknown;
  onRetry: () => void;
}) {
  return (
    <div role="alert" className="space-y-3 rounded-md border border-red-300 bg-red-50 p-4">
      <h2 className="font-semibold text-red-900">{title}</h2>
      <p className="text-red-800">{queryErrorMessage(error)}</p>
      <button
        type="button"
        onClick={onRetry}
        className="rounded-md bg-red-600 px-3 py-1.5 font-semibold text-white hover:bg-red-700"
      >
        Try again
      </button>
    </div>
  );
}
