/** Navigation state carrying a one-time message to the next page ("Pikachu was deleted."). */
export interface FlashState {
  flash: string;
}

export function withFlash(message: string): FlashState {
  return { flash: message };
}

export function readFlash(state: unknown): string | null {
  return typeof state === 'object' &&
    state !== null &&
    'flash' in state &&
    typeof state.flash === 'string'
    ? state.flash
    : null;
}

/** The same state without its message, or null when nothing else is left. */
export function withoutFlash(state: unknown): unknown {
  if (typeof state !== 'object' || state === null || !('flash' in state)) {
    return state;
  }
  const rest = Object.fromEntries(Object.entries(state).filter(([key]) => key !== 'flash'));
  return Object.keys(rest).length > 0 ? rest : null;
}
