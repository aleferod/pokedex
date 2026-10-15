import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { ApiError } from '@/shared/api';

import {
  createRecord,
  deleteRecord,
  fetchLocalPage,
  fetchLocalRecord,
  patchRecord,
  replaceRecord,
  syncFirst,
  syncOne,
} from './api';
import type { LocalListParams } from './localParams';
import type { ProprietaryBody } from './rules';
import type { LocalPokemon } from './types';

export const localKeys = {
  lists: () => ['local', 'list'] as const,
  list: (params: LocalListParams) => ['local', 'list', params] as const,
  record: (id: string) => ['local', 'record', id] as const,
};

/** The list keeps the previous page on screen while the next one loads. */
export function useLocalPage(params: LocalListParams) {
  return useQuery({
    queryKey: localKeys.list(params),
    queryFn: ({ signal }) => fetchLocalPage(params, signal),
    placeholderData: keepPreviousData,
  });
}

export function useLocalRecord(id: string) {
  return useQuery({
    queryKey: localKeys.record(id),
    queryFn: ({ signal }) => fetchLocalRecord(id, signal),
  });
}

/** After a write: the record as the API returned it, and every list loaded again. */
export function useStoreRecord() {
  const queryClient = useQueryClient();
  return (record: LocalPokemon) => {
    queryClient.setQueryData(localKeys.record(record.id), record);
    void queryClient.invalidateQueries({ queryKey: localKeys.lists() });
  };
}

export function useSyncOne() {
  const store = useStoreRecord();
  return useMutation({
    mutationFn: syncOne,
    onSuccess: ({ record }) => {
      store(record);
    },
  });
}

export function useSyncFirst() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: syncFirst,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['local'] });
    },
  });
}

/**
 * Tags edited in place, optimistically: the new tags show at once, a refusal puts the old ones
 * back, and a 409 (someone else saved first) loads the latest version.
 */
export function useTagChange(id: string) {
  const queryClient = useQueryClient();
  const store = useStoreRecord();
  const key = localKeys.record(id);
  return useMutation({
    mutationFn: ({ version, internalTags }: { version: number; internalTags: string[] }) =>
      patchRecord(id, version, { internalTags }),
    onMutate: async ({ internalTags }) => {
      await queryClient.cancelQueries({ queryKey: key });
      const previous = queryClient.getQueryData<LocalPokemon>(key);
      if (previous) {
        queryClient.setQueryData<LocalPokemon>(key, { ...previous, internalTags });
      }
      return { previous };
    },
    onError: (error, _change, context) => {
      if (context?.previous) {
        queryClient.setQueryData(key, context.previous);
      }
      if (error instanceof ApiError && error.status === 409) {
        void queryClient.invalidateQueries({ queryKey: key });
      }
    },
    onSuccess: store,
  });
}

/** Deleting: lists reload either way; a 409 reloads the record so the latest version shows. */
export function useDeleteRecord(id: string) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (version: number) => deleteRecord(id, version),
    onSettled: (_result, error) => {
      void queryClient.invalidateQueries({ queryKey: localKeys.lists() });
      if (error instanceof ApiError && error.status === 409) {
        void queryClient.invalidateQueries({ queryKey: localKeys.record(id) });
      }
    },
  });
}

export function useCreateRecord() {
  const store = useStoreRecord();
  return useMutation({ mutationFn: createRecord, onSuccess: store });
}

export function useReplaceRecord(id: string) {
  const store = useStoreRecord();
  return useMutation({
    mutationFn: ({ version, body }: { version: number; body: ProprietaryBody }) =>
      replaceRecord(id, version, body),
    onSuccess: store,
  });
}
