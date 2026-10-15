import { describe, expect, it } from 'vitest';

import { formatKilograms, formatMetres } from './format';

describe('measures', () => {
  it('writes kilograms with at most one decimal', () => {
    expect(formatKilograms(6.9)).toBe('6.9 kg');
    expect(formatKilograms(13)).toBe('13 kg');
    expect(formatKilograms(0.25)).toBe('0.3 kg');
    expect(formatKilograms(999.9)).toBe('999.9 kg');
  });

  it('groups thousands', () => {
    expect(formatKilograms(1000)).toBe('1,000 kg');
  });

  it('writes metres with at most one decimal', () => {
    expect(formatMetres(0.7)).toBe('0.7 m');
    expect(formatMetres(2)).toBe('2 m');
    expect(formatMetres(14.5)).toBe('14.5 m');
  });
});
