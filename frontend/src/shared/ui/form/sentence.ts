/** "must contain a digit" → "Must contain a digit": API and form messages read as sentences. */
export function sentence(message: string): string {
  return message.length > 0 ? message.charAt(0).toUpperCase() + message.slice(1) : message;
}
