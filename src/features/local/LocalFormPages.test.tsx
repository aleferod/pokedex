import { screen, waitFor, within } from '@testing-library/react';
import { userEvent } from '@testing-library/user-event';
import { http } from 'msw';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';

import { saveSession } from '@/features/auth/session';
import { problemResponse } from '@/test/authApi';
import { localApi, localRecord, recordId } from '@/test/localApi';
import { renderRoute } from '@/test/renderRoute';
import { server } from '@/test/server';
import { testSession } from '@/test/sessions';

const PIKACHU = localRecord({ id: recordId(8001) });
const EDIT = `/local/${PIKACHU.id}/edit`;

function localizedName(n: number) {
  return within(screen.getByRole('group', { name: `Localized name ${String(n)}` }));
}

describe('adding a Pokémon', () => {
  beforeEach(() => {
    saveSession(testSession());
  });
  afterEach(() => {
    sessionStorage.clear();
  });

  it('stores a Pokédex number with every proprietary field and opens the record', async () => {
    const user = userEvent.setup();
    const api = localApi([]);
    const { router } = renderRoute('/local/new');

    await user.type(await screen.findByLabelText('Pokédex number'), '1');
    await user.type(screen.getByLabelText('Region'), '  Kanto ');
    await user.type(screen.getByLabelText('Habitat notes'), 'Grasslands');
    await user.type(screen.getByLabelText('Notes'), 'First partner');
    await user.type(screen.getByLabelText('Tags'), 'Starter, kanto, starter');
    await user.click(screen.getByRole('button', { name: 'Add a localized name' }));
    await user.type(localizedName(1).getByLabelText('Locale'), 'pt-BR');
    await user.type(localizedName(1).getByLabelText('Name'), 'Bulbassauro');
    await user.click(screen.getByRole('button', { name: 'Add a localized name' }));
    await user.type(localizedName(2).getByLabelText('Locale'), 'ja');
    await user.click(screen.getByRole('button', { name: 'Remove localized name 2' }));
    await user.click(screen.getByRole('button', { name: 'Add to my Pokédex' }));

    expect(
      await screen.findByText('Bulbasaur was added to your local Pokédex.'),
    ).toBeInTheDocument();
    expect(api.writes()[0]?.body).toEqual({
      pokeapiId: 1,
      region: 'Kanto',
      habitatNotes: 'Grasslands',
      notes: 'First partner',
      internalTags: ['starter', 'kanto'],
      localizedNames: { 'pt-BR': 'Bulbassauro' },
    });
    const stored = [...api.records.values()].find((r) => r.pokeapiId === 1);
    expect(router.state.location.pathname).toBe(`/local/${String(stored?.id)}`);
    expect(await screen.findByRole('heading', { level: 1, name: 'Bulbasaur' })).toBeInTheDocument();
  });

  it('checks every rule before sending, and focuses the first problem', async () => {
    const user = userEvent.setup();
    const api = localApi([]);
    renderRoute('/local/new');

    await user.type(await screen.findByLabelText('Region'), 'a'.repeat(51));
    await user.type(screen.getByLabelText('Tags'), 'Bad Tag!');
    await user.click(screen.getByRole('button', { name: 'Add to my Pokédex' }));

    const number = screen.getByLabelText('Pokédex number');
    await waitFor(() => {
      expect(number).toHaveFocus();
    });
    expect(number).toHaveAccessibleDescription(expect.stringContaining('Is required'));
    expect(screen.getByLabelText('Region')).toHaveAccessibleDescription(
      expect.stringContaining('Must be at most 50 characters'),
    );
    expect(screen.getByLabelText('Tags')).toHaveAccessibleDescription(
      expect.stringContaining("'Bad Tag!' must be 1 to 30 lower-case letters"),
    );
    expect(api.writes()).toEqual([]);
  });

  it('reports a number that is already stored on its field and keeps the input', async () => {
    const user = userEvent.setup();
    localApi([PIKACHU]);
    renderRoute('/local/new');

    await user.type(await screen.findByLabelText('Pokédex number'), '25');
    await user.type(screen.getByLabelText('Region'), 'Johto');
    await user.click(screen.getByRole('button', { name: 'Add to my Pokédex' }));

    const number = screen.getByLabelText('Pokédex number');
    await waitFor(() => {
      expect(number).toHaveAccessibleDescription(
        expect.stringContaining('Is already stored locally'),
      );
    });
    expect(number).toHaveValue('25');
    expect(screen.getByLabelText('Region')).toHaveValue('Johto');
  });

  it('puts the API’s field errors on the fields, a localized name on its row', async () => {
    const user = userEvent.setup();
    localApi([]);
    server.use(
      http.post('/api/v1/local-pokemon', () =>
        problemResponse(400, 'invalid-request', 'The request is invalid.', [
          { field: 'notes', message: 'must be at most 2000 characters' },
          { field: 'localizedNames.pt-BR', message: 'must be 1 to 50 characters' },
        ]),
      ),
    );
    renderRoute('/local/new');

    await user.type(await screen.findByLabelText('Pokédex number'), '1');
    await user.type(screen.getByLabelText('Notes'), 'Fine here');
    await user.click(screen.getByRole('button', { name: 'Add a localized name' }));
    await user.type(localizedName(1).getByLabelText('Locale'), 'pt-BR');
    await user.type(localizedName(1).getByLabelText('Name'), 'Bulbassauro');
    await user.click(screen.getByRole('button', { name: 'Add to my Pokédex' }));

    await waitFor(() => {
      expect(screen.getByLabelText('Notes')).toHaveAccessibleDescription(
        expect.stringContaining('Must be at most 2000 characters'),
      );
    });
    expect(localizedName(1).getByLabelText('Name')).toHaveAccessibleDescription(
      expect.stringContaining('Must be 1 to 50 characters'),
    );
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });
});

