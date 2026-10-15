export interface PaginationProps {
  /** Current page, 1-based. */
  page: number;
  totalPages: number;
  size: number;
  sizes?: readonly number[];
  onPageChange: (page: number) => void;
  onSizeChange: (size: number) => void;
}

const buttonClass =
  'rounded-md border border-slate-300 bg-white px-3 py-1.5 font-medium hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-50';

/**
 * Previous/next, "Page N of M" and a page-size choice. Controlled: it knows nothing about URLs or
 * data, so any paginated list can use it.
 */
export function Pagination({
  page,
  totalPages,
  size,
  sizes = [20, 40, 60],
  onPageChange,
  onSizeChange,
}: PaginationProps) {
  const lastPage = Math.max(totalPages, 1);
  return (
    <nav aria-label="Pagination" className="flex flex-wrap items-center justify-between gap-3 py-4">
      <div className="flex items-center gap-2">
        <button
          type="button"
          className={buttonClass}
          disabled={page <= 1}
          onClick={() => {
            onPageChange(page - 1);
          }}
        >
          <span aria-hidden="true">← </span>
          <span className="sr-only">Previous page</span>
          <span aria-hidden="true">Previous</span>
        </button>
        <span className="px-2 text-slate-700">
          Page {page} of {lastPage}
        </span>
        <button
          type="button"
          className={buttonClass}
          disabled={page >= lastPage}
          onClick={() => {
            onPageChange(page + 1);
          }}
        >
          <span className="sr-only">Next page</span>
          <span aria-hidden="true">Next</span>
          <span aria-hidden="true"> →</span>
        </button>
      </div>
      <label className="flex items-center gap-2 text-slate-700">
        Per page
        <select
          value={size}
          onChange={(event) => {
            onSizeChange(Number(event.target.value));
          }}
          className="rounded-md border border-slate-300 bg-white px-2 py-1"
        >
          {sizes.map((option) => (
            <option key={option} value={option}>
              {option}
            </option>
          ))}
        </select>
      </label>
    </nav>
  );
}
