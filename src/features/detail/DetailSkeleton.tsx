/** The detail layout without content, so nothing jumps when the Pokémon arrives. */
export function DetailSkeleton() {
  return (
    <div
      data-testid="detail-skeleton"
      aria-hidden="true"
      className="grid animate-pulse gap-6 md:grid-cols-[minmax(0,1fr)_minmax(0,1.2fr)]"
    >
      <div className="aspect-square rounded-lg border border-slate-200 bg-white p-6">
        <div className="h-full w-full rounded-full bg-slate-200" />
      </div>
      <div className="space-y-4">
        <div className="h-4 w-16 rounded bg-slate-200" />
        <div className="h-8 w-1/2 rounded bg-slate-200" />
        <div className="h-6 w-1/3 rounded bg-slate-200" />
        <div className="h-20 rounded bg-slate-200" />
        <div className="h-24 rounded bg-slate-200" />
      </div>
    </div>
  );
}
