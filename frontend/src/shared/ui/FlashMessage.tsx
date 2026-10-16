import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router';

import { readFlash, withoutFlash } from './flash';

/**
 * Shows the message a navigation carried (`state: withFlash(...)`) on the page it led to, once:
 * the history entry is replaced without it, so a reload or Back doesn't repeat it, and it goes
 * away on the next navigation. The live region stays in the layout, so the message is announced.
 */
export function FlashMessage() {
  const location = useLocation();
  const navigate = useNavigate();
  const here = `${location.pathname}${location.search}`;
  const incoming = readFlash(location.state);
  const [shown, setShown] = useState<{ at: string; message: string } | null>(null);

  // Derived state, adjusted while rendering: take a new message, drop it once the address changes.
  if (incoming !== null && (shown?.at !== here || shown.message !== incoming)) {
    setShown({ at: here, message: incoming });
  } else if (incoming === null && shown !== null && shown.at !== here) {
    setShown(null);
  }

  const hash = location.hash;
  const state: unknown = location.state;
  useEffect(() => {
    if (incoming !== null) {
      void navigate(`${here}${hash}`, { replace: true, state: withoutFlash(state) });
    }
  }, [incoming, here, hash, state, navigate]);

  return (
    <div aria-live="polite">
      {shown !== null && shown.at === here && (
        <p className="mb-4 rounded-md border border-green-300 bg-green-50 px-3 py-2 text-green-900">
          {shown.message}
        </p>
      )}
    </div>
  );
}
