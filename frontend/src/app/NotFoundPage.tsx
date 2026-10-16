import { Link } from 'react-router';

import { useDocumentTitle } from '@/shared/hooks/useDocumentTitle';

export function NotFoundPage() {
  useDocumentTitle('Page not found');
  return (
    <section aria-labelledby="not-found-title" className="space-y-4 py-12 text-center">
      <h1 id="not-found-title" className="text-2xl font-bold">
        Page not found
      </h1>
      <p className="text-slate-600">There is nothing at this address.</p>
      <Link to="/" className="font-semibold text-red-700 underline">
        Go to the home page
      </Link>
    </section>
  );
}
