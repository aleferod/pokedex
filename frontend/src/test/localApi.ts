import { http, HttpResponse } from 'msw';

import type { LocalPokemon } from '@/features/local/types';

import { problemResponse } from './authApi';
import { server } from './server';

/**
 * An in-memory pokedex-api for `/api/v1/local-pokemon`: it lists, filters and pages like the
 * API, checks `If-Match` (428 when missing, 409 when stale), bumps versions, and syncs from a small
 * upstream catalog. Every request is recorded with its method, path, query, `If-Match` and body.
 * It does not validate bodies: tests that need the API's 400s answer them with `server.use`.
 */

export interface RecordedRequest {
  method: string;
  path: string;
  search: URLSearchParams;
  ifMatch: string | null;
  body: unknown;
}

const BASE = '/api/v1/local-pokemon';
const NOW = '2026-10-15T12:00:00Z';
const SPRITES = 'https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon';

let nextId = 1;

/** A UUID that is stable within a test run and readable in failures. */
export function recordId(n: number = nextId++): string {
  return `00000000-0000-4000-8000-${String(n).padStart(12, '0')}`;
}

function upstream(pokeapiId: number, name: string, overrides: Partial<LocalPokemon> = {}) {
  return {
    pokeapiId,
    name,
    spriteUrl: `${SPRITES}/${String(pokeapiId)}.png`,
    imageUrl: `${SPRITES}/other/official-artwork/${String(pokeapiId)}.png`,
    category: 'Seed Pokémon',
    types: ['grass', 'poison'],
    height: 0.7,
    weight: 6.9,
    abilities: ['overgrow', 'chlorophyll'],
    stats: { hp: 45, attack: 49, defense: 49, specialAttack: 65, specialDefense: 65, speed: 45 },
    ...overrides,
  };
}

/** What PokeAPI knows, in catalog order: the source of syncs and creates. */
export const UPSTREAM = [
  upstream(1, 'bulbasaur'),
  upstream(2, 'ivysaur', { height: 1, weight: 13 }),
  upstream(3, 'venusaur', { height: 2, weight: 100 }),
  upstream(25, 'pikachu', {
    category: 'Mouse Pokémon',
    types: ['electric'],
    height: 0.4,
    weight: 6,
    abilities: ['static', 'lightning-rod'],
    stats: { hp: 35, attack: 55, defense: 40, specialAttack: 50, specialDefense: 50, speed: 90 },
  }),
];

/** A stored record: Pikachu with proprietary data, unless overridden. */
export function localRecord(overrides: Partial<LocalPokemon> = {}): LocalPokemon {
  const pokeapiId = overrides.pokeapiId ?? 25;
  const source = UPSTREAM.find((p) => p.pokeapiId === pokeapiId) ?? upstream(pokeapiId, 'mon');
  return {
    id: recordId(),
    ...source,
    localizedNames: { 'pt-BR': 'Pikachu' },
    region: 'Kanto',
    habitatNotes: 'Forests near power plants',
    internalTags: ['starter-line', 'electric'],
    notes: 'Loves ketchup',
    createdAt: '2026-10-01T09:00:00Z',
    updatedAt: '2026-10-10T09:00:00Z',
    syncedAt: '2026-10-01T09:00:00Z',
    createdBy: 'ash',
    version: 3,
    ...overrides,
  };
}

const PROPRIETARY = ['localizedNames', 'region', 'habitatNotes', 'internalTags', 'notes'] as const;

function emptyProprietary() {
  return { localizedNames: {}, region: null, habitatNotes: null, internalTags: [], notes: null };
}

