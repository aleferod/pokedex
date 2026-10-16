/** "deoxys-attack" → "Deoxys-attack": PokeAPI names read as names. */
export function displayName(name: string): string {
  return name.length > 0 ? name.charAt(0).toUpperCase() + name.slice(1) : name;
}
