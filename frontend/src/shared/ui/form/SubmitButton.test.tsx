import { fireEvent, render, screen } from '@testing-library/react';
import { userEvent } from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import { SubmitButton } from './SubmitButton';

function Form({ onSubmit }: { onSubmit: () => void }) {
  return (
    <form
      onSubmit={(event) => {
        event.preventDefault();
        onSubmit();
      }}
    >
      <label htmlFor="field">Field</label>
      <input id="field" />
      <SubmitButton busy={false} busyLabel="Saving…">
        Save
      </SubmitButton>
    </form>
  );
}

describe('SubmitButton', () => {
  it('keeps focus in the field when pressed, so leaving the field can’t move the button away', () => {
    render(<Form onSubmit={vi.fn()} />);
    const field = screen.getByLabelText('Field');
    field.focus();

    const notCancelled = fireEvent.mouseDown(screen.getByRole('button', { name: 'Save' }));

    expect(notCancelled).toBe(false);
    expect(field).toHaveFocus();
  });

  it('still submits on a click and on Enter in a field', async () => {
    const user = userEvent.setup();
    const onSubmit = vi.fn();
    render(<Form onSubmit={onSubmit} />);

    await user.click(screen.getByRole('button', { name: 'Save' }));
    await user.type(screen.getByLabelText('Field'), 'x{Enter}');

    expect(onSubmit).toHaveBeenCalledTimes(2);
  });
});
