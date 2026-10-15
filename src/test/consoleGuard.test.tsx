import { render } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { takeConsoleMessages } from './consoleGuard';

describe('console guard', () => {
  it('records the React warning for a list without keys, so the test would fail', () => {
    const items = ['bulbasaur', 'ivysaur'];

    render(
      <ul>
        {items.map((item) => (
          <li>{item}</li>
        ))}
      </ul>,
    );

    const messages = takeConsoleMessages();
    expect(messages.join('\n')).toContain('unique "key" prop');
  });
});
