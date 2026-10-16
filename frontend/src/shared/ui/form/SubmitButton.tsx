import type { ReactNode } from 'react';

/**
 * The form's submit button: disabled and relabelled while the request runs.
 *
 * Pressing it doesn't take focus from the field being edited. Otherwise the press would blur that
 * field, its blur-time validation would add error messages above the button and move it, and the
 * release would land outside the button: no click. The submit validates every field and focuses
 * the first invalid one instead. Keyboard submission is unaffected.
 */
export function SubmitButton({
  busy,
  busyLabel,
  children,
}: {
  busy: boolean;
  busyLabel: string;
  children: ReactNode;
}) {
  return (
    <button
      type="submit"
      disabled={busy}
      aria-busy={busy}
      onMouseDown={(event) => {
        event.preventDefault();
      }}
      className="w-full rounded-md bg-red-600 px-4 py-2 font-semibold text-white hover:bg-red-700 disabled:cursor-wait disabled:opacity-70"
    >
      {busy ? busyLabel : children}
    </button>
  );
}
