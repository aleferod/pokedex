import type { ReactNode } from 'react';

/** The form's submit button: disabled and relabelled while the request runs. */
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
      className="w-full rounded-md bg-red-600 px-4 py-2 font-semibold text-white hover:bg-red-700 disabled:cursor-wait disabled:opacity-70"
    >
      {busy ? busyLabel : children}
    </button>
  );
}
