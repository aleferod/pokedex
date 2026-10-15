import { screen, waitFor, within } from '@testing-library/react';
import { userEvent } from '@testing-library/user-event';
import { http } from 'msw';
import { afterEach, describe, expect, it } from 'vitest';

import { saveSession } from '@/features/auth/session';
import { problemResponse } from '@/test/authApi';
import { localApi, localRecord, recordId } from '@/test/localApi';
import { renderRoute } from '@/test/renderRoute';
import { server } from '@/test/server';
import { testSession } from '@/test/sessions';

const PIKACHU = localRecord({ id: recordId(7001) });
const PAGE = `/local/${PIKACHU.id}`;

function signInAs(role: 'USER' | 'ADMIN') {
  saveSession(testSession(role === 'ADMIN' ? { role, username: 'admin' } : { role }));
}

async function openPikachu(role: 'USER' | 'ADMIN' = 'USER', records = [PIKACHU]) {
  signInAs(role);
  const api = localApi(records);
  const view = renderRoute(PAGE);
  await screen.findByRole('heading', { level: 1, name: 'Pikachu' });
  return { api, ...view };
}

function section(name: string) {
  return within(screen.getByRole('region', { name }));
}

function tags(): string[] {
  return within(screen.getByRole('list', { name: 'Tags' }))
    .queryAllByRole('listitem')
    .map((item) => item.firstChild?.textContent ?? '');
}

describe('a record’s page', () => {
  afterEach(() => {
    sessionStorage.clear();
  });

  it('shows the PokeAPI data as read-only and the proprietary fields', async () => {
    await openPikachu();

    expect(screen.getByText('#25')).toBeInTheDocument();
    const upstream = section('From PokeAPI');
    expect(upstream.getByText(/read-only/i)).toBeInTheDocument();
    expect(upstream.getByRole('list', { name: 'Types' })).toHaveTextContent('electric');
    expect(upstream.getByText('Mouse Pokémon')).toBeInTheDocument();
    expect(upstream.getByText('0.4 m')).toBeInTheDocument();
    expect(upstream.getByText('6 kg')).toBeInTheDocument();
    expect(upstream.getByRole('table', { name: 'Base stats' })).toHaveTextContent('Speed90');
    expect(upstream.getByText(/Synced/).querySelector('time')).toHaveAttribute(
      'dateTime',
      PIKACHU.syncedAt,
    );

    const own = section('Your data');
    expect(own.getByText('Kanto')).toBeInTheDocument();
    expect(own.getByText('Forests near power plants')).toBeInTheDocument();
    expect(own.getByText('Loves ketchup')).toBeInTheDocument();
    expect(own.getByRole('list', { name: 'Localized names' })).toHaveTextContent('pt-BR: Pikachu');
    expect(tags()).toEqual(['starter-line', 'electric']);

    expect(screen.getByText(/Added by ash/).querySelector('time')).toHaveAttribute(
      'dateTime',
      PIKACHU.createdAt,
    );
    expect(screen.getByRole('link', { name: 'Edit' })).toHaveAttribute('href', `${PAGE}/edit`);
    expect(screen.getByRole('link', { name: 'View on the public page' })).toHaveAttribute(
      'href',
      '/pokemon/25',
    );
    await waitFor(() => {
      expect(document.title).toBe('Pikachu · My Pokédex · Pokédex');
    });
  });

  it('says which proprietary fields are still empty', async () => {
    await openPikachu('USER', [
      localRecord({
        id: PIKACHU.id,
        region: null,
        habitatNotes: null,
        notes: null,
        localizedNames: {},
        internalTags: [],
      }),
    ]);

    const own = section('Your data');
    for (const text of [
      'No region yet',
      'No habitat notes yet',
      'No notes yet',
      'No localized names yet',
      'No tags yet',
    ]) {
      expect(own.getByText(text)).toBeInTheDocument();
    }
  });

  it('re-syncs from PokeAPI and keeps the proprietary fields', async () => {
    const user = userEvent.setup();
    const { api } = await openPikachu();

    await user.click(screen.getByRole('button', { name: 'Re-sync from PokeAPI' }));

    expect(
      await screen.findByText(
        'Pikachu was re-synced from PokeAPI. Its proprietary fields were kept.',
      ),
    ).toBeInTheDocument();
    expect(
      section('From PokeAPI')
        .getByText(/Synced/)
        .querySelector('time'),
    ).toHaveAttribute('dateTime', '2026-10-15T12:00:00Z');
    expect(section('Your data').getByText('Kanto')).toBeInTheDocument();
    expect(api.writes().map((r) => `${r.method} ${r.path}`)).toEqual([
      'POST /api/v1/local-pokemon/sync/25',
    ]);
  });

  it('says when the record isn’t in the local Pokédex', async () => {
    signInAs('USER');
    localApi([]);
    renderRoute(`/local/${recordId(7999)}`);

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Record not found' }),
    ).toBeInTheDocument();
    expect(
      screen.getByText('This record isn’t in your local Pokédex. It may have been deleted.'),
    ).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Back to My Pokédex' })).toHaveAttribute(
      'href',
      '/local',
    );
  });
});