export function localApi(initial: LocalPokemon[] = [], options: { user?: string } = {}) {
  const records = new Map(initial.map((record) => [record.id, structuredClone(record)]));
  const requests: RecordedRequest[] = [];
  const user = options.user ?? 'trainer';
  let held: Promise<void> | null = null;

  /** Records the request; a write then waits while `hold()` is on. */
  async function remember(request: Request): Promise<void> {
    const url = new URL(request.url);
    const text = await request.clone().text();
    requests.push({
      method: request.method,
      path: url.pathname,
      search: url.searchParams,
      ifMatch: request.headers.get('If-Match'),
      body: text === '' ? undefined : (JSON.parse(text) as unknown),
    });
    if (held !== null && request.method !== 'GET') {
      await held;
    }
  }

  function find(id: string) {
    return records.get(id);
  }

  function byNumberOrName(idOrName: string) {
    return UPSTREAM.find((p) => String(p.pokeapiId) === idOrName || p.name === idOrName);
  }

  /** 428 without If-Match, 409 when it names an older version, otherwise nothing. */
  function precondition(request: Request, record: LocalPokemon, required: boolean) {
    const ifMatch = request.headers.get('If-Match');
    if (ifMatch === null) {
      return required
        ? problemResponse(428, 'precondition-required', 'This request needs If-Match.')
        : null;
    }
    return ifMatch === `"${String(record.version)}"`
      ? null
      : problemResponse(409, 'conflict', 'The local Pokémon was changed by another request.');
  }

  function save(record: LocalPokemon, status = 200) {
    records.set(record.id, record);
    return HttpResponse.json(record, {
      status,
      headers: { ETag: `"${String(record.version)}"`, Location: `${BASE}/${record.id}` },
    });
  }

  function sync(source: (typeof UPSTREAM)[number]) {
    const existing = [...records.values()].find((r) => r.pokeapiId === source.pokeapiId);
    if (existing) {
      return {
        status: 200,
        record: {
          ...existing,
          ...source,
          syncedAt: NOW,
          updatedAt: NOW,
          version: existing.version + 1,
        },
      };
    }
    return {
      status: 201,
      record: {
        id: recordId(),
        ...source,
        ...emptyProprietary(),
        createdAt: NOW,
        updatedAt: NOW,
        syncedAt: NOW,
        createdBy: user,
        version: 0,
      } satisfies LocalPokemon,
    };
  }

  server.use(
    http.get(BASE, async ({ request }) => {
      await remember(request);
      const params = new URL(request.url).searchParams;
      const page = Number(params.get('page') ?? 0);
      const size = Number(params.get('size') ?? 20);
      const name = params.get('name')?.toLowerCase();
      const tag = params.get('tag')?.toLowerCase();
      const matching = [...records.values()]
        .filter((r) => name === undefined || r.name.includes(name))
        .filter((r) => tag === undefined || r.internalTags.includes(tag))
        .sort((a, b) => a.pokeapiId - b.pokeapiId);
      return HttpResponse.json({
        content: matching.slice(page * size, page * size + size),
        page,
        size,
        totalElements: matching.length,
        totalPages: Math.ceil(matching.length / size),
      });
    }),

    http.get(`${BASE}/:id`, async ({ request, params }) => {
      await remember(request);
      const record = find(String(params.id));
      return record
        ? HttpResponse.json(record, { headers: { ETag: `"${String(record.version)}"` } })
        : problemResponse(404, 'not-found', 'No local Pokémon with this id.');
    }),

    http.post(`${BASE}/sync/:idOrName`, async ({ request, params }) => {
      await remember(request);
      const idOrName = String(params.idOrName);
      const source = byNumberOrName(idOrName);
      if (!source) {
        return problemResponse(404, 'not-found', `No Pokémon found for '${idOrName}'.`);
      }
      const { status, record } = sync(source);
      return save(record, status);
    }),

    http.post(`${BASE}/sync`, async ({ request }) => {
      await remember(request);
      const limit = Number(new URL(request.url).searchParams.get('limit') ?? 10);
      const results = UPSTREAM.slice(0, limit).map((source) => {
        const { status, record } = sync(source);
        records.set(record.id, record);
        return {
          pokeapiId: source.pokeapiId,
          name: source.name,
          status: status === 201 ? 'created' : 'updated',
        };
      });
      return HttpResponse.json({
        results,
        created: results.filter((r) => r.status === 'created').length,
        updated: results.filter((r) => r.status === 'updated').length,
      });
    }),

    http.post(BASE, async ({ request }) => {
      await remember(request);
      const body = (await request.json()) as { pokeapiId: number } & Partial<LocalPokemon>;
      if ([...records.values()].some((r) => r.pokeapiId === body.pokeapiId)) {
        return problemResponse(409, 'conflict', 'This Pokémon is already stored locally.', [
          { field: 'pokeapiId', message: 'is already stored locally' },
        ]);
      }
      const source = byNumberOrName(String(body.pokeapiId));
      if (!source) {
        return problemResponse(400, 'invalid-request', 'The request is invalid.', [
          { field: 'pokeapiId', message: 'no Pokémon has this number' },
        ]);
      }
      const { record } = sync(source);
      return save({ ...record, ...emptyProprietary(), ...pick(body) }, 201);
    }),

    http.put(`${BASE}/:id`, async ({ request, params }) => {
      await remember(request);
      const record = find(String(params.id));
      if (!record) {
        return problemResponse(404, 'not-found', 'No local Pokémon with this id.');
      }
      const failed = precondition(request, record, true);
      if (failed) {
        return failed;
      }
      const body = (await request.json()) as Partial<LocalPokemon>;
      return save({
        ...record,
        ...emptyProprietary(),
        ...pick(body),
        updatedAt: NOW,
        version: record.version + 1,
      });
    }),

    http.patch(`${BASE}/:id`, async ({ request, params }) => {
      await remember(request);
      const record = find(String(params.id));
      if (!record) {
        return problemResponse(404, 'not-found', 'No local Pokémon with this id.');
      }
      const failed = precondition(request, record, true);
      if (failed) {
        return failed;
      }
      const body = (await request.json()) as Partial<LocalPokemon>;
      return save({ ...record, ...pick(body), updatedAt: NOW, version: record.version + 1 });
    }),

    http.delete(`${BASE}/:id`, async ({ request, params }) => {
      await remember(request);
      const record = find(String(params.id));
      if (!record) {
        return problemResponse(404, 'not-found', 'No local Pokémon with this id.');
      }
      const failed = precondition(request, record, false);
      if (failed) {
        return failed;
      }
      records.delete(record.id);
      return new HttpResponse(null, { status: 204 });
    }),
  );

  return {
    requests,
    records,
    /** Requests other than reads, in order. */
    writes: () => requests.filter((r) => r.method !== 'GET'),
    /** Writes wait (reads don't) until the returned function is called. */
    hold(): () => void {
      let release: () => void = () => {
        // Replaced by the promise's resolve below.
      };
      held = new Promise<void>((resolve) => {
        release = resolve;
      });
      return () => {
        held = null;
        release();
      };
    },
    /** Someone else saves the record: its version moves on and `changes` apply. */
    changeElsewhere(id: string, changes: Partial<LocalPokemon> = {}) {
      const record = find(id);
      if (record) {
        records.set(id, { ...record, ...changes, version: record.version + 1 });
      }
    },
  };
}

/** Only the proprietary members of a body, as the API applies them. */
function pick(body: Partial<LocalPokemon>): Partial<LocalPokemon> {
  return Object.fromEntries(
    PROPRIETARY.filter((field) => field in body).map((field) => [field, body[field]]),
  );
}
