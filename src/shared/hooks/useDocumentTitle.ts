import { useEffect } from 'react';

export const APP_NAME = 'Pokédex';

/** Sets the document title to "<title> · Pokédex" while the calling page is shown. */
export function useDocumentTitle(title: string): void {
  useEffect(() => {
    document.title = `${title} · ${APP_NAME}`;
  }, [title]);
}
