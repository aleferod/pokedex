/*
 * One colour per type. White text sits on -700/-800 shades, and the light ones (electric, fairy)
 * take dark text, so every badge keeps at least 4.5:1 contrast.
 */
const TYPE_CLASSES: Partial<Record<string, string>> = {
  normal: 'bg-stone-600 text-white',
  fire: 'bg-orange-700 text-white',
  water: 'bg-blue-700 text-white',
  electric: 'bg-yellow-300 text-slate-900',
  grass: 'bg-green-700 text-white',
  ice: 'bg-cyan-800 text-white',
  fighting: 'bg-red-800 text-white',
  poison: 'bg-purple-700 text-white',
  ground: 'bg-amber-800 text-white',
  flying: 'bg-indigo-600 text-white',
  psychic: 'bg-pink-700 text-white',
  bug: 'bg-lime-800 text-white',
  rock: 'bg-yellow-800 text-white',
  ghost: 'bg-violet-800 text-white',
  dragon: 'bg-indigo-800 text-white',
  dark: 'bg-neutral-800 text-white',
  steel: 'bg-slate-600 text-white',
  fairy: 'bg-pink-300 text-slate-900',
};

const NEUTRAL = 'bg-slate-600 text-white';

export function TypeBadges({ types }: { types: readonly string[] }) {
  return (
    <ul aria-label="Types" className="flex flex-wrap gap-2">
      {types.map((type) => (
        <li
          key={type}
          className={`rounded-full px-3 py-0.5 text-sm font-semibold ${TYPE_CLASSES[type] ?? NEUTRAL}`}
        >
          {type}
        </li>
      ))}
    </ul>
  );
}
