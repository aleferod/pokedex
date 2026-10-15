import { useQueryClient } from '@tanstack/react-query';
import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router';

import { displayName } from '@/features/catalog';
import { ApiError } from '@/shared/api';
import { withFlash } from '@/shared/ui/flash';
import { FormAlert } from '@/shared/ui/form';
import { queryErrorMessage } from '@/shared/ui/queryErrorMessage';

import type { LocalPokemon } from './types';
import { localKeys, useDeleteRecord } from './useLocal';

const CONFLICT =
  'Someone else changed this record, so it wasn’t deleted. Check the latest version first.';
const FORBIDDEN = 'Only administrators can delete records.';

/** US04, administrators only: delete after an inline confirmation, for the version shown. */
export function DeleteRecord({ record }: { record: LocalPokemon }) {
  const name = displayName(record.name);
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const remove = useDeleteRecord(record.id);
  const [confirming, setConfirming] = useState(false);
  const [problem, setProblem] = useState<string | null>(null);
  const deleteButton = useRef<HTMLButtonElement>(null);
  const cancelButton = useRef<HTMLButtonElement>(null);
  const opened = useRef(false);

  // Focus follows the confirmation: Cancel (the safe choice) when it opens, Delete when it closes.
  useEffect(() => {
    if (confirming) {
      opened.current = true;
      cancelButton.current?.focus();
    } else if (opened.current) {
      opened.current = false;
      deleteButton.current?.focus();
    }
  }, [confirming]);

  const leave = (message: string) => {
    void navigate('/local', { state: withFlash(message) });
    queryClient.removeQueries({ queryKey: localKeys.record(record.id) });
  };

  const confirm = async () => {
    setProblem(null);
    try {
      await remove.mutateAsync(record.version);
      leave(`${name} was deleted.`);
    } catch (error) {
      if (error instanceof ApiError && error.status === 404) {
        leave(`${name} was already deleted.`);
        return;
      }
      setConfirming(false);
      setProblem(
        error instanceof ApiError && error.status === 409
          ? CONFLICT
          : error instanceof ApiError && error.status === 403
            ? FORBIDDEN
            : queryErrorMessage(error),
      );
    }
  };

  return (
    <div className="space-y-3">
      {problem !== null && <FormAlert>{problem}</FormAlert>}
      {confirming ? (
        <div
          role="group"
          aria-labelledby="delete-title"
          className="space-y-3 rounded-lg border border-red-300 bg-red-50 p-4"
        >
          <h2 id="delete-title" className="text-lg font-semibold text-red-900">
            Delete {name}?
          </h2>
          <p className="text-red-900">
            This removes it from the local Pokédex for everyone. It can’t be undone.
          </p>
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              disabled={remove.isPending}
              onClick={() => {
                void confirm();
              }}
              className="rounded-md bg-red-700 px-4 py-2 font-semibold text-white hover:bg-red-800 disabled:opacity-70"
            >
              {remove.isPending ? 'Deleting…' : `Delete ${name}`}
            </button>
            <button
              ref={cancelButton}
              type="button"
              onClick={() => {
                setConfirming(false);
              }}
              className="rounded-md border border-slate-300 bg-white px-4 py-2 font-semibold hover:border-slate-500"
            >
              Cancel
            </button>
          </div>
        </div>
      ) : (
        <button
          ref={deleteButton}
          type="button"
          onClick={() => {
            setProblem(null);
            setConfirming(true);
          }}
          className="rounded-md border border-red-600 bg-white px-4 py-2 font-semibold text-red-700 hover:bg-red-50"
        >
          Delete
        </button>
      )}
    </div>
  );
}