describe('tags in place', () => {
  afterEach(() => {
    sessionStorage.clear();
  });

  it('removes a tag at once and sends a merge patch with the version shown', async () => {
    const user = userEvent.setup();
    const { api } = await openPikachu();
    const release = api.hold();

    await user.click(screen.getByRole('button', { name: 'Remove tag starter-line' }));

    expect(tags()).toEqual(['electric']);
    await waitFor(() => {
      expect(api.writes()).toHaveLength(1);
    });
    const patch = api.writes()[0];
    expect(patch?.method).toBe('PATCH');
    expect(patch?.ifMatch).toBe('"3"');
    expect(patch?.body).toEqual({ internalTags: ['electric'] });

    release();
    // The tag controls wait for the change in flight (one version at a time).
    const next = screen.getByRole('button', { name: 'Remove tag electric' });
    await waitFor(() => {
      expect(next).toBeEnabled();
    });
    await user.click(next);

    await waitFor(() => {
      expect(api.writes()).toHaveLength(2);
    });
    expect(api.writes()[1]?.ifMatch).toBe('"4"');
    expect(api.writes()[1]?.body).toEqual({ internalTags: [] });
    expect(await section('Your data').findByText('No tags yet')).toBeInTheDocument();
  });

  it('adds a tag, normalised like the API does', async () => {
    const user = userEvent.setup();
    const { api } = await openPikachu();
    const release = api.hold();

    await user.type(screen.getByLabelText('Add a tag'), '  Legendary {Enter}');

    expect(tags()).toEqual(['starter-line', 'electric', 'legendary']);
    expect(screen.getByLabelText('Add a tag')).toHaveValue('');
    await waitFor(() => {
      expect(api.writes()[0]?.body).toEqual({
        internalTags: ['starter-line', 'electric', 'legendary'],
      });
    });
    expect(api.writes()[0]?.ifMatch).toBe('"3"');
    release();
  });

  it('checks a new tag before sending it', async () => {
    const user = userEvent.setup();
    const { api } = await openPikachu();
    const field = screen.getByLabelText('Add a tag');

    await user.type(field, 'Bad Tag!{Enter}');
    await waitFor(() => {
      expect(field).toHaveAccessibleDescription(
        expect.stringContaining(
          "'Bad Tag!' must be 1 to 30 lower-case letters, digits or hyphens, starting with a letter or digit",
        ),
      );
    });

    await user.clear(field);
    await user.type(field, 'Electric{Enter}');
    await waitFor(() => {
      expect(field).toHaveAccessibleDescription(
        expect.stringContaining("'electric' is already a tag"),
      );
    });
    expect(api.writes()).toEqual([]);
  });

  it('undoes a tag change when someone else changed the record first', async () => {
    const user = userEvent.setup();
    const { api } = await openPikachu();
    api.changeElsewhere(PIKACHU.id, {
      notes: 'Changed in another tab',
      internalTags: ['starter-line', 'electric', 'shiny'],
    });

    await user.click(screen.getByRole('button', { name: 'Remove tag starter-line' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Someone else changed this record, so your tag change was undone. The latest version is shown.',
    );
    expect(await section('Your data').findByText('Changed in another tab')).toBeInTheDocument();
    expect(tags()).toEqual(['starter-line', 'electric', 'shiny']);
  });

  it('undoes a tag change the API refuses and says why', async () => {
    const user = userEvent.setup();
    await openPikachu();
    server.use(
      http.patch('/api/v1/local-pokemon/:id', () =>
        problemResponse(400, 'invalid-request', 'The request is invalid.', [
          { field: 'internalTags', message: 'must have at most 20 tags' },
        ]),
      ),
    );

    await user.click(screen.getByRole('button', { name: 'Remove tag electric' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Must have at most 20 tags');
    expect(tags()).toEqual(['starter-line', 'electric']);
  });
});

describe('deleting a record', () => {
  afterEach(() => {
    sessionStorage.clear();
  });

  it('is not offered to a regular user', async () => {
    await openPikachu('USER');

    expect(screen.queryByRole('button', { name: 'Delete' })).not.toBeInTheDocument();
  });

  it('lets an administrator delete after confirming, with the version shown', async () => {
    const user = userEvent.setup();
    const { api, router } = await openPikachu('ADMIN');

    await user.click(screen.getByRole('button', { name: 'Delete' }));
    const confirm = within(screen.getByRole('group', { name: 'Delete Pikachu?' }));
    expect(confirm.getByText(/can’t be undone/)).toBeInTheDocument();
    expect(confirm.getByRole('button', { name: 'Cancel' })).toHaveFocus();
    await user.click(confirm.getByRole('button', { name: 'Delete Pikachu' }));

    expect(await screen.findByText('Pikachu was deleted.')).toBeInTheDocument();
    expect(router.state.location.pathname).toBe('/local');
    expect(api.writes().map((r) => `${r.method} ${r.path} ${String(r.ifMatch)}`)).toEqual([
      `DELETE /api/v1/local-pokemon/${PIKACHU.id} "3"`,
    ]);
    expect(await screen.findByText('Your local Pokédex is empty.')).toBeInTheDocument();
  });

  it('keeps the record when the administrator cancels', async () => {
    const user = userEvent.setup();
    const { api } = await openPikachu('ADMIN');

    await user.click(screen.getByRole('button', { name: 'Delete' }));
    await user.click(screen.getByRole('button', { name: 'Cancel' }));

    expect(screen.queryByRole('group', { name: 'Delete Pikachu?' })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Delete' })).toHaveFocus();
    expect(api.writes()).toEqual([]);
  });

  it('explains when someone else changed the record before the delete', async () => {
    const user = userEvent.setup();
    const { api } = await openPikachu('ADMIN');
    api.changeElsewhere(PIKACHU.id, { notes: 'Changed in another tab' });

    await user.click(screen.getByRole('button', { name: 'Delete' }));
    await user.click(screen.getByRole('button', { name: 'Delete Pikachu' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Someone else changed this record, so it wasn’t deleted. Check the latest version first.',
    );
    expect(await section('Your data').findByText('Changed in another tab')).toBeInTheDocument();
    expect(api.records.has(PIKACHU.id)).toBe(true);
  });

  it('says only administrators can delete when the API answers 403', async () => {
    const user = userEvent.setup();
    await openPikachu('ADMIN');
    server.use(
      http.delete('/api/v1/local-pokemon/:id', () =>
        problemResponse(403, 'forbidden', 'Access is denied.'),
      ),
    );

    await user.click(screen.getByRole('button', { name: 'Delete' }));
    await user.click(screen.getByRole('button', { name: 'Delete Pikachu' }));

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Only administrators can delete records.',
    );
  });

  it('returns to the list when the record was already deleted', async () => {
    const user = userEvent.setup();
    const { api, router } = await openPikachu('ADMIN');
    api.records.delete(PIKACHU.id);

    await user.click(screen.getByRole('button', { name: 'Delete' }));
    await user.click(screen.getByRole('button', { name: 'Delete Pikachu' }));

    expect(await screen.findByText('Pikachu was already deleted.')).toBeInTheDocument();
    expect(router.state.location.pathname).toBe('/local');
  });
});
