import type { ComponentProps } from 'react';

import { sentence } from './sentence';

export interface TextFieldProps extends Omit<ComponentProps<'input'>, 'id'> {
  id: string;
  label: string;
  hint?: string;
  errors?: readonly string[];
}

/**
 * A labelled input whose hint and errors are announced with it (`aria-describedby`) and whose
 * invalid state is exposed (`aria-invalid`). Spread React Hook Form's `register(...)` into it.
 */
export function TextField({
  id,
  label,
  hint,
  errors = [],
  className,
  ...inputProps
}: TextFieldProps) {
  const hintId = hint === undefined ? undefined : `${id}-hint`;
  const errorsId = errors.length === 0 ? undefined : `${id}-errors`;
  const describedBy = [errorsId, hintId].filter((value) => value !== undefined).join(' ');
  const invalid = errors.length > 0;

  return (
    <div className="space-y-1">
      <label htmlFor={id} className="block font-medium">
        {label}
      </label>
      <input
        id={id}
        aria-invalid={invalid}
        aria-describedby={describedBy === '' ? undefined : describedBy}
        className={`block w-full rounded-md border px-3 py-2 ${
          invalid ? 'border-red-600' : 'border-slate-300'
        } ${className ?? ''}`}
        {...inputProps}
      />
      {invalid && (
        <ul id={errorsId} data-testid={`${id}-errors`} className="space-y-0.5 text-sm text-red-700">
          {errors.map((message) => (
            <li key={message}>{sentence(message)}</li>
          ))}
        </ul>
      )}
      {hint !== undefined && (
        <p id={hintId} className="text-sm text-slate-600">
          {hint}
        </p>
      )}
    </div>
  );
}
