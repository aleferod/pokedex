/**
 * Where to go after signing in. Only paths inside this app are accepted: anything else (another
 * site via "//host" or "/\host", an absolute URL, "javascript:") falls back to home, so a crafted
 * link cannot turn the sign-in page into an open redirect.
 */
export function safeNext(value: string | null | undefined): string {
  if (!value?.startsWith('/') || value.startsWith('//') || value.startsWith('/\\')) {
    return '/';
  }
  return value;
}
