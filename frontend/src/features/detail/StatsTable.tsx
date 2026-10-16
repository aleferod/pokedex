import type { BaseStats } from './types';

const STATS: readonly (readonly [keyof BaseStats, string])[] = [
  ['hp', 'HP'],
  ['attack', 'Attack'],
  ['defense', 'Defense'],
  ['specialAttack', 'Special Attack'],
  ['specialDefense', 'Special Defense'],
  ['speed', 'Speed'],
];

/** The highest base stat in the games, so a full bar means the maximum possible. */
const MAX_BASE_STAT = 255;

/** The six base stats as name and value pairs; the bars are visual only. */
export function StatsTable({ stats }: { stats: BaseStats }) {
  const total = STATS.reduce((sum, [key]) => sum + stats[key], 0);
  return (
    <table className="w-full">
      <caption className="mb-3 text-left text-xl font-semibold">Base stats</caption>
      <tbody>
        {STATS.map(([key, label]) => (
          <tr key={key}>
            <th
              scope="row"
              className="py-1 pr-3 text-left font-medium whitespace-nowrap text-slate-700"
            >
              {label}
            </th>
            <td className="w-12 py-1 pr-3 text-right tabular-nums">{stats[key]}</td>
            <td aria-hidden="true" className="w-full py-1">
              <div className="h-2 rounded-full bg-slate-200">
                <div
                  className="h-2 rounded-full bg-red-600"
                  style={{ width: `${String(Math.min(100, (stats[key] / MAX_BASE_STAT) * 100))}%` }}
                />
              </div>
            </td>
          </tr>
        ))}
      </tbody>
      <tfoot className="border-t border-slate-200">
        <tr>
          <th scope="row" className="pt-2 pr-3 text-left font-semibold">
            Total
          </th>
          <td className="pt-2 pr-3 text-right font-semibold tabular-nums">{total}</td>
          <td aria-hidden="true" />
        </tr>
      </tfoot>
    </table>
  );
}