describe('editing a record', () => {
  beforeEach(() => {
    saveSession(testSession());
  });
  afterEach(() => {
    sessionStorage.clear();
  });

  it('starts from the record and saves every field with the version it loaded', async () => {
    const user = userEvent.setup();
    const api = localApi([PIKACHU]);
    const { router } = renderRoute(EDIT);

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Edit Pikachu' }),
    ).toBeInTheDocument();
    const region = screen.getByLabelText('Region');
    expect(region).toHaveValue('Kanto');
    expect(screen.getByLabelText('Tags')).toHaveValue('starter-line, electric');
    expect(localizedName(1).getByLabelText('Locale')).toHaveValue('pt-BR');
    expect(localizedName(1).getByLabelText('Name')).toHaveValue('Pikachu');
    await waitFor(() => {
      expect(document.title).toBe('Edit Pikachu · Pokédex');
    });

    await user.clear(region);
    await user.type(region, 'Johto');
    await user.clear(screen.getByLabelText('Notes'));
    await user.click(screen.getByRole('button', { name: 'Save changes' }));

    expect(await screen.findByText('Your changes were saved.')).toBeInTheDocument();
    expect(router.state.location.pathname).toBe(`/local/${PIKACHU.id}`);
    const put = api.writes()[0];
    expect(put?.method).toBe('PUT');
    expect(put?.ifMatch).toBe('"3"');
    expect(put?.body).toEqual({
      region: 'Johto',
      habitatNotes: 'Forests near power plants',
      notes: null,
      internalTags: ['starter-line', 'electric'],
      localizedNames: { 'pt-BR': 'Pikachu' },
    });
    expect(await screen.findByText('Johto')).toBeInTheDocument();
  });

  it('keeps its loaded version, explains a conflict and loads the latest on request', async () => {
    const user = userEvent.setup();
    const api = localApi([PIKACHU]);
    const { queryClient } = renderRoute(EDIT);
    const region = await screen.findByLabelText('Region');

    // Someone else saves, and this tab happens to refetch the record in the background.
    api.changeElsewhere(PIKACHU.id, { notes: 'Changed in another tab' });
    await queryClient.refetchQueries({ queryKey: ['local', 'record', PIKACHU.id] });

    await user.clear(region);
    await user.type(region, 'Johto');
    await user.click(screen.getByRole('button', { name: 'Save changes' }));

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent(
      'Someone else changed this record after you opened it, so your changes weren’t saved.',
    );
    expect(api.writes()[0]?.ifMatch).toBe('"3"');
    expect(region).toHaveValue('Johto');

    await user.click(within(alert).getByRole('button', { name: 'Load the latest version' }));

    await waitFor(() => {
      expect(screen.getByLabelText('Notes')).toHaveValue('Changed in another tab');
    });
    expect(screen.getByLabelText('Region')).toHaveValue('Kanto');
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Save changes' }));

    expect(await screen.findByText('Your changes were saved.')).toBeInTheDocument();
    expect(api.writes()[1]?.ifMatch).toBe('"4"');
  });

  it('puts the API’s tag error on the tags line', async () => {
    const user = userEvent.setup();
    localApi([PIKACHU]);
    server.use(
      http.put('/api/v1/local-pokemon/:id', () =>
        problemResponse(400, 'invalid-request', 'The request is invalid.', [
          { field: 'internalTags', message: 'must have at most 20 tags' },
        ]),
      ),
    );
    renderRoute(EDIT);

    await user.click(await screen.findByRole('button', { name: 'Save changes' }));

    await waitFor(() => {
      expect(screen.getByLabelText('Tags')).toHaveAccessibleDescription(
        expect.stringContaining('Must have at most 20 tags'),
      );
    });
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('says when the record to edit is not there', async () => {
    localApi([]);
    renderRoute(`/local/${recordId(8999)}/edit`);

    expect(
      await screen.findByRole('heading', { level: 1, name: 'Record not found' }),
    ).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Back to My Pokédex' })).toHaveAttribute(
      'href',
      '/local',
    );
  });
});
