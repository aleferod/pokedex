import { render, screen } from '@testing-library/react';
import { userEvent } from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import { Pagination } from './Pagination';

function renderPagination(page: number, totalPages: number) {
  const onPageChange = vi.fn();
  const onSizeChange = vi.fn();
  render(
    <Pagination
      page={page}
      totalPages={totalPages}
      size={20}
      onPageChange={onPageChange}
      onSizeChange={onSizeChange}
    />,
  );
  return { onPageChange, onSizeChange };
}

describe('Pagination', () => {
  it('shows where the user is and moves either way', async () => {
    const user = userEvent.setup();
    const { onPageChange } = renderPagination(2, 68);

    const nav = screen.getByRole('navigation', { name: 'Pagination' });
    expect(nav).toHaveTextContent('Page 2 of 68');

    await user.click(screen.getByRole('button', { name: 'Next page' }));
    await user.click(screen.getByRole('button', { name: 'Previous page' }));

    expect(onPageChange.mock.calls).toEqual([[3], [1]]);
  });

  it('cannot go before the first page', () => {
    renderPagination(1, 68);

    expect(screen.getByRole('button', { name: 'Previous page' })).toBeDisabled();
    expect(screen.getByRole('button', { name: 'Next page' })).toBeEnabled();
  });

  it('cannot go past the last page', () => {
    renderPagination(68, 68);

    expect(screen.getByRole('button', { name: 'Next page' })).toBeDisabled();
  });

  it('never shows fewer than one page', () => {
    renderPagination(1, 0);

    expect(screen.getByRole('navigation', { name: 'Pagination' })).toHaveTextContent('Page 1 of 1');
    expect(screen.getByRole('button', { name: 'Next page' })).toBeDisabled();
  });

  it('lets the user choose how many entries a page shows', async () => {
    const user = userEvent.setup();
    const { onSizeChange } = renderPagination(3, 68);

    const select = screen.getByRole('combobox', { name: 'Per page' });
    expect(select).toHaveValue('20');
    await user.selectOptions(select, '40');

    expect(onSizeChange).toHaveBeenCalledWith(40);
  });
});
