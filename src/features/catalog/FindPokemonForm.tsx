import { type SubmitEvent, useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router';

import { TextField } from '@/shared/ui/form';

import { catalogSearch, DEFAULT_SIZE, parseName, parseSize } from './catalogParams';
import { parsePokemonIdentifier } from './identifier';

const TOO_LITTLE = 'Type at least 3 letters of a name, or a Pokédex number';
const CHARACTERS = 'Use letters, digits, hyphens or spaces';
const TOO_LONG = 'Use at most 50 characters';
const MAX_NAME = 50;

/** How long typing has to pause before the catalog is searched. */
const SEARCH_PAUSE_MS = 300;

/** What the text asks for: a Pokémon to open, a search, the whole catalog, or not enough yet. */
type Intent =
  | { kind: 'open'; identifier: string }
  | { kind: 'search'; name: string }
  | { kind: 'clear' }
  | { kind: 'incomplete' }
  | { kind: 'invalid'; message: string };

function intentOf(text: string): Intent {
  const trimmed = text.trim();
  if (trimmed === '') {
    return { kind: 'clear' };
  }
  if (/^\d+$/.test(trimmed)) {
    const identifier = parsePokemonIdentifier(trimmed);
    return identifier === null ? { kind: 'incomplete' } : { kind: 'open', identifier };
  }
  if (!/^[A-Za-z0-9\s-]+$/.test(trimmed)) {
    return { kind: 'invalid', message: CHARACTERS };
  }
  if (trimmed.length > MAX_NAME) {
    return { kind: 'invalid', message: TOO_LONG };
  }
  const name = parseName(trimmed);
  return name === '' ? { kind: 'incomplete' } : { kind: 'search', name };
}

/**
 * "Find a Pokémon": part of a name searches the catalog (pokedex-api matches names containing it,
 * from 3 characters), and a Pokédex number opens that Pokémon.
 *
 * With `autoSearch` (on the catalog), the search follows the typing after a short pause and lives
 * in the URL (`?name=`), replacing the history entry rather than adding one per pause. The field
 * keeps its own text, so it is never re-mounted while typing; it follows the URL only when the
 * URL changes from outside (Back, "Clear search"). Elsewhere, submitting opens the catalog's
 * search results.
 */
export function FindPokemonForm({ autoSearch = false }: { autoSearch?: boolean }) {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const current = autoSearch ? parseName(searchParams.get('name')) : '';
  const size = autoSearch ? parseSize(searchParams.get('size')) : DEFAULT_SIZE;

  const [text, setText] = useState(current);
  const [seen, setSeen] = useState(current);
  const [written, setWritten] = useState(current);
  const [submitted, setSubmitted] = useState(false);

  // Derived state, adjusted while rendering: the URL changed and it wasn't this form's own search.
  if (current !== seen) {
    setSeen(current);
    if (current !== written && parseName(text) !== current) {
      setText(current);
      setWritten(current);
    }
  }

  const intent = intentOf(text);
  const target = intent.kind === 'search' ? intent.name : intent.kind === 'clear' ? '' : null;

  useEffect(() => {
    if (!autoSearch || target === null || target === current) {
      return undefined;
    }
    const timer = setTimeout(() => {
      setWritten(target);
      setSearchParams(new URLSearchParams(catalogSearch(1, size, target)), { replace: true });
    }, SEARCH_PAUSE_MS);
    return () => {
      clearTimeout(timer);
    };
  }, [autoSearch, target, current, size, setSearchParams]);

  const onSubmit = (event: SubmitEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSubmitted(true);
    if (intent.kind === 'open') {
      void navigate(`/pokemon/${intent.identifier}`);
    } else if (target !== null && autoSearch) {
      setWritten(target);
      setSearchParams(new URLSearchParams(catalogSearch(1, size, target)), { replace: true });
    } else if (intent.kind === 'search') {
      void navigate(`/pokemon${catalogSearch(1, DEFAULT_SIZE, intent.name)}`);
    }
  };

  const errors =
    intent.kind === 'invalid'
      ? [intent.message]
      : submitted && intent.kind === 'incomplete'
        ? [TOO_LITTLE]
        : [];

  return (
    <form
      role="search"
      noValidate
      onSubmit={onSubmit}
      className="flex w-full max-w-sm items-start gap-2"
    >
      <div className="flex-1">
        <TextField
          id="find-pokemon"
          label="Find a Pokémon"
          hint="Part of a name (3 letters or more), or a Pokédex number."
          autoComplete="off"
          value={text}
          errors={errors}
          onChange={(event) => {
            setText(event.target.value);
            setSubmitted(false);
          }}
        />
      </div>
      <button
        type="submit"
        className="mt-7 rounded-md bg-red-600 px-4 py-2 font-semibold text-white hover:bg-red-700"
      >
        Find
      </button>
    </form>
  );
}
