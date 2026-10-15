import { apiRequest, apiResponse, type Page } from '@/shared/api';

import type { LocalListParams } from './localParams';
import type { CreateBody, ProprietaryBody } from './rules';
import type { BulkSyncResult, LocalPokemon } from './types';

const BASE = '/local-pokemon';

/** One page of the local records; `page` is 1-based here and 0-based in the API. */
export function fetchLocalPage(
  { page, size, name, tag }: LocalListParams,
  signal?: AbortSignal,
): Promise<Page<LocalPokemon>> {
  const query = new URLSearchParams({ page: String(page - 1), size: String(size) });
  if (name !== '') {
    query.set('name', name);
  }
  if (tag !== '') {
    query.set('tag', tag);
  }
  return apiRequest<Page<LocalPokemon>>(`${BASE}?${query.toString()}`, signal ? { signal } : {});
}

export function fetchLocalRecord(id: string, signal?: AbortSignal): Promise<LocalPokemon> {
  return apiRequest<LocalPokemon>(`${BASE}/${encodeURIComponent(id)}`, signal ? { signal } : {});
}

/** Imports a Pokémon (201, `imported`) or refreshes its PokeAPI data (200). */
export async function syncOne(
  identifier: string,
): Promise<{ record: LocalPokemon; imported: boolean }> {
  const { status, body } = await apiResponse<LocalPokemon>(
    `${BASE}/sync/${encodeURIComponent(identifier)}`,
    { method: 'POST' },
  );
  return { record: body, imported: status === 201 };
}

export function syncFirst(limit: number): Promise<BulkSyncResult> {
  return apiRequest<BulkSyncResult>(`${BASE}/sync?limit=${String(limit)}`, { method: 'POST' });
}

/** The `If-Match` value for the version the user saw: the API's ETag is the version in quotes. */
export function ifMatch(version: number): Record<string, string> {
  return { 'If-Match': `"${String(version)}"` };
}

type ProprietaryPatch = Partial<
  Pick<LocalPokemon, 'localizedNames' | 'region' | 'habitatNotes' | 'internalTags' | 'notes'>
>;

/** JSON Merge Patch of proprietary fields, for the version the user saw. */
export function patchRecord(
  id: string,
  version: number,
  patch: ProprietaryPatch,
): Promise<LocalPokemon> {
  return apiRequest<LocalPokemon>(`${BASE}/${encodeURIComponent(id)}`, {
    method: 'PATCH',
    body: patch,
    headers: ifMatch(version),
  });
}

/** Administrators only; the API checks the version too. */
export function deleteRecord(id: string, version: number): Promise<void> {
  return apiRequest<undefined>(`${BASE}/${encodeURIComponent(id)}`, {
    method: 'DELETE',
    headers: ifMatch(version),
  });
}

/** Stores a Pokémon by number with its proprietary fields (201). */
export function createRecord(body: CreateBody): Promise<LocalPokemon> {
  return apiRequest<LocalPokemon>(BASE, { method: 'POST', body });
}

/** Replaces every proprietary field, for the version the form loaded. */
export function replaceRecord(
  id: string,
  version: number,
  body: ProprietaryBody,
): Promise<LocalPokemon> {
  return apiRequest<LocalPokemon>(`${BASE}/${encodeURIComponent(id)}`, {
    method: 'PUT',
    body,
    headers: ifMatch(version),
  });
}
