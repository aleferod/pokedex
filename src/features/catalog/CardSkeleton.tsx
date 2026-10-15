/** A card-sized placeholder, so the grid does not jump when data arrives. */
export function CardSkeleton() {
  return (
    <div
      data-testid="card-skeleton"
      aria-hidden="true"
      className="h-64 animate-pulse rounded-lg border border-slate-200 bg-white p-4"
    >
      <div className="mx-auto h-24 w-24 rounded-full bg-slate-200" />
      <div className="mt-4 h-4 w-1/3 rounded bg-slate-200" />
      <div className="mt-2 h-5 w-2/3 rounded bg-slate-200" />
      <div className="mt-2 h-4 w-1/2 rounded bg-slate-200" />
    </div>
  );
}
