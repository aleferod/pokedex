package com.ballastlane.pokedex.adapter.out.pokeapi;

/**
 * Cleans PokeAPI flavor texts, which keep the line breaks and form feeds of the original game
 * screens: every control character and soft hyphen becomes a space, whitespace runs collapse to one
 * space, and the result is trimmed. Nothing readable left gives {@code null}.
 */
final class FlavorText {

  private static final char SOFT_HYPHEN = '­';

  private FlavorText() {}

  static String clean(String raw) {
    if (raw == null) {
      return null;
    }
    StringBuilder text = new StringBuilder(raw.length());
    raw.codePoints()
        .map(c -> Character.isISOControl(c) || c == SOFT_HYPHEN ? ' ' : c)
        .forEach(text::appendCodePoint);
    String cleaned = text.toString().replaceAll("\\s+", " ").trim();
    return cleaned.isEmpty() ? null : cleaned;
  }
}
