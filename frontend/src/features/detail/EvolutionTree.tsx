import { Link } from 'react-router';

import { type CatalogReturnState, displayName } from '@/features/catalog';

import type { EvolutionStage } from './types';

interface TreeProps {
  currentId: number;
  linkState: CatalogReturnState;
}

/** The whole family from its base form; each stage links to its page and says how it is reached. */
export function EvolutionTree({ chain, ...props }: TreeProps & { chain: EvolutionStage }) {
  if (chain.evolvesTo.length === 0) {
    return <p className="text-slate-700">{displayName(chain.name)} does not evolve.</p>;
  }
  return (
    <ul>
      <StageItem stage={chain} {...props} />
    </ul>
  );
}

function StageItem({ stage, currentId, linkState }: TreeProps & { stage: EvolutionStage }) {
  const name = displayName(stage.name);
  const current = stage.id === currentId;
  return (
    <li className="space-y-2">
      <div className="flex flex-wrap items-center gap-x-2 gap-y-1">
        {stage.condition !== null && (
          <span className="text-sm text-slate-600">
            <span aria-hidden="true">↳ </span>
            {stage.condition}
          </span>
        )}
        <Link
          to={`/pokemon/${String(stage.id)}`}
          state={linkState}
          aria-current={current ? 'page' : undefined}
          className={`rounded-full border px-3 py-1 font-semibold ${
            current
              ? 'border-red-600 bg-red-600 text-white'
              : 'border-slate-300 bg-white text-slate-900 hover:border-red-600'
          }`}
        >
          {name}
        </Link>
      </div>
      {stage.evolvesTo.length > 0 && (
        <ul
          aria-label={`Evolutions of ${name}`}
          className="ml-3 space-y-2 border-l-2 border-slate-200 pl-4"
        >
          {stage.evolvesTo.map((next) => (
            <StageItem key={next.id} stage={next} currentId={currentId} linkState={linkState} />
          ))}
        </ul>
      )}
    </li>
  );
}
