import type { ReactNode } from 'react';

/** A form-level message, announced as soon as it appears. */
export function FormAlert({ children }: { children: ReactNode }) {
  return (
    <div role="alert" className="rounded-md border border-red-300 bg-red-50 px-3 py-2 text-red-800">
      {children}
    </div>
  );
}
